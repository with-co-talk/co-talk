# Fastlane으로 iOS·macOS·Android 한 번에 배포하기 — Co-Talk 멀티플랫폼 자동화

> "flutter build ios, flutter build macos, flutter build appbundle… 이걸 매번 수동으로?"

Flutter 앱을 iOS, macOS, Android 세 플랫폼에 동시에 배포해야 할 때, 수동 빌드는 금방 한계에 부딪힌다. 각 플랫폼마다 빌드 명령이 다르고, 코드 서명 방식이 다르고, 업로드 도구도 다르다. Co-Talk 프로젝트를 운영하면서 이 세 가지를 하나의 Fastfile로 통합한 경험을 정리했다. 특히 macOS 코드 서명은 iOS보다 훨씬 복잡해서 별도 섹션으로 다뤘다. 시리즈 17편이다.

---

## 1. 통합 Fastfile 설계

세 플랫폼을 하나의 Fastfile로 관리하는 핵심 아이디어는 레인(lane) 계층화다. 최상위 레인이 하위 레인을 호출하는 구조로, 각 플랫폼별 레인은 독립적으로도 실행 가능하고, 통합 레인에서도 호출된다.

<!-- IMAGE: Fastlane 레인 계층 구조 다이어그램 -->

```plantuml
@startuml
!theme plain
top to bottom direction

rectangle "deploy_all_platforms" as DAP #FFccFF {
}

rectangle "deploy_all\n(iOS + macOS)" as DA {
  rectangle "bump_build" as BB
  rectangle "deploy_ios" as DI
  rectangle "deploy_macos" as DM
}

rectangle "deploy_android" as DAN

DAP --> DA
DAP --> DAN

DI --> "build_ios\n(flutter build ipa)" as BI
DI --> "upload_ios\n(upload_to_testflight)" as UI

DM --> "setup_macos_signing\n(cert + sigh)" as SMS
DM --> "build_macos_signed\n(flutter build + resign)" as BMS
DM --> "upload_macos\n(productbuild + testflight)" as UM

DAN --> "build_android\n(flutter build appbundle)" as BA
DAN --> "upload_android\n(upload_to_play_store)" as UA
@enduml
```

실제 Fastfile의 상위 레인 구조는 다음과 같다.

```ruby
# fastlane/Fastfile
default_platform(:ios)

desc "iOS + macOS + Android 모두 배포"
lane :deploy_all_platforms do |options|
  deploy_all(skip_bump: options[:skip_bump])

  UI.header("Android 빌드 시작")
  deploy_android(track: options[:track] || "internal")

  UI.success("모든 플랫폼 배포 완료!")
end

desc "iOS와 macOS 모두 빌드 및 TestFlight 업로드"
lane :deploy_all do |options|
  bump_build unless options[:skip_bump]

  UI.header("iOS 빌드 시작")
  deploy_ios(skip_build: options[:skip_build], skip_bump: true)

  UI.header("macOS 빌드 시작")
  deploy_macos(skip_build: options[:skip_build], skip_bump: true)

  UI.success("iOS + macOS 배포 완료!")
end
```

`bump_build`가 한 번만 실행되고 iOS/macOS에 동일한 빌드 넘버가 적용되는 구조다. `skip_bump: true`를 하위 레인에 전달해서 빌드 넘버가 이중으로 증가하는 것을 막는다.

레인 구조를 이렇게 설계한 이유는 유연성 때문이다. iOS만 배포하고 싶으면 `fastlane deploy_ios`, Android만 배포하고 싶으면 `fastlane deploy_android`로 독립 실행이 가능하다. 전체 배포가 필요하면 `fastlane deploy_all_platforms`를 쓰면 된다.

---

## 2. App Store Connect API Key 인증

예전에는 Apple ID와 비밀번호로 fastlane이 App Store Connect에 로그인했다. 2FA가 도입되면서 이 방식이 CI 환경에서 불안정해졌다. 지금은 API Key 방식이 표준이다.

