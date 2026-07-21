package com.emall.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void derivesExtensionFromFileSignatureAndIgnoresOriginalName() throws Exception {
        FileStorageService service = service(DataSize.ofMegabytes(5));
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        };
        MockMultipartFile upload = new MockMultipartFile(
                "file", "../../attack.exe", "application/octet-stream", png);

        String url = service.storeImage(upload, FileStorageService.ImageCategory.AVATAR);

        assertThat(url).startsWith("/uploads/avatars/").endsWith(".png");
        Path stored = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(stored.normalize()).startsWith(tempDir.toAbsolutePath().normalize());
        assertThat(Files.readAllBytes(stored)).isEqualTo(png);
    }

    @Test
    void rejectsContentThatOnlyClaimsToBeAnImage() {
        FileStorageService service = service(DataSize.ofMegabytes(5));
        MockMultipartFile upload = new MockMultipartFile(
                "file", "fake.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> service.storeImage(upload, FileStorageService.ImageCategory.COMMENT))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void rejectsOversizedImageBeforeWritingIt() {
        FileStorageService service = service(DataSize.ofBytes(8));
        MockMultipartFile upload = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6});

        assertThatThrownBy(() -> service.storeImage(upload, FileStorageService.ImageCategory.COMMON))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("413 PAYLOAD_TOO_LARGE");
    }

    private FileStorageService service(DataSize maximumSize) {
        FileStorageService service = new FileStorageService(tempDir.toString(), maximumSize);
        service.initialize();
        return service;
    }
}
