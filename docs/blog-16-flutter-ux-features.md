# Flutter UX 깊이 파기 — 테마, 생체인증, 오프라인 캐시

> "앱이 잠금 해제될 때마다 비밀번호를 치게 할 순 없잖아?"

채팅 앱에서 사용자 경험을 결정짓는 요소는 생각보다 많다. 메시지 전송 속도나 알림 정확도처럼 눈에 띄는 기능만이 아니라, "앱이 어두운 방에서도 눈이 안 아프냐", "자리를 잠깐 비웠다 돌아왔을 때 매번 잠금을 풀어야 하냐", "인터넷이 끊겼을 때 이전 대화를 볼 수 있냐" 같은 것들이 실제 사용 만족도를 좌우한다.

이번 편은 co-talk Flutter 앱에서 구현한 세 가지 UX 기능을 다룬다.

1. **테마 시스템** — Light/Dark/System 전환 + TextScaler로 전역 폰트 크기 조절
2. **생체인증 & 앱 잠금** — Grace Period와 Hybrid 동기/비동기 캐시
3. **Drift(SQLite) 오프라인 캐시** — FTS5 전문 검색과 syncStatus 관리

그리고 마지막에 이 과정에서 발견한 설정 상태 리셋 버그 — `copyWith`의 함정도 정리한다.

시리즈 16편이다.

---

## 1. 테마 시스템 — Light/Dark/System + TextScaler

### Material 3 기반 테마 정의

Flutter 3.8부터 Material 3가 기본이다. `useMaterial3: true`를 명시하고, `ColorScheme`으로 색상 팔레트를 한 곳에서 관리한다.

```dart
// lib/core/theme/app_theme.dart
class AppTheme {
  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      primaryColor: AppColors.primary,
      colorScheme: const ColorScheme.light(
        primary: AppColors.primary,
        secondary: AppColors.secondary,
        surface: AppColors.surface,
        error: AppColors.error,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
      ),
    );
  }

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.backgroundDark,
      colorScheme: const ColorScheme.dark(
        surface: AppColors.surfaceDark,
      ),
    );
  }
}
```

`AppColors`는 별도 파일로 분리해서 색상값을 한 곳에서 관리한다. 테마 파일에서 직접 `Color(0xFF...)` 를 쓰면 나중에 브랜드 컬러가 바뀔 때 여러 파일을 수정해야 한다.

### ThemeCubit — 설정 저장 + 시스템 테마 판별

```dart
// lib/presentation/blocs/theme/theme_cubit.dart
@lazySingleton
class ThemeCubit extends Cubit<ThemeMode> {
  final ThemeLocalDataSource _dataSource;
  ThemeCubit(this._dataSource) : super(ThemeMode.system);

  Future<void> loadTheme() async {
    final savedMode = await _dataSource.getThemeMode();
    emit(savedMode ?? ThemeMode.system);
  }

  Future<void> setTheme(ThemeMode mode) async {
    await _dataSource.saveThemeMode(mode);
    emit(mode);
  }

  bool isDarkMode(BuildContext context) {
    if (state == ThemeMode.system) {
      return MediaQuery.platformBrightnessOf(context) == Brightness.dark;
    }
    return state == ThemeMode.dark;
  }
}
```

`@lazySingleton`이 붙어 있어서 GetIt이 첫 번째 요청 시 인스턴스를 생성하고 이후로는 같은 인스턴스를 재사용한다. `loadTheme()`은 앱 시작 시 한 번만 호출하면 된다.

`isDarkMode(context)`가 필요한 이유: `ThemeMode.system`일 때 현재 실제로 다크인지 라이트인지 알려면 OS 밝기를 확인해야 한다. `MediaQuery.platformBrightnessOf(context)`가 그 값을 준다.

### TextScaler — 전역 폰트 크기 조절

