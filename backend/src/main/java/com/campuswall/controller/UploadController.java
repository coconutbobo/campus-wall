// 校园墙项目
// 图片上传接口：只接受图片类型，单张不超过 5MB，保存到本地上传目录并返回图片地址。
package com.campuswall.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    @Value("${app.upload.dir}")
    private String uploadDir;

    @PostMapping
    // 上传图片：只接受图片文件，单张超过 5MB 就拒绝，保存后返回图片地址。
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty file");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only image allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image too large");
        }
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }
        String filename = UUID.randomUUID().toString() + extension;
        Files.copy(file.getInputStream(), dir.resolve(filename));
        return Map.of("url", "/uploads/" + filename);
    }
}
