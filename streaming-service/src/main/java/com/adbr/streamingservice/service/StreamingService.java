package com.adbr.streamingservice.service;

import com.adbr.streamingservice.dto.StreamingResponse;
import com.adbr.streamingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamingService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private long presignedUrlExpiry;

//    redis key for caching streaming Urls
    private static final String STREAMING_URL_PREFIX = "streaming:url:";

//    1. check redis cache for existing presigned URL
//    2. if not cached, generate new URL from S3
//    3. cache the URL in Redis
//    presigned URL gives temp. access
    public StreamingResponse getStreamingUrl(String movieId, String playlistKey) {
        String cacheKey = STREAMING_URL_PREFIX + movieId;
        String cacheURL = redisTemplate.opsForValue().get(cacheKey);

        if(cacheURL != null) {
            return new StreamingResponse(
                    movieId,
                    cacheURL,
                    "1080p, 720p, 480p, 360p",
                    presignedUrlExpiry
            );
        }

//        gen. URL from S3
        String presignedURL = genPresignedUrl(playlistKey);

//        cache in redis for 55 min
        redisTemplate.opsForValue().set(
                cacheKey,
                presignedURL,
                55,
                TimeUnit.MINUTES
        );

        log.info("streaming URL generated and cached for: {}", movieId);
        return new StreamingResponse(
                movieId,
                presignedURL,
                "1080p, 720p, 480p, 360p",
                presignedUrlExpiry
        );
    }

    private String genPresignedUrl(String playlistKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(playlistKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiry))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

//    invalidate cache streaming URL
//    when video is re-encoded or updated
    public void invalidateCache(String movieId) {
        String cacheKey = STREAMING_URL_PREFIX + movieId;
        redisTemplate.delete(cacheKey);
        log.info("streaming url cache invalidated for movie: {}", movieId);
    }

    public String getSignedPlaylist(String movieId, String playlistPath) {
//        get base path for this playlist
        String basePath = playlistPath.substring(0,
                playlistPath.lastIndexOf('/')+1);

//        read m3u8 content from S3
        String m3u8Content = readFromS3(playlistPath);

//        rewrite each line i.e segment or playlist reference
        String signedContent = rewriteM3u8SignedUrls(
                m3u8Content, basePath
        );

        return signedContent;
    }

    private String rewriteM3u8SignedUrls(String m3u8Content, String basePath) {
        StringBuilder rewritten = new StringBuilder();

        for(String line: m3u8Content.split("\n")) {
            String trimmed = line.trim();

//            skip empty lines and comments
            if(trimmed.isEmpty() || trimmed.startsWith("#")) {
                rewritten.append(line).append("\n");
                continue;
            }

            String fullKey = basePath + trimmed;
            String signedUrl = genPresignedUrl(fullKey);

            rewritten.append(signedUrl).append("\n");
        }

        return rewritten.toString();
    }

    private String readFromS3(String playlistPath) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(playlistPath)
                .build();

        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);

        return new BufferedReader(new InputStreamReader(response))
                .lines()
                .collect(Collectors.joining("\n"));
    }

}
