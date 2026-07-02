package com.emall.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/file")
public class FileController {

    // ✨ 核心修复：统一使用 Docker 容器内部的挂载路径
    private final String UPLOAD_DIR_PATH = "/uploads/";

    // 1. 头像上传接口
    @PostMapping("/uploadAvatar")
    public Map<String, String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username
    ) throws IOException {
        File dir = new File(UPLOAD_DIR_PATH);
        if (!dir.exists()) dir.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

        String safeUsername = username.replaceAll("[\\\\/:*?\"<>|]", "");
        String newFileName = "avatar_" + safeUsername + "_" + System.currentTimeMillis() + suffix;

        File dest = new File(UPLOAD_DIR_PATH + newFileName);
        file.transferTo(dest);

        Map<String, String> res = new HashMap<>();
        res.put("url", "/uploads/" + newFileName);
        return res;
    }

    // 2. 商品评价上传接口
    @PostMapping("/upload")
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("nickname") String nickname,
            @RequestParam("productName") String productName
    ) throws IOException {
        File dir = new File(UPLOAD_DIR_PATH);
        if (!dir.exists()) dir.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String safeNickname = nickname.replaceAll("[\\\\/:*?\"<>|]", "");
        String safeProductName = productName.replaceAll("[\\\\/:*?\"<>|]", "");
        String newFileName = safeNickname + "_" + safeProductName + "_" + System.currentTimeMillis() + suffix;

        File dest = new File(UPLOAD_DIR_PATH + newFileName);
        file.transferTo(dest);

        Map<String, String> res = new HashMap<>();
        // ✨ 核心修复：干掉写死的 localhost:8080，改用相对路径保证 Vite 代理畅通
        res.put("url", "/uploads/" + newFileName);
        return res;
    }

    // 3. 通用文件/海报上传接口
    @PostMapping("/uploadCommon")
    public Map<String, String> uploadCommon(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        File dir = new File(UPLOAD_DIR_PATH);
        if (!dir.exists()) dir.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

        String newFileName = "common_" + System.currentTimeMillis() + suffix;
        File dest = new File(UPLOAD_DIR_PATH + newFileName);
        file.transferTo(dest);

        Map<String, String> res = new HashMap<>();
        res.put("url", "/uploads/" + newFileName);
        return res;
    }
}