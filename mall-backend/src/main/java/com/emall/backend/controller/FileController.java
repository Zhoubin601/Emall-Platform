package com.emall.backend.controller;

import com.emall.backend.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/uploadAvatar")
    public Map<String, String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return response(fileStorageService.storeImage(file, FileStorageService.ImageCategory.AVATAR));
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return response(fileStorageService.storeImage(file, FileStorageService.ImageCategory.COMMENT));
    }

    @PostMapping("/uploadCommon")
    public Map<String, String> uploadCommon(@RequestParam("file") MultipartFile file) {
        return response(fileStorageService.storeImage(file, FileStorageService.ImageCategory.COMMON));
    }

    private Map<String, String> response(String url) {
        return Map.of("url", url);
    }
}