사용자가 설정에서 폰트 크기를 0.8~1.4 사이로 설정할 수 있게 했다. 이 값을 모든 위젯에 적용하는 방법은 `MaterialApp.router`의 `builder` 콜백에서 `MediaQuery`를 감싸는 것이다.

```dart
// lib/app.dart
MaterialApp.router(
  theme: AppTheme.lightTheme,
  darkTheme: AppTheme.darkTheme,
  themeMode: themeMode,
  builder: (context, child) {
    return MediaQuery(
      data: MediaQuery.of(context).copyWith(
        textScaler: TextScaler.linear(chatSettingsState.settings.fontSize),
      ),
      child: _AppLifecycleHandler(
        child: Stack(
          children: [
            child ?? const SizedBox.shrink(),
            BlocBuilder<AppLockCubit, AppLockState>(
              builder: (context, lockState) {
                if (lockState.status == AppLockStatus.unlocked) {
                  return const SizedBox.shrink();
                }
                return const AppLockPage();
              },
            ),
          ],
        ),
      ),
    );
  },
),
```

`builder` 콜백에서 `MediaQuery`를 새로 만들어 씌우면 모든 하위 위젯이 덮어쓴 값을 읽는다. `Text`, `RichText`, `TextField` 등 폰트를 사용하는 모든 위젯에 자동으로 적용된다. 개별 위젯마다 스타일을 바꿀 필요가 없다.

`Stack`의 구조를 보면 앱 본체(`child`)와 `AppLockPage`가 겹쳐 있다. 잠금 상태일 때 `AppLockPage`가 화면 전체를 덮는 방식이다. 이 구조에 대해서는 다음 섹션에서 자세히 다룬다.

<!-- IMAGE: 테마 설정 화면 스크린샷 — Light/Dark/System 토글과 폰트 크기 슬라이더 -->

---

## 2. 생체인증 & 앱 잠금

### 앱 잠금 생명주기

앱 잠금은 두 가지 진입점이 있다: 앱 시작(`checkLockOnLaunch`)과 백그라운드에서 포그라운드 복귀(`checkLockOnResume`). 두 경우의 동작이 다르다.

```plantuml
@startuml
!theme plain
start
:앱 시작 (main.dart);
:checkLockOnLaunch();

if (biometric 활성화?) then (yes)
  :잠금 화면 표시;
  :생체인증 시도;
  if (성공?) then (yes)
    :_lastAuthenticatedAt = now;
    :잠금 해제;
  else (no)
    :잠금 유지\n수동 재시도 버튼;
  endif
else (no)
  :바로 앱 진입;
endif

... 앱 사용 중 ...

:앱 백그라운드 → 포그라운드;
:checkLockOnResume();

if (_cachedBiometricEnabled?) then (yes)
  if (Grace Period 30초 이내?) then (yes)
    :잠금 건너뜀;
  else (no)
    :즉시 잠금 (캐시 기반);
    :생체인증 시도;
  endif
else (no)
  :_refreshCache()\nSecureStorage 확인;
endif
stop
@enduml
```

앱 시작 시에는 Grace Period가 없다. 재부팅 후 앱을 처음 여는 경우에는 반드시 인증해야 한다. 반면 포그라운드 복귀 시에는 30초 유예 기간을 준다. 사진 앱에서 사진을 고른 뒤 돌아올 때마다 지문을 찍어야 한다면 아무도 안 쓴다.

### Hybrid 동기/비동기 캐시 전략

