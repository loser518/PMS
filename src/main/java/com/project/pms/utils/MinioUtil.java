package com.project.pms.utils;


import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @className: MinioUtil
 * @description: MinIO工具类
 * @author: loser
 * @createTime: 2026/2/3 21:21
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.upload.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${minio.upload.allowed-extensions}")
    private List<String> allowedExtensions;

    // ====================== 桶操作 ======================

    /**
     * 检查桶是否存在
     */
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("检查桶是否存在失败: {}", bucketName, e);
            return false;
        }
    }

    /**
     * 创建桶
     */
    public boolean makeBucket(String bucketName) {
        try {
            boolean exists = bucketExists(bucketName);
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("桶创建成功: {}", bucketName);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("创建桶失败: {}", bucketName, e);
            return false;
        }
    }

    /**
     * 获取所有桶
     */
    public List<Bucket> listBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("获取桶列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除桶
     */
    public boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("桶删除成功: {}", bucketName);
            return true;
        } catch (Exception e) {
            log.error("删除桶失败: {}", bucketName, e);
            return false;
        }
    }

    // ====================== 文件上传 ======================

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, bucketName);
    }

    /**
     * 上传文件到指定桶
     */
    public String uploadFile(MultipartFile file, String bucketName) {
        try {
            // 验证文件
            validateFile(file);

            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename());

            // 创建桶（如果不存在）
            makeBucket(bucketName);

            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), fileName);
            return fileName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（自定义路径）
     */
    public String uploadFile(String bucketName, String objectName, InputStream inputStream,
                             String contentType, long size) {
        try {
            // 创建桶（如果不存在）
            makeBucket(bucketName);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            log.info("文件上传成功: {} -> {}", objectName, bucketName);
            return objectName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传Base64文件
     */
    public String uploadBase64File(String base64Data, String fileName, String contentType) {
        try {
            // 解码Base64
            byte[] data = java.util.Base64.getDecoder().decode(base64Data);

            // 生成文件名
            String finalFileName = generateFileName(fileName);

            // 上传文件
            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(finalFileName)
                        .stream(inputStream, data.length, -1)
                        .contentType(contentType)
                        .build());
            }

            log.info("Base64文件上传成功: {}", finalFileName);
            return finalFileName;
        } catch (Exception e) {
            log.error("Base64文件上传失败", e);
            throw new RuntimeException("Base64文件上传失败: " + e.getMessage());
        }
    }

    // ====================== 文件下载 ======================

    /**
     * 获取文件流
     */
    public InputStream getFile(String fileName) {
        return getFile(bucketName, fileName);
    }

    /**
     * 获取文件流
     */
    public InputStream getFile(String bucketName, String fileName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            log.error("获取文件失败: {}", fileName, e);
            throw new RuntimeException("获取文件失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件到本地
     */
    public void downloadFile(String fileName, String localFilePath) {
        try {
            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .filename(localFilePath)
                    .build());
            log.info("文件下载成功: {} -> {}", fileName, localFilePath);
        } catch (Exception e) {
            log.error("文件下载失败: {}", fileName, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    // ====================== 文件URL ======================

    /**
     * 获取文件访问URL（带过期时间）
     */
    public String getPresignedObjectUrl(String fileName, int expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(fileName)
                    .expiry(expiry, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", fileName, e);
            throw new RuntimeException("获取文件URL失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问URL（默认7天）
     */
    public String getFileUrl(String fileName) {
        return getPresignedObjectUrl(fileName, 7 * 24 * 60); // 7天
    }

    /**
     * 获取文件访问URL（无过期时间）
     */
    public String getPermanentUrl(String bucketName, String objectName) {
        try {
            return getObjectUrl(bucketName, objectName);
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", objectName, e);
            return null;
        }
    }

    /**
     * 获取文件访问URL（无过期时间或长过期时间）
     */
    public String getObjectUrl(String bucketName, String objectName) {
        try {
            // 设置一个很长的过期时间，比如10年
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(365 * 10, TimeUnit.DAYS)  // 10年，近似永久
                            .build()
            );
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", objectName, e);
            return null;
        }
    }
    // ====================== 文件URL增强 ======================

    /**
     * 方案一：获取临时访问URL（私密文件使用）
     * @param fileName 文件名
     * @param expiry 过期时间
     * @param unit 时间单位
     */
    public String getTemporaryUrl(String fileName, int expiry, TimeUnit unit) {
        return getTemporaryUrl(bucketName, fileName, expiry, unit);
    }

    /**
     * 方案一：获取临时访问URL（私密文件使用）
     */
    public String getTemporaryUrl(String bucketName, String objectName, int expiry, TimeUnit unit) {
        try {
            // 参数校验：确保不超过7天
            long expirySeconds = unit.toSeconds(expiry);
            long maxSeconds = TimeUnit.DAYS.toSeconds(7);

            if (expirySeconds > maxSeconds) {
                log.warn("过期时间超过7天上限，自动调整为7天: {} -> {}", objectName, expirySeconds);
                expirySeconds = (int) maxSeconds;
            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry((int) expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取临时URL失败: {}", objectName, e);
            return null;
        }
    }

    /**
     * 方案二：获取公开URL（需提前设置桶策略为公开）
     * 适用场景：公告图片、系统图标、不敏感文件
     */
    public String getPublicUrl(String fileName) {
        return getPublicUrl(bucketName, fileName);
    }

    /**
     * 方案二：获取公开URL
     */
    public String getPublicUrl(String bucketName, String objectName) {
        return String.format("%s/%s/%s", endpoint, bucketName, objectName);
    }

    /**
     * 智能获取URL - 根据文件类型自动选择
     */
    public String getOptimalUrl(String fileName, boolean isPublic) {
        if (isPublic) {
            return getPublicUrl(fileName);
        } else {
            return getTemporaryUrl(fileName, 7, TimeUnit.DAYS);
        }
    }

    // ====================== 文件管理 ======================

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String fileName) {
        return fileExists(bucketName, fileName);
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String bucketName, String fileName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件信息
     */
    public StatObjectResponse getFileInfo(String fileName) {
        return getFileInfo(bucketName, fileName);
    }

    /**
     * 获取文件信息
     */
    public StatObjectResponse getFileInfo(String bucketName, String fileName) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            log.error("获取文件信息失败: {}", fileName, e);
            return null;
        }
    }

    public String getFilename(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }

        // 移除查询参数
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }

        // 移除URL锚点（如果有）
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex > 0) {
            url = url.substring(0, fragmentIndex);
        }

        // 获取最后一个斜杠后的内容
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }

        return url;
    }

    /**
     * 删除文件
     */
    public boolean removeFile(String fileName) {
        return removeFile(bucketName, fileName);
    }

    /**
     * 删除文件
     */
    public boolean removeFile(String bucketName, String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
            log.info("文件删除成功: {}", fileName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", fileName, e);
            return false;
        }
    }

    /**
     * 批量删除文件
     */
    public List<DeleteError> removeFiles(List<String> fileNames) {
        return removeFiles(bucketName, fileNames);
    }

    /**
     * 批量删除文件
     */
    public List<DeleteError> removeFiles(String bucketName, List<String> fileNames) {
        try {
            List<DeleteObject> objects = new ArrayList<>();
            for (String fileName : fileNames) {
                objects.add(new DeleteObject(fileName));
            }

            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objects)
                            .build());

            List<DeleteError> errors = new ArrayList<>();
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                if (error != null) {
                    errors.add(error);
                }
            }

            log.info("批量删除文件完成，成功: {}, 失败: {}",
                    fileNames.size() - errors.size(), errors.size());
            return errors;
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            throw new RuntimeException("批量删除文件失败: " + e.getMessage());
        }
    }

    // ====================== 文件列表 ======================

    /**
     * 列出桶中的所有文件
     */
    public List<Item> listFiles() {
        return listFiles(bucketName);
    }

    /**
     * 列出桶中的所有文件
     */
    public List<Item> listFiles(String bucketName) {
        List<Item> items = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                items.add(result.get());
            }
        } catch (Exception e) {
            log.error("列出文件失败", e);
        }
        return items;
    }

    /**
     * 列出指定前缀的文件
     */
    public List<Item> listFilesByPrefix(String prefix) {
        return listFilesByPrefix(bucketName, prefix);
    }

    /**
     * 列出指定前缀的文件
     */
    public List<Item> listFilesByPrefix(String bucketName, String prefix) {
        List<Item> items = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                items.add(result.get());
            }
        } catch (Exception e) {
            log.error("列出文件失败", e);
        }
        return items;
    }

    // ====================== 辅助方法 ======================

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new RuntimeException("不支持的文件类型: " + extension +
                    ", 支持的类型: " + String.join(", ", allowedExtensions));
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        return uuid + "." + extension;
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

    /**
     * 获取文件大小（格式化）
     */
    public String getFormattedFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

}


