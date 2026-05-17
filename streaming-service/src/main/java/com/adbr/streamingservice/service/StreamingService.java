package com.adbr.streamingservice.service;

import com.adbr.streamingservice.dto.StreamingResponse;
import com.adbr.streamingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamingService {

    private final S3Presigner s3Presigner;
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

    }

}