```dart
// lib/presentation/blocs/app/app_lock_cubit.dart
@lazySingleton
class AppLockCubit extends Cubit<AppLockState> {
  bool _cachedBiometricEnabled = false;
  bool _cacheLoaded = false;
  DateTime? _lastAuthenticatedAt;
  static const _authGracePeriod = Duration(seconds: 30);

  Future<void> checkLockOnResume() async {
    // 1단계: 동기 캐시로 즉시 판단
    if (_cacheLoaded && _cachedBiometricEnabled) {
      if (_isWithinGracePeriod()) return;
      emit(const AppLockState.locked());
      return;
    }
    // 2단계: 비동기 SecureStorage 확인 (fallback)
    await _refreshCache();
    if (!_cachedBiometricEnabled) return;
    if (_isWithinGracePeriod()) return;
    emit(const AppLockState.locked());
  }

  Future<void> checkLockOnLaunch() async {
    await _refreshCache();
    if (!_cachedBiometricEnabled) return;
    emit(const AppLockState.locked());  // 앱 시작 시 grace period 없음
  }

  Future<void> authenticate() async {
    emit(const AppLockState.authenticating());
    final success = await _biometricService.authenticate();
    if (success) {
      _lastAuthenticatedAt = DateTime.now();
      emit(const AppLockState.unlocked());
    } else {
      emit(const AppLockState.locked());
    }
  }

  bool _isWithinGracePeriod() {
    if (_lastAuthenticatedAt == null) return false;
    return DateTime.now().difference(_lastAuthenticatedAt!) < _authGracePeriod;
  }

  Future<void> _refreshCache() async {
    _cachedBiometricEnabled =
        await _secureStorage.read(key: 'biometric_enabled') == 'true';
    _cacheLoaded = true;
  }

  void updateBiometricEnabledCache(bool enabled) {
    _cachedBiometricEnabled = enabled;
    _cacheLoaded = true;
  }
}
```

왜 동기 캐시(`_cachedBiometricEnabled`)가 필요한지 설명하겠다.

`WidgetsBindingObserver.didChangeAppLifecycleState`는 앱 생명주기 변화를 감지하는 콜백이다. 이 콜백에서 `SecureStorage`를 읽으면 문제가 생긴다. `SecureStorage`는 iOS Keychain, Android Keystore를 통해 데이터를 읽는 비동기 작업이다. `await`로 기다리는 동안 약 0.5초 정도 지연이 발생한다.

이 0.5초가 보안 구멍이 된다. 포그라운드로 전환되고 나서 잠금 화면이 뜨기까지 반 박자 늦는 것이다. 그 사이에 화면이 잠깐 보인다.

해결책이 Hybrid 전략이다. 메모리에 동기 캐시를 유지해서 즉시 잠금 여부를 판단하고, 비동기 `SecureStorage` 확인은 캐시가 없을 때만 fallback으로 사용한다. 설정 화면에서 생체인증을 켜거나 끌 때 `updateBiometricEnabledCache()`를 호출해서 캐시를 즉시 업데이트한다.

### AppLockPage — 자동 생체인증 트리거

```dart
// lib/presentation/pages/app_lock/app_lock_page.dart
class AppLockPage extends StatefulWidget {
  const AppLockPage({super.key});

  @override
  State<AppLockPage> createState() => _AppLockPageState();
}

class _AppLockPageState extends State<AppLockPage> {
  bool _autoAuthTriggered = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _tryAutoAuthenticate();
    });
  }

  Future<void> _tryAutoAuthenticate() async {
    if (_autoAuthTriggered) return;
    _autoAuthTriggered = true;
    await context.read<AppLockCubit>().authenticate();
  }

  void _onUnlockSuccess() {
    _autoAuthTriggered = false; // 다음 잠금 시 다시 자동 시도
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<AppLockCubit, AppLockState>(
      listener: (context, state) {
        if (state.status == AppLockStatus.unlocked) {
          _onUnlockSuccess();
        }
      },
      child: Scaffold(
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.lock_outline, size: 64),
              const SizedBox(height: 24),
              const Text('앱이 잠겨 있습니다'),
              const SizedBox(height: 32),
              ElevatedButton.icon(
                onPressed: _tryAutoAuthenticate,
                icon: const Icon(Icons.fingerprint),
                label: const Text('생체인증으로 잠금 해제'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
```

`_autoAuthTriggered` 플래그가 핵심이다. 잠금 화면이 표시되면 `initState`에서 자동으로 생체인증을 시도한다. 실패했을 때 다시 `_tryAutoAuthenticate()`를 호출하면 `_autoAuthTriggered`가 이미 `true`라서 무시된다. 이렇게 무한 루프를 방지한다.