이유는 명확하다.

- 2FA 우회 로직 불필요 (2FA 팝업을 자동화하는 코드는 언제 깨질지 모른다)
- CI 서버에서도 안정적으로 동작
- 토큰 유효기간 20분, Fastlane이 자동 갱신
- 팀 멤버가 바뀌어도 인증 정보 교체 불필요

```ruby
# fastlane/Fastfile
def get_api_key
  app_store_connect_api_key(
    key_id: ENV["APP_STORE_CONNECT_API_KEY_KEY_ID"],
    issuer_id: ENV["APP_STORE_CONNECT_API_KEY_ISSUER_ID"],
    key_filepath: ENV["APP_STORE_CONNECT_API_KEY_KEY_FILEPATH"],
    duration: 1200,  # 20분
    in_house: false
  )
end
```

`.env.example` 파일에 필요한 환경변수를 문서화해 두었다.

```bash
# fastlane/.env.example
APPLE_ID=your-apple-id@example.com
TEAM_ID=YOUR_TEAM_ID
ITC_TEAM_ID=YOUR_ITC_TEAM_ID
APP_STORE_CONNECT_API_KEY_KEY_ID=YOUR_KEY_ID
APP_STORE_CONNECT_API_KEY_ISSUER_ID=YOUR_ISSUER_ID
APP_STORE_CONNECT_API_KEY_KEY_FILEPATH=./fastlane/keys/AuthKey_YOUR_KEY_ID.p8
```

`.p8` 파일은 App Store Connect에서 한 번만 다운로드할 수 있다. 분실하면 재발급이 필요하다. `fastlane/keys/` 디렉토리에 보관하되, `.gitignore`에 반드시 추가해야 한다. 이 파일이 Git에 올라가는 순간 즉시 폐기하고 재발급해야 한다.

API Key 발급 경로: App Store Connect → 사용자 및 액세스 → 통합 → App Store Connect API → 팀 키 생성. 역할은 최소한으로 설정한다. 배포 자동화에는 "앱 관리자" 역할이면 충분하다.

---

## 3. iOS 빌드 & 배포

iOS 배포는 세 플랫폼 중 가장 간단하다. Flutter가 대부분의 코드 서명 작업을 자동으로 처리한다.

```ruby
# fastlane/Fastfile
desc "iOS 빌드만"
lane :build_ios do
  UI.message("Flutter iOS 빌드 중...")
  project_root = File.expand_path("..", Dir.pwd)

  cocoapods(
    podfile: "#{project_root}/ios/Podfile",
    use_bundle_exec: false
  )

  system(
    "cd #{project_root} && flutter build ipa --release --dart-define=ENVIRONMENT=prod"
  ) || UI.user_error!("Flutter iOS 빌드 실패")

  UI.success("iOS 빌드 완료")
end

desc "iOS TestFlight 업로드"
lane :upload_ios do
  project_root = File.expand_path("..", Dir.pwd)
  ipa_path = Dir["#{project_root}/build/ios/ipa/*.ipa"].first

  if ipa_path.nil?
    UI.user_error!("IPA 파일을 찾을 수 없습니다.")
  end

  api_key = get_api_key
  upload_to_testflight(
    api_key: api_key,
    ipa: ipa_path,
    skip_waiting_for_build_processing: true,
    skip_submission: true
  )
end

desc "iOS 빌드 + TestFlight 업로드"
lane :deploy_ios do |options|
  build_ios unless options[:skip_build]
  upload_ios
end
```

`flutter build ipa`가 ExportOptions.plist를 자동 생성한다. Xcode에서 수동으로 Archive → Distribute를 하는 것과 동일한 결과물이 나온다. `--dart-define=ENVIRONMENT=prod`로 빌드 타임에 환경을 구분한다. Flutter 코드에서 `const String env = String.fromEnvironment('ENVIRONMENT', defaultValue: 'dev')`로 읽을 수 있다.

