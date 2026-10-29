package com.zhiguang.controller;

import com.zhiguang.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileController {

    /** 允许的图片扩展名（SVG/HTML 可携带脚本，一律拒绝） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 扩展名 → 文件头魔数校验器 */
    private static final Map<String, java.util.function.Predicate<byte[]>> MAGIC_CHECKERS = Map.of(
            "jpg", bytes -> bytes.length > 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF,
            "jpeg", bytes -> bytes.length > 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF,
            "png", bytes -> bytes.length > 3 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47,
            "gif", bytes -> bytes.length > 3 && bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38,
            "webp", bytes -> bytes.length > 11 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                    && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50
    );

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private int serverPort;

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "avatars");
    }

    @PostMapping("/cover")
    public ResponseEntity<ApiResponse<String>> uploadCover(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "covers");
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "images");
    }

    private ResponseEntity<ApiResponse<String>> uploadFile(MultipartFile file, String subDir) {
        // 校验文件是否为空
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "请选择要上传的文件"));
        }

        // 扩展名白名单（客户端自报的 Content-Type 完全不可信，不作为依据）
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "只支持 jpg/png/gif/webp 格式的图片"));
        }

        // 校验文件大小 (最大 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文件大小不能超过 10MB"));
        }

        // 文件头魔数校验：防止伪装成图片扩展名的 HTML/SVG/脚本文件
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(16);
            if (!MAGIC_CHECKERS.get(extension).test(head)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "文件内容不是有效的图片"));
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "文件读取失败"));
        }

        try {
            // 使用项目根目录的绝对路径
            String projectRoot = System.getProperty("user.dir");
            Path uploadPath = Paths.get(projectRoot, uploadDir, subDir);

            // 创建上传目录
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名（扩展名已经过白名单校验）
            String fileName = UUID.randomUUID().toString() + "." + extension;

            // 保存文件
            Path targetPath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 返回访问 URL（相对路径，前端代理会处理）
            String fileUrl = "/uploads/" + subDir + "/" + fileName;

            log.info("文件上传成功: {}", fileUrl);

            return ResponseEntity.ok(ApiResponse.success("上传成功", fileUrl));

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "文件上传失败"));
        }
    }
}