잠금이 해제되면(`AppLockStatus.unlocked`) `_autoAuthTriggered`를 `false`로 리셋한다. 다음에 다시 잠길 때 자동 인증이 작동하도록.

<!-- IMAGE: 앱 잠금 화면 스크린샷 — 지문 아이콘과 잠금 해제 버튼 -->

---

## 3. Drift 오프라인 캐시

### 왜 Drift인가

처음에는 `shared_preferences`로 메시지를 JSON 직렬화해서 저장했다. 방 하나에 메시지 수백 개가 생기면 JSON 파싱에만 수십 밀리초가 걸렸다. 인덱스가 없으니 특정 메시지를 찾으려면 전체를 순회해야 했다.

Drift는 Flutter용 SQLite 래퍼다. 타입 안전한 Dart API를 제공하고, 컴파일 타임에 SQL을 검증한다. 그리고 FTS5(Full-Text Search 5) 확장으로 전문 검색을 지원한다.

### 테이블 정의

```dart
// lib/data/datasources/local/database/tables.dart
class Messages extends Table {
  IntColumn get id => integer()();
  IntColumn get chatRoomId => integer()();
  IntColumn get senderId => integer()();
  TextColumn get senderNickname => text().nullable()();
  TextColumn get content => text()();
  TextColumn get type => text().withDefault(const Constant('TEXT'))();
  TextColumn get fileUrl => text().nullable()();
  IntColumn get replyToMessageId => integer().nullable()();
  BoolColumn get isDeleted => boolean().withDefault(const Constant(false))();
  IntColumn get createdAt => integer()();  // Unix ms
  TextColumn get syncStatus =>
      text().withDefault(const Constant('synced'))();

  @override
  Set<Column> get primaryKey => {id};
}

class ChatRooms extends Table {
  IntColumn get id => integer()();
  TextColumn get type => text().withDefault(const Constant('DIRECT'))();
  TextColumn get lastMessage => text().nullable()();
  IntColumn get unreadCount => integer().withDefault(const Constant(0))();
  IntColumn get otherUserId => integer().nullable()();
  BoolColumn get isOtherUserOnline =>
      boolean().withDefault(const Constant(false))();
  IntColumn get lastSyncAt => integer().nullable()();
}

class MessageReactions extends Table {
  IntColumn get messageId => integer()();
  IntColumn get userId => integer()();
  TextColumn get emoji => text()();
}
```

`syncStatus` 컬럼이 오프라인 지원의 핵심이다. `synced`, `pending`, `failed` 세 값을 갖는다.

- `synced`: 서버와 동기화 완료
- `pending`: 오프라인 중 전송 대기
- `failed`: 전송 시도했지만 실패

앱이 온라인으로 전환될 때 `syncStatus = 'pending'`인 메시지를 찾아 재전송한다.

### 데이터베이스 초기화와 백그라운드 Isolate

```dart
// lib/data/datasources/local/database/app_database.dart
@DriftDatabase(tables: [Messages, ChatRooms, MessageReactions])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 3;

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      onCreate: (Migrator m) async {
        await m.createAll();
        await _createFtsTable(m);
      },
      onUpgrade: (Migrator m, int from, int to) async {
        if (from < 2) {
          await m.addColumn(messages, messages.syncStatus);
        }
        if (from < 3) {
          await _createFtsTable(m);
        }
      },
    );
  }
}

QueryExecutor _openConnection() {
  return NativeDatabase.createInBackground(
    File(path.join(documentsDirectory, 'co_talk.db')),
    logStatements: kDebugMode,
  );
}
```

`NativeDatabase.createInBackground`가 중요하다. SQLite 작업을 별도 Isolate에서 실행해서 UI 스레드를 블로킹하지 않는다. 메시지 수천 개를 한 번에 INSERT하더라도 앱이 멈추지 않는다.

