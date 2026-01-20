package com.cotalk.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemoryFileStorage 단위 테스트.
 *
 * @author seunggu.lee
 */
@DisplayName("InMemoryFileStorage 단위 테스트")
class InMemoryFileStorageTest {

    private InMemoryFileStorage fileStorage;

    @BeforeEach
    void setUp() {
        fileStorage = new InMemoryFileStorage();
    }

    @Test
    @DisplayName("파일 업로드 성공")
    void should_uploadFile_when_validInput() {
        // given
        String content = "test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = "test.txt";

        // when
        String url = fileStorage.upload(inputStream, fileName, "text/plain", content.length());

        // then
        assertNotNull(url);
        assertTrue(url.contains(fileName));
        assertTrue(fileStorage.exists(fileName));
    }

    @Test
    @DisplayName("업로드된 파일 내용 확인")
    void should_storeContent_when_fileUploaded() {
        // given
        String content = "test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = "test.txt";

        // when
        fileStorage.upload(inputStream, fileName, "text/plain", content.length());

        // then
        byte[] storedContent = fileStorage.getFile(fileName);
        assertNotNull(storedContent);
        assertEquals(content, new String(storedContent, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("파일 삭제 성공")
    void should_deleteFile_when_fileExists() {
        // given
        String content = "test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = "test.txt";
        fileStorage.upload(inputStream, fileName, "text/plain", content.length());

        // when
        fileStorage.delete(fileName);

        // then
        assertFalse(fileStorage.exists(fileName));
        assertNull(fileStorage.getFile(fileName));
    }

    @Test
    @DisplayName("존재하지 않는 파일 삭제 시 예외 없음")
    void should_notThrow_when_deletingNonExistentFile() {
        // given
        String fileName = "non-existent.txt";

        // when & then
        assertDoesNotThrow(() -> fileStorage.delete(fileName));
    }

    @Test
    @DisplayName("파일 존재 여부 확인 - 존재하는 파일")
    void should_returnTrue_when_fileExists() {
        // given
        String content = "test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String fileName = "test.txt";
        fileStorage.upload(inputStream, fileName, "text/plain", content.length());

        // when
        boolean exists = fileStorage.exists(fileName);

        // then
        assertTrue(exists);
    }

    @Test
    @DisplayName("파일 존재 여부 확인 - 존재하지 않는 파일")
    void should_returnFalse_when_fileNotExists() {
        // given
        String fileName = "non-existent.txt";

        // when
        boolean exists = fileStorage.exists(fileName);

        // then
        assertFalse(exists);
    }

    @Test
    @DisplayName("Pre-signed URL 생성 성공")
    void should_generatePresignedUrl_when_validFileName() {
        // given
        String fileName = "test.txt";
        int expirationMinutes = 30;

        // when
        String url = fileStorage.generatePresignedUrl(fileName, expirationMinutes);

        // then
        assertNotNull(url);
        assertTrue(url.contains(fileName));
        assertTrue(url.contains("expires=" + expirationMinutes));
    }

    @Test
    @DisplayName("모든 파일 삭제 (clear)")
    void should_clearAllFiles_when_clearCalled() {
        // given
        fileStorage.upload(
                new ByteArrayInputStream("content1".getBytes()),
                "file1.txt", "text/plain", 8);
        fileStorage.upload(
                new ByteArrayInputStream("content2".getBytes()),
                "file2.txt", "text/plain", 8);

        // when
        fileStorage.clear();

        // then
        assertFalse(fileStorage.exists("file1.txt"));
        assertFalse(fileStorage.exists("file2.txt"));
    }

    @Test
    @DisplayName("업로드 URL 형식 확인")
    void should_returnCorrectUrlFormat_when_upload() {
        // given
        String content = "test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        String fileName = "test.txt";

        // when
        String url = fileStorage.upload(inputStream, fileName, "text/plain", content.length());

        // then
        assertTrue(url.startsWith("http://localhost:8080/files/"));
        assertTrue(url.endsWith(fileName));
    }

    @Test
    @DisplayName("동일 파일명 업로드 시 덮어쓰기")
    void should_overwrite_when_sameFileNameUploaded() {
        // given
        String fileName = "test.txt";
        String content1 = "first content";
        String content2 = "second content";

        fileStorage.upload(
                new ByteArrayInputStream(content1.getBytes()),
                fileName, "text/plain", content1.length());

        // when
        fileStorage.upload(
                new ByteArrayInputStream(content2.getBytes()),
                fileName, "text/plain", content2.length());

        // then
        byte[] storedContent = fileStorage.getFile(fileName);
        assertEquals(content2, new String(storedContent, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("업로드 시 IOException 발생하면 RuntimeException 던짐")
    void should_throwRuntimeException_when_ioExceptionOccurs() {
        // given
        InputStream failingInputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Read failed");
            }
        };
        String fileName = "test.txt";

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileStorage.upload(failingInputStream, fileName, "text/plain", 100));
        assertEquals("Failed to read file", exception.getMessage());
    }

    @Test
    @DisplayName("빈 파일 업로드 성공")
    void should_uploadEmptyFile_when_emptyContent() {
        // given
        byte[] emptyContent = new byte[0];
        InputStream inputStream = new ByteArrayInputStream(emptyContent);
        String fileName = "empty.txt";

        // when
        String url = fileStorage.upload(inputStream, fileName, "text/plain", 0);

        // then
        assertNotNull(url);
        assertTrue(fileStorage.exists(fileName));
        assertArrayEquals(emptyContent, fileStorage.getFile(fileName));
    }

    @Test
    @DisplayName("특수 문자가 포함된 파일명 처리")
    void should_handleSpecialCharacters_when_fileNameContainsSpecialChars() {
        // given
        String content = "test content";
        String fileName = "test-file_2024.txt";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        // when
        String url = fileStorage.upload(inputStream, fileName, "text/plain", content.length());

        // then
        assertNotNull(url);
        assertTrue(url.contains(fileName));
        assertTrue(fileStorage.exists(fileName));
    }
}