`skip_waiting_for_build_processing: true`는 TestFlight 업로드 후 Apple 서버의 처리를 기다리지 않는다는 의미다. 처리 시간이 10~30분인데, CI에서 이걸 기다리면 자원 낭비다. 처리 완료 알림은 이메일로 받는다.

---

## 4. macOS 코드 서명 심화 — 가장 까다로운 부분

솔직히 말하면 macOS 배포 자동화가 이 프로젝트에서 가장 오래 걸린 부분이다. 이틀 넘게 `codesign` 오류와 씨름했다.

왜 macOS가 iOS보다 복잡한가:

- `flutter build macos`는 ad-hoc 서명을 생성한다. App Store 제출이 불가능한 서명이다.
- Apple Distribution 인증서로 수동 재서명이 필요하다.
- 앱 번들 내부의 프레임워크까지 안에서 밖으로 순서대로 서명해야 한다. 순서가 틀리면 검증이 실패한다.
- `.pkg` 파일로 패키징 후 업로드해야 한다. iOS처럼 `.ipa`가 아니다.

<!-- IMAGE: macOS 코드 서명 순서 — 프레임워크 내부부터 앱 번들까지 -->

**cert + sigh 설정**

두 종류의 인증서가 필요하다. 앱 자체를 서명하는 Apple Distribution 인증서와, `.pkg`를 서명하는 Mac Installer Distribution 인증서다.

```ruby
# fastlane/Fastfile
lane :setup_macos_signing do
  api_key = get_api_key

  # 1. Apple Distribution 인증서 (앱 서명용)
  cert(
    platform: "macos",
    team_id: ENV["TEAM_ID"] || "{yourTeamId}",
    api_key: api_key,
    generate_apple_certs: true
  )

  app_cert_id = lane_context[SharedValues::CERT_CERTIFICATE_ID]

  # 2. Mac Installer Distribution 인증서 (pkg 서명용)
  cert(
    platform: "macos",
    type: "mac_installer_distribution",
    team_id: ENV["TEAM_ID"] || "{yourTeamId}",
    api_key: api_key,
    generate_apple_certs: true
  )

  # 3. App Store 프로비저닝 프로필
  sigh(
    platform: "macos",
    app_identifier: "com.cotalk.coTalkFlutter",
    team_id: ENV["TEAM_ID"] || "{yourTeamId}",
    api_key: api_key,
    cert_id: app_cert_id,
    force: true
  )
end
```

`cert` 액션이 키체인에 인증서를 설치하고, `sigh`가 프로비저닝 프로필을 다운로드한다. CI 환경에서는 임시 키체인을 만들어 사용하는 것이 안전하다. 로컬 개발자 키체인을 건드리지 않기 위해서다.

**재서명 스크립트**

이 스크립트가 핵심이다. Flutter가 생성한 ad-hoc 서명을 Apple Distribution 서명으로 교체한다.

