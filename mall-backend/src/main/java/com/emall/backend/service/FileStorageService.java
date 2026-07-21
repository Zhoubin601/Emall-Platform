package com.emall.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path uploadRoot;
    private final long maxImageSize;

    public FileStorageService(
            @Value("${storage.upload-dir:/uploads}") String uploadDir,
            @Value("${storage.image-max-size:5MB}") DataSize maxImageSize) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.maxImageSize = maxImageSize.toBytes();
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("无法初始化上传目录", exception);
        }
    }

    public String storeImage(MultipartFile file, ImageCategory category) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要上传的图片");
        }
        if (file.getSize() > maxImageSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "图片大小超过系统限制");
        }

        try {
            byte[] content = file.getBytes();
            ImageType type = detectImageType(content);
            Path categoryDir = uploadRoot.resolve(category.directory()).normalize();
            ensureWithinUploadRoot(categoryDir);
            Files.createDirectories(categoryDir);

            String fileName = UUID.randomUUID() + "." + type.extension();
            Path target = categoryDir.resolve(fileName).normalize();
            ensureWithinUploadRoot(target);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
            return "/uploads/" + category.directory() + "/" + fileName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败", exception);
        }
    }

    private void ensureWithinUploadRoot(Path path) {
        if (!path.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传路径不合法");
        }
    }

    private ImageType detectImageType(byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) return ImageType.JPEG;
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return ImageType.PNG;
        if (startsWithAscii(bytes, "GIF87a") || startsWithAscii(bytes, "GIF89a")) return ImageType.GIF;
        if (bytes.length >= 12 && startsWithAscii(bytes, "RIFF")
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ImageType.WEBP;
        }
        throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "仅支持 JPEG、PNG、WebP 或 GIF 图片");
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != signature[index]) return false;
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, String signature) {
        if (bytes.length < signature.length()) return false;
        for (int index = 0; index < signature.length(); index++) {
            if (bytes[index] != (byte) signature.charAt(index)) return false;
        }
        return true;
    }

    public enum ImageCategory {
        AVATAR("avatars"), COMMENT("comments"), COMMON("common");

        private final String directory;

        ImageCategory(String directory) {
            this.directory = directory;
        }

        public String directory() {
            return directory;
        }
    }

    private enum ImageType {
        JPEG("jpg"), PNG("png"), GIF("gif"), WEBP("webp");

        private final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }

        String extension() {
            return extension;
        }
    }
}
