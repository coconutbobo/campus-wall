package com.campuswall.controller;

// 图片上传接口：只接受图片，单张不超过 5MB，登录后才能上传。
// 光看请求头里的文件类型是不够的，类型可以伪造，所以这里会读文件头部的真实字节来判断格式。
// 只接受 JPEG、PNG、GIF、WebP 四种真实格式，SVG 会直接拒绝，因为 SVG 里可以藏脚本，有跨站脚本风险。
// 保存时用随机文件名加真实格式后缀，不用用户传来的原始文件名，避免路径穿越和同名覆盖。
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

    // 根据文件头判断真实格式，返回对应的后缀，不符合四种格式就返回空。
    private String detectExtension(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return ".jpg";
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return ".png";
        }
        if (bytes.length >= 6 && bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46) {
            return ".gif";
        }
        if (bytes.length >= 12 && bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 && bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46 && bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45 && bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50) {
            return ".webp";
        }
        return null;
    }

    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty file");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image too large");
        }
        byte[] bytes = file.getBytes();
        String extension = detectExtension(bytes);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only jpg, png, gif and webp images are allowed");
        }
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID().toString() + extension;
        Files.write(dir.resolve(filename), bytes);
        return Map.of("url", "/uploads/" + filename);
    }
}