```bash
#!/bin/bash
# scripts/resign_for_appstore.sh
set -e

APP_PATH="$1"
ENTITLEMENTS="$2"
PROFILE_PATH="$3"
SIGN_IDENTITY="Apple Distribution: {YourName} ({yourTeamId})"

echo "재서명 대상: $APP_PATH"
echo "서명 ID: $SIGN_IDENTITY"

# 1. 프로비저닝 프로필 복사
cp "$PROFILE_PATH" "$APP_PATH/Contents/embedded.provisionprofile"
echo "프로비저닝 프로필 복사 완료"

# 2. 프레임워크 내부 실행파일 서명 (가장 깊은 곳부터)
FRAMEWORKS_PATH="$APP_PATH/Contents/Frameworks"
for framework in "$FRAMEWORKS_PATH"/*.framework; do
  if [ -d "$framework" ]; then
    framework_name=$(basename "$framework" .framework)
    executable="$framework/Versions/A/$framework_name"
    if [ -f "$executable" ]; then
      echo "프레임워크 실행파일 서명: $framework_name"
      codesign --force --timestamp --options runtime \
        --sign "$SIGN_IDENTITY" "$executable"
    fi
  fi
done

# 3. 프레임워크 번들 전체 서명
for framework in "$FRAMEWORKS_PATH"/*.framework; do
  if [ -d "$framework" ]; then
    framework_name=$(basename "$framework" .framework)
    echo "프레임워크 번들 서명: $framework_name"
    codesign --force --timestamp --options runtime \
      --sign "$SIGN_IDENTITY" "$framework"
  fi
done

# 4. 메인 실행파일 서명 (entitlements 포함)
echo "메인 실행파일 서명..."
codesign --force --timestamp --options runtime \
  --entitlements "$ENTITLEMENTS" \
  --sign "$SIGN_IDENTITY" "$APP_PATH/Contents/MacOS/Co Talk"

# 5. 앱 번들 전체 서명
echo "앱 번들 전체 서명..."
codesign --force --timestamp --options runtime \
  --entitlements "$ENTITLEMENTS" \
  --sign "$SIGN_IDENTITY" "$APP_PATH"

# 6. 검증
echo "서명 검증 중..."
codesign --verify --deep --strict --verbose=2 "$APP_PATH"
echo "서명 검증 완료"
```

서명 순서가 왜 중요한지 설명하면:

- `codesign --verify --deep`은 앱 번들 내부 모든 서명을 재귀적으로 검증한다.
- 바깥 번들을 먼저 서명하면, 나중에 내부를 서명할 때 바깥 번들의 서명이 깨진다. macOS는 번들 내용이 바뀌면 서명을 무효로 본다.
- 따라서 반드시 가장 안쪽(프레임워크 내부 실행파일)부터 서명하고 바깥으로 나와야 한다.

각 플래그의 의미:

- `--options runtime`: Hardened Runtime 활성화. App Store 제출 필수 조건이다.
- `--timestamp`: Apple 타임스탬프 서버를 통해 서명 시각을 인증한다. 공증(Notarization)에 필요하다.
- `--entitlements`: 앱이 사용하는 권한을 선언한다. 네트워크 접근, 샌드박스 등.
- `--force`: 기존 서명을 덮어쓴다.

**Entitlements 파일**

macOS 앱에 필요한 권한을 선언한다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<!-- macos/Runner/Release.entitlements -->
<plist version="1.0">
<dict>
  <key>com.apple.security.app-sandbox</key>
  <true/>
  <key>com.apple.security.network.client</key>
  <true/>
  <key>com.apple.security.network.server</key>
  <true/>
  <key>com.apple.security.files.user-selected.read-write</key>
  <true/>
</dict>
</plist>
```

**Fastfile에서 재서명 스크립트 호출**

```ruby
# fastlane/Fastfile
lane :build_macos_signed do
  project_root = File.expand_path("..", Dir.pwd)

  # Flutter 빌드 (ad-hoc 서명)
  system(
    "cd #{project_root} && flutter build macos --release --dart-define=ENVIRONMENT=prod"
  ) || UI.user_error!("Flutter macOS 빌드 실패")

  app_path = "#{project_root}/build/macos/Build/Products/Release/Co Talk.app"
  entitlements = "#{project_root}/macos/Runner/Release.entitlements"
  profile_path = Dir[
    "#{ENV['HOME']}/Library/MobileDevice/Provisioning Profiles/*.provisionprofile"
  ].first

  if profile_path.nil?
    UI.user_error!("프로비저닝 프로필을 찾을 수 없습니다. setup_macos_signing을 먼저 실행하세요.")
  end

  resign_script = "#{project_root}/scripts/resign_for_appstore.sh"
  sh("bash '#{resign_script}' '#{app_path}' '#{entitlements}' '#{profile_path}'")

  UI.success("macOS 재서명 완료")