### FTS5 전문 검색

```dart
// lib/data/datasources/local/database/app_database.dart
Future<void> _createFtsTable(Migrator m) async {
  await customStatement('''
    CREATE VIRTUAL TABLE IF NOT EXISTS messages_fts USING fts5(
      content, file_name,
      content='messages', content_rowid='id'
    )
  ''');

  await customStatement('''
    CREATE TRIGGER IF NOT EXISTS messages_ai AFTER INSERT ON messages BEGIN
      INSERT INTO messages_fts(rowid, content, file_name)
      VALUES (NEW.id, NEW.content, NEW.file_name);
    END
  ''');

  await customStatement('''
    CREATE TRIGGER IF NOT EXISTS messages_ad AFTER DELETE ON messages BEGIN
      INSERT INTO messages_fts(messages_fts, rowid, content, file_name)
      VALUES ('delete', OLD.id, OLD.content, OLD.file_name);
    END
  ''');

  await customStatement('''
    CREATE TRIGGER IF NOT EXISTS messages_au AFTER UPDATE ON messages BEGIN
      INSERT INTO messages_fts(messages_fts, rowid, content, file_name)
      VALUES ('delete', OLD.id, OLD.content, OLD.file_name);
      INSERT INTO messages_fts(rowid, content, file_name)
      VALUES (NEW.id, NEW.content, NEW.file_name);
    END
  ''');
}
```

FTS5 Virtual Table은 일반 테이블과 연결(`content='messages'`)되어 있다. INSERT/DELETE/UPDATE 트리거 세 개가 `messages` 테이블 변경 시 자동으로 FTS 인덱스를 동기화한다. 이후 DAO에서는 검색할 때만 FTS 테이블을 조인하면 된다.

### 검색 쿼리

```dart
// lib/data/datasources/local/database/daos/message_dao.dart
Future<List<Message>> searchMessages(String query, {int? chatRoomId}) async {
  final escaped = query.replaceAll('"', '""');
  String sql = '''
    SELECT m.* FROM messages m
    INNER JOIN messages_fts ON m.id = messages_fts.rowid
    WHERE messages_fts MATCH ?
  ''';
  final variables = <Variable>[Variable.withString('"$escaped"*')];

  if (chatRoomId != null) {
    sql += ' AND m.chat_room_id = ?';
    variables.add(Variable.withInt(chatRoomId));
  }

  sql += ' AND m.is_deleted = 0 ORDER BY m.created_at DESC LIMIT 50';

  final rows = await customSelect(sql, variables: variables).get();
  return rows.map((row) => Message.fromData(row.data)).toList();
}
```

`"$escaped"*`가 prefix wildcard 검색이다. `"안녕"*`으로 검색하면 "안녕하세요", "안녕하십니까"가 모두 매칭된다. 쌍따옴표 안의 공백은 구문(phrase) 검색이 되므로, 사용자 입력에서 쌍따옴표를 이스케이프(`"` → `""`)해야 한다.

### 로컬-리모트 동기화 전략

```dart
// lib/data/repositories/chat_message_repository_impl.dart
@override
Stream<List<ChatMessage>> watchMessages(int chatRoomId) async* {
  // 1. 로컬 DB에서 즉시 데이터 반환
  yield await _localDataSource.getMessages(chatRoomId);

  // 2. 백그라운드에서 서버 동기화
  _syncFromServer(chatRoomId).ignore();

  // 3. 이후로는 로컬 DB 변경 스트림을 구독
  yield* _localDataSource.watchMessages(chatRoomId);
}

Future<void> _syncFromServer(int chatRoomId) async {
  try {
    final lastSyncAt = await _localDataSource.getLastSyncAt(chatRoomId);
    final remoteMessages = await _remoteDataSource.getMessages(
      chatRoomId,
      after: lastSyncAt,
    );
    await _localDataSource.upsertMessages(remoteMessages);
    await _localDataSource.updateLastSyncAt(chatRoomId, DateTime.now());
  } catch (e) {
    // 네트워크 오류는 무시 — 로컬 캐시로 계속 서비스
  }
}
```

