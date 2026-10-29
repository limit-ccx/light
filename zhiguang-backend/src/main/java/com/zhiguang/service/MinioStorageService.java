package com.zhiguang.service;

import com.zhiguang.config.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 文件存储服务
 * <p>
 * 提供文件上传、下载、删除功能：
 * - 图片/视频上传
 * - 文件访问 URL 生成
 * - 文件删除
 * - Bucket 管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 初始化 Bucket（创建并设置访问策略）
     */
    public void initializeBucket() {
        try {
            String bucketName = minioProperties.getBucket();

            // 检查 Bucket 是否存在
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!exists) {
                // 创建 Bucket（保持私有：公开读策略会让全部对象可被匿名枚举/下载，
                // 读取一律走 getPresignedUrl 短时效签名，SEC-P2-3）
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());

                log.info("MinIO Bucket 初始化成功: bucket={}, policy=private", bucketName);
            } else {
                log.info("MinIO Bucket 已存在: bucket={}", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO Bucket 初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file, String category) {
        try {
            String bucketName = minioProperties.getBucket();
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String objectName = generateObjectName(category, extension);

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String fileUrl = getFileUrl(objectName);
            log.info("文件上传成功: objectName={}, size={}, url={}", objectName, file.getSize(), fileUrl);

            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传失败: filename={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（InputStream）
     */
    public String uploadFile(InputStream inputStream, String filename, String contentType, String category, long size) {
        try {
            String bucketName = minioProperties.getBucket();
            String extension = getFileExtension(filename);
            String objectName = generateObjectName(category, extension);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            String fileUrl = getFileUrl(objectName);
            log.info("文件上传成功（InputStream）: objectName={}, url={}", objectName, fileUrl);

            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传失败（InputStream）: filename={}, error={}", filename, e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String objectName) {
        try {
            String bucketName = minioProperties.getBucket();

            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

            log.debug("文件下载成功: objectName={}", objectName);
            return stream;
        } catch (Exception e) {
            log.error("文件下载失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        try {
            String bucketName = minioProperties.getBucket();

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

            log.info("文件删除成功: objectName={}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问 URL
     */
    public String getFileUrl(String objectName) {
        return String.format("%s/%s/%s", minioProperties.getEndpoint(), minioProperties.getBucket(), objectName);
    }

    /**
     * 获取临时访问 URL（带签名，有效期 7 天）
     */
    public String getPresignedUrl(String objectName, int expiryDays) {
        try {
            String bucketName = minioProperties.getBucket();

            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expiryDays * 24 * 60 * 60)  // 秒
                    .build());

            log.debug("临时 URL 生成成功: objectName={}, expiryDays={}", objectName, expiryDays);
            return url;
        } catch (Exception e) {
            log.error("临时 URL 生成失败: objectName={}, error={}", objectName, e.getMessage(), e);
            throw new RuntimeException("临时 URL 生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成对象名称（UUID + 时间戳）
     */
    private String generateObjectName(String category, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/%s_%s.%s", category, timestamp, uuid, extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}