end
```

**macOS 업로드**

```ruby
# fastlane/Fastfile
lane :upload_macos do
  project_root = File.expand_path("..", Dir.pwd)
  app_path = "#{project_root}/build/macos/Build/Products/Release/Co Talk.app"
  pkg_path = "#{project_root}/build/macos/CoTalk.pkg"

  # .app → .pkg 변환 및 Installer 인증서로 서명
  sh(
    "productbuild --component '#{app_path}' /Applications " \
    "--sign '3rd Party Mac Developer Installer: {yourName}({yourTeamId})' " \
    "'#{pkg_path}'"
  )

  api_key = get_api_key
  upload_to_testflight(
    api_key: api_key,
    pkg: pkg_path,
    skip_waiting_for_build_processing: true,
    skip_submission: true,
    app_platform: "osx"
  )

  UI.success("macOS TestFlight 업로드 완료")
end

desc "macOS 전체 배포"
lane :deploy_macos do |options|
  setup_macos_signing
  build_macos_signed unless options[:skip_build]
  upload_macos
end
```

macOS는 IPA가 아니라 `.pkg`로 업로드한다. `productbuild`가 `.app`을 `.pkg`로 패키징한다. 여기서 "3rd Party Mac Developer Installer" 인증서가 사용된다. App Distribution 인증서와 다른 인증서다. 두 인증서 모두 `setup_macos_signing`에서 준비된다.

---

## 5. Android — Google Play 업로드

Android는 상대적으로 단순하다. Flutter 빌드가 코드 서명까지 처리하고, Fastlane이 Google Play에 업로드한다.

```ruby
# fastlane/Fastfile
desc "Android 빌드만"
lane :build_android do
  project_root = File.expand_path("..", Dir.pwd)
  system(
    "cd #{project_root} && flutter build appbundle --release --dart-define=ENVIRONMENT=prod"
  ) || UI.user_error!("Flutter Android 빌드 실패")

  UI.success("Android 빌드 완료")
end

desc "Google Play 업로드"
lane :upload_android do |options|
  project_root = File.expand_path("..", Dir.pwd)
  aab_path = "#{project_root}/build/app/outputs/bundle/release/app-release.aab"
  track = options[:track] || "internal"

  unless File.exist?(aab_path)
    UI.user_error!("AAB 파일을 찾을 수 없습니다: #{aab_path}")
  end

  upload_to_play_store(
    package_name: "com.cotalk.co_talk_flutter",
    aab: aab_path,
    track: track,
    release_status: "draft",
    json_key: "#{Dir.pwd}/keys/play-store-key.json",
    skip_upload_metadata: true,
    skip_upload_images: true,
    skip_upload_screenshots: true
  )

  UI.success("Android #{track} 트랙 업로드 완료")
end

desc "Android 빌드 + Play Store 업로드"
lane :deploy_android do |options|
  build_android
  upload_android(track: options[:track] || "internal")