이 패턴이 "stale-while-revalidate"다. 로컬 데이터를 즉시 보여주고, 백그라운드에서 서버와 동기화한 뒤 차이가 있으면 UI가 자동으로 업데이트된다. Drift의 `watchMessages()`는 `Stream`을 반환해서 DB 변경이 생기면 자동으로 새 데이터를 방출한다.

<!-- IMAGE: 오프라인 상태에서 채팅방 접속 — 로컬 캐시로 이전 메시지 표시되는 스크린샷 -->

---

## 4. 설정 상태 리셋 버그 — copyWith의 함정

### 버그 발견

어느 날 다크 모드를 켜고 채팅 설정으로 들어갔더니 폰트 크기가 기본값으로 초기화되어 있었다. 설정을 바꾼 적도 없는데.

코드를 추적하니 `ChatSettingsCubit`의 named constructor가 문제였다.

```dart
// 문제 있는 코드
class ChatSettingsState {
  final ChatSettings settings;
  final ChatSettingsStatus status;

  const ChatSettingsState({
    required this.settings,
    required this.status,
  });

  // 이 생성자가 문제
  const ChatSettingsState.loading() : this(
    settings: ChatSettings.defaults(),  // 기본값으로 리셋!
    status: ChatSettingsStatus.loading,
  );
}

// Cubit에서
Future<void> saveSettings(ChatSettings newSettings) async {
  emit(ChatSettingsState.loading());  // settings가 기본값으로 리셋됨
  await _repository.save(newSettings);
  emit(ChatSettingsState.loaded(newSettings));
}
```

`ChatSettingsState.loading()`이 `this()`로 생성자를 위임할 때 `settings: ChatSettings.defaults()`를 명시했다. 즉 로딩 상태로 전환하는 순간 현재 설정이 기본값으로 날아간다.

저장 완료 전에 에러가 나거나, 로딩 상태가 UI에 잠깐이라도 반영되면 사용자는 설정이 초기화된 것을 본다.

### 해결: copyWith 사용

```dart
// lib/presentation/blocs/settings/chat_settings_cubit.dart
Future<void> saveSettings(ChatSettings newSettings) async {
  // 기존 settings를 유지하면서 status만 변경
  emit(state.copyWith(status: ChatSettingsStatus.loading));

  try {
    await _repository.save(newSettings);
    emit(state.copyWith(
      settings: newSettings,
      status: ChatSettingsStatus.loaded,
    ));
  } catch (e) {
    emit(state.copyWith(status: ChatSettingsStatus.error));
  }
}
```

`state.copyWith(status: newStatus)`는 현재 상태의 모든 필드를 복사하고 지정한 필드만 바꾼다. `settings`는 건드리지 않으니 기존 값이 유지된다.

`@lazySingleton` Cubit에서 `loadSettings()`는 초기 상태일 때만 호출해야 한다는 것도 배웠다. 싱글턴이라 앱 생명주기 동안 같은 인스턴스가 유지되는데, 화면에 들어올 때마다 `loadSettings()`를 부르면 이미 설정을 불러온 상태에서 다시 로딩 상태로 전환된다.

```dart
@override
void initState() {
  super.initState();
  final cubit = context.read<ChatSettingsCubit>();
  // 초기 상태일 때만 로드 (싱글턴 중복 로드 방지)
  if (cubit.state.status == ChatSettingsStatus.initial) {
    cubit.loadSettings();
  }
}
```

### NotificationSettingsCubit — Optimistic Update

알림 설정에서는 한 발 더 나아가 낙관적 업데이트(Optimistic Update)를 적용했다.

