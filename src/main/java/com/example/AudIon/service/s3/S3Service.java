package com.example.AudIon.service.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 s3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cdn-base:}")
    private String cdnBase;

    @Value("${app.upload.max-file-size:50MB}")
    private String maxFileSize;

    private static final Pattern SAFE_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/wave", "audio/x-wav",
            "audio/ogg", "audio/aac", "audio/mp4", "audio/x-m4a"
    );
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB default

    /** 기존과 동일: 퍼블릭 업로드, 공개 URL 반환(MVP 간단모드) */
    public String uploadVoiceFile(MultipartFile file, String walletAddress) {
        try {
            S3UploadResult result = uploadVoiceFile(file, walletAddress, true, 0);
            return result.publicUrl() != null ? result.publicUrl() : result.s3Url();
        } catch (Exception e) {
            log.error("Failed to upload voice file for wallet: {}", walletAddress, e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    /** 개선판: 퍼블릭 여부/프리사인 만료초 옵션으로 유연하게 */
    public S3UploadResult uploadVoiceFile(MultipartFile file,
                                          String walletAddress,
                                          boolean makePublic,
                                          int presignExpireSeconds) {

        // Input validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size: " + maxFileSize);
        }

        // Validate content type
        String contentType = file.getContentType();
        if (!isValidAudioFile(contentType, file.getOriginalFilename())) {
            throw new IllegalArgumentException("Invalid audio file type. Allowed types: " + ALLOWED_AUDIO_TYPES);
        }

        // Sanitize inputs
        String safeWallet = sanitizeWalletAddress(walletAddress);
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // Generate unique key with date-based hierarchy
        String key = generateS3Key(safeWallet, fileExtension);

        // Prepare metadata
        ObjectMetadata metadata = createObjectMetadata(contentType, file.getSize());

        try {
            // Upload to S3
            uploadToS3(file, key, metadata, makePublic);

            // Generate URLs
            String s3Url = s3.getUrl(bucket, key).toString();
            String publicUrl = makePublic ? buildPublicUrl(key) : null;
            URL presignedUrl = generatePresignedUrl(key, makePublic, presignExpireSeconds);

            log.info("Successfully uploaded file: {} to key: {}", file.getOriginalFilename(), key);
            return new S3UploadResult(key, s3Url, publicUrl, presignedUrl);

        } catch (AmazonServiceException e) {
            log.error("AWS S3 service error during upload: {}", e.getMessage(), e);
            throw new RuntimeException("S3 service error: " + e.getErrorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during S3 upload", e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    /**
     * Delete file from S3
     */
    public boolean deleteFile(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        try {
            s3.deleteObject(bucket, s3Key);
            log.info("Successfully deleted file: {}", s3Key);
            return true;
        } catch (AmazonServiceException e) {
            log.error("Failed to delete file: {} - {}", s3Key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            return false;
        }

        try {
            return s3.doesObjectExist(bucket, s3Key);
        } catch (AmazonServiceException e) {
            log.error("Error checking file existence: {} - {}", s3Key, e.getMessage(), e);
            return false;
        }
    }

    // Private helper methods

    private boolean isValidAudioFile(String contentType, String filename) {
        // Check MIME type
        if (StringUtils.hasText(contentType) && ALLOWED_AUDIO_TYPES.contains(contentType.toLowerCase())) {
            return true;
        }

        // Fallback: check file extension
        String ext = getFileExtension(filename);
        return Arrays.asList("mp3", "wav", "ogg", "aac", "m4a").contains(ext.toLowerCase());
    }

    private String sanitizeWalletAddress(String walletAddress) {
        if (!StringUtils.hasText(walletAddress)) {
            return "unknown";
        }
        return SAFE_PATTERN.matcher(walletAddress.trim()).replaceAll("_");
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "wav"; // Default extension
        }

        String ext = FilenameUtils.getExtension(filename);
        return StringUtils.hasText(ext) ? ext.toLowerCase() : "wav";
    }

    private String generateS3Key(String safeWallet, String extension) {
        LocalDate date = LocalDate.now();
        return String.format("voices/%s/%04d/%02d/%02d/%s.%s",
                safeWallet,
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                UUID.randomUUID(),
                extension);
    }

    private ObjectMetadata createObjectMetadata(String contentType, long fileSize) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        metadata.setContentLength(fileSize);

        // Add cache control for audio files
        metadata.setCacheControl("max-age=31536000"); // 1 year

        // Add metadata for tracking
        metadata.addUserMetadata("uploaded-at", String.valueOf(System.currentTimeMillis()));

        return metadata;
    }

    private void uploadToS3(MultipartFile file, String key, ObjectMetadata metadata, boolean makePublic)
            throws IOException {

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(bucket, key, inputStream, metadata);

            if (makePublic) {
                request.withCannedAcl(CannedAccessControlList.PublicRead);
            }

            s3.putObject(request);
        }
    }

    private String buildPublicUrl(String key) {
        if (StringUtils.hasText(cdnBase)) {
            String cleanCdnBase = cdnBase.endsWith("/") ? cdnBase.substring(0, cdnBase.length() - 1) : cdnBase;
            return cleanCdnBase + "/" + key;
        }
        return s3.getUrl(bucket, key).toString();
    }

    private URL generatePresignedUrl(String key, boolean makePublic, int presignExpireSeconds) {
        if (makePublic || presignExpireSeconds <= 0) {
            return null;
        }

        try {
            Date expiration = new Date(System.currentTimeMillis() + (presignExpireSeconds * 1000L));
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);

            return s3.generatePresignedUrl(request);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", key, e);
            return null;
        }
    }

    /** 업로드 결과 DTO */
    public record S3UploadResult(String key, String s3Url, String publicUrl, URL presignedUrl) {}
}