end
```

몇 가지 중요한 결정들이다.

**`release_status: "draft"`**: 업로드만 하고 실제 출시는 하지 않는다. Google Play Console에서 수동으로 출시 버튼을 누르는 방식이다. 자동화 실수로 프로덕션에 잘못된 빌드가 배포되는 사고를 방지한다. 처음에는 `release_status: "completed"`로 완전 자동화를 시도했다가, 리뷰 미통과 상태에서 배포되는 아찔한 경험 후 draft로 바꿨다.

**트랙 전략**: internal → alpha → beta → production 순서로 승격한다. CI에서는 항상 internal로 올리고, 수동 검증 후 더 넓은 트랙으로 승격한다. `fastlane deploy_android track:alpha`처럼 트랙을 파라미터로 받아서 유연하게 운영한다.

**서비스 계정 JSON**: Google Cloud Console에서 서비스 계정을 만들고 JSON 키를 다운로드한다. Google Play API 접근 권한은 최소한으로 부여한다. `fastlane/keys/play-store-key.json`에 보관하고 `.gitignore`에 추가한다.

**metadata skip**: `skip_upload_metadata: true`로 스토어 설명, 이미지, 스크린샷 업로드를 생략한다. Google Play Console에서 직접 관리하는 것이 더 편하다. 특히 스크린샷은 여러 기기 크기로 준비해야 하는데, 이걸 CI에서 자동화하는 것은 득보다 실이 많다.

---

## 6. 빌드 넘버 동기화

iOS와 macOS는 같은 Apple 개발자 계정에 같은 앱 번들 ID를 쓴다. App Store Connect에서는 두 플랫폼을 하나의 앱으로 관리하는데, 빌드 넘버가 다르면 충돌이 생긴다. 두 플랫폼의 빌드 넘버를 항상 동기화해야 한다.

```ruby
# fastlane/Fastfile
desc "iOS + macOS 빌드 넘버 동기화 증가"
lane :bump_build do |options|
  project_root = File.expand_path("..", Dir.pwd)

  if options[:build]
    new_build = options[:build].to_s
  else
    current_build = get_build_number(
      xcodeproj: "#{project_root}/ios/Runner.xcodeproj"
    ).to_i
    new_build = (current_build + 1).to_s
  end

  UI.message("빌드 번호 #{new_build}로 업데이트 중...")

  # iOS 빌드 넘버
  increment_build_number(
    build_number: new_build,
    xcodeproj: "#{project_root}/ios/Runner.xcodeproj"
  )

  # macOS 빌드 넘버 (iOS와 동일하게)
  increment_build_number(
    build_number: new_build,
    xcodeproj: "#{project_root}/macos/Runner.xcodeproj"
  )

  UI.success("빌드 번호 #{new_build}로 업데이트 완료")
  new_build
end
```

`options[:build]`로 특정 빌드 넘버를 지정할 수도 있다. CI에서 GitHub Actions의 런 번호를 빌드 넘버로 쓰면 일관성이 높아진다.

```bash
# GitHub Actions에서 특정 빌드 넘버 지정
fastlane bump_build build:${{ github.run_number }}
```

Android의 빌드 넘버는 `pubspec.yaml`의 `version: 1.0.0+빌드번호`에서 관리한다. `flutter build appbundle`이 이 값을 읽어서 `versionCode`로 사용한다. iOS/macOS와는 별도로 관리되므로 동기화 이슈가 없다.

---

## 7. App Store 심사 제출

TestFlight 배포 후 App Store 심사 제출까지 자동화한다. 스토어 카피(설명, 키워드, 릴리스 노트)를 코드로 관리하는 것이 핵심이다.

```ruby
# fastlane/Fastfile
desc "App Store 심사 제출 (iOS)"
lane :submit_ios do |options|
  api_key = get_api_key

  deliver(
    api_key: api_key,
    app_identifier: "com.cotalk.coTalkFlutter",
    skip_binary_upload: true,
    submit_for_review: true,
    automatic_release: options[:auto_release] || false,
    metadata_path: "#{Dir.pwd}/metadata",
    screenshots_path: "#{Dir.pwd}/screenshots",
    force: true
  )
end
```

metadata 디렉토리 구조:

```
fastlane/
  metadata/
    en-US/
      name.txt
      subtitle.txt
      description.txt
      keywords.txt
      release_notes.txt
      promotional_text.txt
      support_url.txt
      privacy_url.txt
    ko/
      name.txt
      subtitle.txt
      description.txt
      keywords.txt
      release_notes.txt
```

`release_notes.txt`에 버전별 변경사항을 적고, Git에 커밋하면 된다. 스토어 카피 변경 이력이 Git 로그에 남는다. 다국어 관리도 파일 구조로 자연스럽게 된다. `automatic_release: false`로 설정해서 심사 통과 후 수동으로 출시 시점을 결정한다.

---

## 8. 전체 Fastfile 실행 명령 정리

자주 쓰는 명령을 정리했다.

```bash
# 전체 플랫폼 배포 (iOS + macOS + Android internal)
fastlane deploy_all_platforms

