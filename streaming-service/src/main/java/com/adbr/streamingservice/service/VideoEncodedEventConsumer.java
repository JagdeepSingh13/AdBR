package com.adbr.streamingservice.service;

import com.adbr.streamingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String masterPlaylistKeyPrefix = "streaming:playlist:";

//    listens to video.encoded and stores master playlist key in redis
//    allows streaming service to quickly find playlist key by movieId
    @KafkaListener(
            topics = "video.encoded",
            groupId = "streaming-service-group"
    )
    public void consumeVideoEncodedEvent(VideoEncodedEvent event) {
        log.info("consumed video encoded event for: {}- {}", event.getMovieId(), event.isSuccess());

        if(event.isSuccess()) {
//            store master playlist key in redis
            String cacheKey = masterPlaylistKeyPrefix + event.getMovieId();
            redisTemplate.opsForValue().set(cacheKey, event.getMasterPlaylistKey());

            log.info("master playlist key stored for: {}", event.getMovieId());
        } else {
            log.error("encoding failed for: {} - {}", event.getMovieId(), event.getErrorMessage());
        }
    }


}
