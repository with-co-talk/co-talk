package com.cotalk.domain.validator;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.service.UploadFileService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 파일 메시지 서버사이드 검증기.
 * <p>
 * 파일 메시지는 클라이언트가 보낸 {@code fileUrl}, {@code contentType} 등을
 * 그대로 신뢰하지 않고, 서버에서 다시 검증한다.
 * </p>
 * <ul>
 *   <li>{@code contentType}: {@link UploadFileService#ALLOWED_CONTENT_TYPES} 허용 목록 재검증</li>
 *   <li>{@code fileUrl}/{@code thumbnailUrl}: 본 서버가 업로드 시 생성하는
 *       스토리지 객체 경로({@code uploads/{senderId}/...})에 속하는지,
 *       그리고 URL의 scheme/host/베이스 경로가 서버 스토리지 화이트리스트와
 *       일치하는지 검증하여 외부 URL·경로 탈출(traversal)·타인 소유 경로를 거부한다.</li>
 * </ul>
 * <p>
 * 검증 기준은 {@code UploadFileService}가 생성하는 실제 저장 경로 규칙
 * ({@code uploads/{userId}/{uuid}.{ext}})과, 실제 파일 저장소(MinIO 공개 URL,
 * MinIO presigned URL, InMemory)가 반환하는 URL의 베이스 주소에 근거한다.
 * </p>
 * <p>
 * <b>호스트 화이트리스트:</b> 업로드가 반환하는 URL은 항상
 * {@code {baseUrl}/.../uploads/{userId}/{file}} 형태이며, {@code baseUrl}의
 * scheme·host(·port)·베이스 path가 허용 목록 중 하나와 일치할 때만 통과시킨다.
 * 따라서 {@code http://evil.com/uploads/{senderId}/f.jpg} 처럼 path만 흉내 낸
 * 외부 URL은 host 불일치로 거부된다. 허용 목록이 비어 있으면(설정 미주입)
 * 하위 호환을 위해 host 검증을 건너뛰고 경로 검증만 수행한다.
 * </p>
 *
 * @author seunggu.lee
 * @see UploadFileService
 */
public class FileMessageValidator {

    /**
     * 업로드 객체 키의 최상위 디렉터리.
     * {@code UploadFileService.generateStoragePath}가 생성하는 {@code uploads/{userId}/...} 규칙과 일치한다.
     */
    private static final String UPLOAD_ROOT = "uploads";

    /**
     * 허용된 스토리지 베이스 URL 목록.
     * 각 항목은 실제 파일 저장소가 업로드 응답으로 반환하는 URL의 베이스
     * ({@code scheme://host[:port]/basePath}) 를 정규화한 값이다.
     * 비어 있으면 host 검증을 생략하고 경로 검증만 수행한다(하위 호환).
     */
    private final List<AllowedBase> allowedBases;

    /**
     * host 검증을 수행하지 않는 검증기를 생성한다(하위 호환용).
     * 경로 구조({@code uploads/{senderId}/...}) 검증만 수행한다.
     */
    public FileMessageValidator() {
        this.allowedBases = List.of();
    }

    /**
     * 허용된 스토리지 베이스 URL 화이트리스트로 검증기를 생성한다.
     *
     * @param allowedBaseUrls 실제 파일 저장소가 반환하는 URL의 베이스 목록
     *                        (예: {@code http://localhost:9000/cotalk},
     *                        {@code http://localhost:8080/files}). null/blank 항목은 무시된다.
     */
    public FileMessageValidator(List<String> allowedBaseUrls) {
        List<AllowedBase> bases = new ArrayList<>();
        if (allowedBaseUrls != null) {
            for (String baseUrl : allowedBaseUrls) {
                AllowedBase base = AllowedBase.parse(baseUrl);
                if (base != null && !bases.contains(base)) {
                    bases.add(base);
                }
            }
        }
        this.allowedBases = List.copyOf(bases);
    }

    /**
     * 파일 메시지의 contentType과 URL들을 서버사이드에서 검증한다.
     *
     * @param senderId     발신자(현재 인증된 사용자) ID
     * @param contentType  클라이언트가 보낸 MIME 타입
     * @param fileUrl      클라이언트가 보낸 파일 URL
     * @param thumbnailUrl 클라이언트가 보낸 썸네일 URL (선택, null 허용)
     * @throws FileUploadException 허용되지 않은 contentType이거나 신뢰할 수 없는 URL인 경우
     */
    public void validate(Long senderId, String contentType, String fileUrl, String thumbnailUrl) {
        validateContentType(contentType);
        validateOwnedUploadUrl(senderId, fileUrl);
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            validateOwnedUploadUrl(senderId, thumbnailUrl);
        }
    }

    /**
     * contentType이 업로드 허용 목록에 포함되는지 검증한다.
     *
     * @param contentType 검증할 MIME 타입
     * @throws FileUploadException 허용 목록에 없는 경우
     */
    private void validateContentType(String contentType) {
        if (contentType == null || !UploadFileService.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw FileUploadException.invalidFileType(contentType);
        }
    }

    /**
     * URL이 본 서버 업로드 객체 경로({@code uploads/{senderId}/...})에 속하는지 검증한다.
     * <p>
     * 절대 URL인 경우 scheme/host가 스토리지 화이트리스트와 일치하는지 먼저 확인하고,
     * 허용된 베이스 path 바로 뒤에 {@code uploads/{senderId}/{file}} 구조가
     * <b>정확히</b> 위치하는지 검증한다. 경로 탈출({@code ..}) 세그먼트는 거부한다.
     * </p>
     *
     * @param senderId 발신자 ID
     * @param url      검증할 URL
     * @throws FileUploadException URL이 본 서버 업로드 경로 규칙에 맞지 않는 경우
     */
    private void validateOwnedUploadUrl(Long senderId, String url) {
        URI uri = parseUri(url);
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw invalidFileUrl(url);
        }

        // 경로 탈출 세그먼트 거부 (디코딩된 path 기준)
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) {
                throw invalidFileUrl(url);
            }
        }

        if (uri.isAbsolute() || uri.getHost() != null) {
            // 절대 URL: scheme/host 화이트리스트 + 베이스 path 뒤 정확한 구조 검증
            validateAbsoluteUrl(senderId, url, uri, path);
        } else {
            // 상대 경로: 베이스가 없으므로 path가 정확히 uploads/{senderId}/{file} 로 시작해야 한다
            validateRelativePath(senderId, url, path);
        }
    }

    /**
     * 절대 URL을 scheme/host 화이트리스트와 정확한 경로 구조로 검증한다.
     */
    private void validateAbsoluteUrl(Long senderId, String url, URI uri, String path) {
        if (allowedBases.isEmpty()) {
            // 하위 호환: host 화이트리스트 미설정 시 경로 구조만 엄격 검증.
            // 이 경우 베이스 prefix를 특정할 수 없으므로 path 어디든 uploads/{senderId}/{file} 매칭을 허용한다.
            if (!matchesUploadStructureAnywhere(senderId, path)) {
                throw invalidFileUrl(url);
            }
            return;
        }

        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? null : uri.getHost().toLowerCase();
        int port = uri.getPort();

        for (AllowedBase base : allowedBases) {
            if (!base.matchesAuthority(scheme, host, port)) {
                continue;
            }
            // 베이스 path 바로 뒤에 uploads/{senderId}/{file} 가 정확히 위치하는지 확인
            if (base.matchesUploadPath(path, senderId)) {
                return;
            }
        }
        throw invalidFileUrl(url);
    }

    /**
     * 상대 경로를 정확히 {@code uploads/{senderId}/{file}} 로 시작하는지 검증한다.
     */
    private void validateRelativePath(Long senderId, String url, String path) {
        String normalized = stripLeadingSlash(path);
        if (allowedBases.isEmpty()) {
            // 하위 호환: 베이스 미설정 시 path 어디든 uploads/{senderId}/{file} 매칭 허용
            if (!matchesUploadStructureAnywhere(senderId, path)) {
                throw invalidFileUrl(url);
            }
            return;
        }
        // 베이스가 설정된 경우, 상대 경로는 베이스 path 뒤 구조와 동일하게 취급할 수 없으므로
        // uploads/{senderId}/{file} 로 정확히 시작하는 경우만 허용한다.
        if (!startsWithUploadStructure(normalized, senderId)) {
            throw invalidFileUrl(url);
        }
    }

    /**
     * path 어디서든 {@code uploads/{senderId}/{file}} 구조를 찾는다(하위 호환용 완화 매칭).
     */
    private boolean matchesUploadStructureAnywhere(Long senderId, String path) {
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if (UPLOAD_ROOT.equals(segments[i])) {
                return i + 2 < segments.length
                        && String.valueOf(senderId).equals(segments[i + 1])
                        && !segments[i + 2].isEmpty();
            }
        }
        return false;
    }

    /**
     * 정규화된 path(선행 슬래시 제거)가 정확히 {@code uploads/{senderId}/{file}...} 로 시작하는지 검사한다.
     */
    private boolean startsWithUploadStructure(String path, Long senderId) {
        String[] segments = path.split("/");
        return segments.length >= 3
                && UPLOAD_ROOT.equals(segments[0])
                && String.valueOf(senderId).equals(segments[1])
                && !segments[2].isEmpty();
    }

    private String stripLeadingSlash(String path) {
        int i = 0;
        while (i < path.length() && path.charAt(i) == '/') {
            i++;
        }
        return path.substring(i);
    }

    private URI parseUri(String url) {
        if (url == null || url.isBlank()) {
            throw invalidFileUrl(url);
        }
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw invalidFileUrl(url);
        }
    }

    /**
     * 신뢰할 수 없는 파일 URL 예외를 생성한다.
     *
     * @param url 문제가 된 URL
     * @return FileUploadException
     */
    private FileUploadException invalidFileUrl(String url) {
        return new FileUploadException("신뢰할 수 없는 파일 URL입니다: " + url);
    }

    /**
     * 허용된 스토리지 베이스(scheme/host/port + 베이스 path)를 표현하는 값 객체.
     */
    private static final class AllowedBase {

        private final String scheme;
        private final String host;
        private final int port;
        /** 선행/후행 슬래시를 제거한 베이스 path 세그먼트들 (예: {@code [cotalk]}, {@code [files]}). */
        private final List<String> basePathSegments;

        private AllowedBase(String scheme, String host, int port, List<String> basePathSegments) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.basePathSegments = basePathSegments;
        }

        /**
         * 베이스 URL 문자열을 파싱한다. 절대 URL이 아니거나 host가 없으면 null을 반환한다.
         */
        static AllowedBase parse(String baseUrl) {
            if (baseUrl == null || baseUrl.isBlank()) {
                return null;
            }
            try {
                URI uri = new URI(baseUrl.trim());
                if (!uri.isAbsolute() || uri.getHost() == null) {
                    return null;
                }
                String scheme = uri.getScheme().toLowerCase();
                String host = uri.getHost().toLowerCase();
                int port = normalizePort(scheme, uri.getPort());
                List<String> segments = new ArrayList<>();
                String path = uri.getPath();
                if (path != null) {
                    for (String s : path.split("/")) {
                        if (!s.isEmpty()) {
                            segments.add(s);
                        }
                    }
                }
                return new AllowedBase(scheme, host, port, List.copyOf(segments));
            } catch (URISyntaxException e) {
                return null;
            }
        }

        private static int normalizePort(String scheme, int port) {
            if (port != -1) {
                return port;
            }
            return switch (scheme) {
                case "http" -> 80;
                case "https" -> 443;
                default -> -1;
            };
        }

        boolean matchesAuthority(String scheme, String host, int port) {
            return this.scheme.equals(scheme)
                    && this.host.equals(host)
                    && this.port == normalizePort(scheme == null ? "" : scheme, port);
        }

        /**
         * URL path가 이 베이스의 path 바로 뒤에 {@code uploads/{senderId}/{file}} 구조를
         * 정확히 가지는지 검사한다.
         */
        boolean matchesUploadPath(String urlPath, Long senderId) {
            List<String> segments = new ArrayList<>();
            for (String s : urlPath.split("/")) {
                if (!s.isEmpty()) {
                    segments.add(s);
                }
            }
            int base = basePathSegments.size();
            // 베이스 prefix 일치 확인
            if (segments.size() < base) {
                return false;
            }
            for (int i = 0; i < base; i++) {
                if (!basePathSegments.get(i).equals(segments.get(i))) {
                    return false;
                }
            }
            // 베이스 바로 뒤: uploads / {senderId} / {file}
            if (segments.size() < base + 3) {
                return false;
            }
            return UPLOAD_ROOT.equals(segments.get(base))
                    && String.valueOf(senderId).equals(segments.get(base + 1))
                    && !segments.get(base + 2).isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AllowedBase other)) {
                return false;
            }
            return port == other.port
                    && scheme.equals(other.scheme)
                    && host.equals(other.host)
                    && basePathSegments.equals(other.basePathSegments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scheme, host, port, basePathSegments);
        }
    }
}