# iOS + macOS만 배포
fastlane deploy_all

# iOS만 배포
fastlane deploy_ios

# macOS만 배포
fastlane deploy_macos

# Android internal 트랙 배포
fastlane deploy_android

# Android alpha 트랙 배포
fastlane deploy_android track:alpha

# 빌드 넘버만 증가 (배포 없이)
fastlane bump_build

# 특정 빌드 넘버로 설정
fastlane bump_build build:100

# App Store 심사 제출
fastlane submit_ios

# macOS 서명 설정만 실행
fastlane setup_macos_signing
```

배포 전 로컬에서 한 번 dry run으로 확인하는 습관이 좋다.

```bash
# 환경변수 로드 확인
fastlane run app_store_connect_api_key \
  key_id:$APP_STORE_CONNECT_API_KEY_KEY_ID \
  issuer_id:$APP_STORE_CONNECT_API_KEY_ISSUER_ID \
  key_filepath:$APP_STORE_CONNECT_API_KEY_KEY_FILEPATH
```

---

## 9. 교훈

세 플랫폼 배포를 자동화하면서 얻은 교훈들이다.

**1. macOS 코드 서명이 iOS보다 훨씬 복잡하다**

Flutter가 ad-hoc 서명을 생성하기 때문에 수동 재서명 스크립트가 필수다. 서명 순서(안에서 밖으로)를 틀리면 `codesign --verify --deep`에서 실패한다. `resign_for_appstore.sh`를 프로젝트에 포함시키고 버전 관리하는 것이 맞다.

**2. API Key 인증이 비밀번호 인증보다 훨씬 안정적이다**

App Store Connect API Key(.p8)는 2FA 없이 동작하고, CI에서도 안정적이다. 한 번 설정하면 유지보수가 거의 없다. 비밀번호 기반 인증은 Apple 계정 보안 정책이 바뀔 때마다 깨진다. 더 이상 쓸 이유가 없다.

**3. `draft` 릴리스 전략이 안전하다**

Google Play에 `release_status: "draft"`로 올리면 실수로 프로덕션 배포되는 사고를 방지한다. 자동화의 편의성과 안전성을 균형 있게 가져가려면, 마지막 출시 버튼은 수동으로 누르는 것이 낫다. CI에서 draft까지 올리고, 검증 후 수동 출시하는 패턴을 추천한다.

**4. 빌드 넘버는 반드시 동기화하라**

같은 앱 식별자를 쓰는 iOS/macOS는 빌드 넘버가 다르면 App Store Connect에서 혼란이 생긴다. `bump_build` 레인이 두 Xcode 프로젝트를 동시에 업데이트하고, 하위 레인에는 `skip_bump: true`를 전달해서 이중 증가를 막는 것이 핵심이다.

**5. Metadata-as-code로 스토어 카피를 버전 관리하라**

스토어 설명과 릴리스 노트를 `fastlane/metadata/` 디렉토리에서 파일로 관리하면 변경 이력 추적과 다국어 관리가 쉬워진다. "언제 이 문구를 바꿨더라?"를 Git 로그에서 확인할 수 있다. 스크린샷은 자동화보다 수동 관리가 현실적이다.

**6. 레인 계층화로 유연성을 확보하라**

최상위 레인이 하위 레인을 호출하는 구조를 만들면, 전체 배포와 개별 배포 모두 가능하다. CI에서는 전체를 실행하고, 로컬 디버깅 시에는 특정 레인만 실행한다. 레인을 너무 큰 단위로 만들면 중간 단계만 재실행하기 어려워진다.

---

다음 편에서는 이 Fastlane 파이프라인을 GitHub Actions로 자동화하는 CI/CD 구성을 다룬다.

[다음 편: Flutter CI/CD — GitHub Actions로 테스트부터 스토어 배포까지](blog-18-flutter-cicd-github-actions.md)