```dart
// lib/presentation/blocs/settings/notification_settings_cubit.dart
Future<void> _updateSetting(NotificationSettings newSettings) async {
  final previousSettings = state.settings;

  // UI 즉시 업데이트 (낙관적)
  emit(state.copyWith(settings: newSettings));

  try {
    await _repository.updateNotificationSettings(newSettings);
  } catch (e) {
    // 실패 시 이전 설정으로 롤백
    emit(state.copyWith(
      status: NotificationSettingsStatus.error,
      settings: previousSettings,
    ));
  }
}
```

토글을 누르면 서버 응답을 기다리지 않고 즉시 UI가 반응한다. 서버 저장이 실패하면 이전 설정으로 되돌린다. 설정 화면에서 이 정도 latency 최적화가 필요한가 싶겠지만, 네트워크가 느린 환경에서 토글이 0.5초 후에 반응한다면 "내가 제대로 누른 건지" 확신이 안 선다.

---

## 5. 교훈

이번에 세 기능을 구현하면서 얻은 것들을 정리한다.

**1. TextScaler는 builder에서 덮어써라**

`MaterialApp.router`의 `builder` 콜백에서 `MediaQuery`를 감싸면 모든 하위 위젯에 사용자 폰트 크기가 적용된다. 개별 위젯마다 `fontSize`를 주입할 필요가 없다. 전역 설정은 전역적으로 적용하는 것이 맞다.

**2. 앱 잠금은 동기 캐시가 핵심**

`SecureStorage`는 비동기라 `didChangeAppLifecycleState`에서 바로 쓸 수 없다. `await`로 기다리는 0.5초 동안 잠금 화면이 늦게 뜨는 보안 구멍이 생긴다. 동기 메모리 캐시(`_cachedBiometricEnabled`)로 즉시 잠금하고, 비동기 `SecureStorage` 확인은 캐시가 없을 때만 fallback으로 사용하라.

**3. Grace Period가 UX를 결정한다**

사진 앱에서 파일을 고르고 돌아올 때, 카메라를 켰다 끄고 돌아올 때마다 생체인증을 요구하면 사용자가 떠난다. 30초 유예 기간이 보안과 편의성 사이의 현실적인 타협점이다. 카카오톡도 이 패턴을 쓴다.

**4. FTS5 트리거는 설정하면 잊어도 된다**

INSERT/DELETE/UPDATE 트리거로 검색 인덱스를 자동 동기화하면 DAO에서 별도로 FTS 테이블을 관리할 필요가 없다. 트리거 세 개가 데이터 일관성을 보장한다. 검색할 때만 FTS 테이블을 조인하면 끝이다.

**5. named constructor의 `this()` 위임을 의심하라**

Dart의 생성자 위임(`this()`)은 명시하지 않은 필드를 기본값으로 리셋한다. 상태 전환 시(`loading`, `saving` 등) 반드시 `state.copyWith()`를 사용해서 기존 필드를 보존하라. 특히 `@lazySingleton` Cubit에서 이 버그는 재현이 어렵다. 화면에 처음 들어갈 때는 괜찮고 두 번째 이후 동작에서만 문제가 나타나기 때문이다.

**6. Drift의 `NativeDatabase.createInBackground`는 필수**

메인 스레드에서 SQLite를 돌리면 대용량 INSERT 시 UI가 버벅인다. `createInBackground`로 별도 Isolate에서 실행하는 것이 기본값이어야 한다.

**7. 오프라인 캐시의 stale-while-revalidate 패턴**

로컬 데이터를 즉시 보여주고, 백그라운드에서 서버와 동기화한다. 차이가 생기면 Drift의 `watch()` 스트림이 자동으로 UI를 업데이트한다. 사용자는 네트워크 상태에 상관없이 앱을 쓸 수 있다.

---

다음 편에서는 Fastlane으로 iOS, macOS, Android 세 플랫폼을 한 번에 배포하는 과정을 다룬다.

[다음 편: Fastlane으로 iOS·macOS·Android 한 번에 배포하기](blog-17-fastlane-multiplatform.md)
