package com.adbr.encodingservice.service;

import com.adbr.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {

    private final EncodingService encodingService;

//    listens for video.uploaded topic, then EncodingService, then video.encoded topic

    @KafkaListener(
            topics = "video.uploaded",
            groupId = "encoding-service-group"
    )
    public void consumeVideoUploadedEvent(VideoUploadedEvent event) {
        log.info("consumed video uploaded event for movie: {}, file: {}", event.getMovieId(), event.getOriginalFileName());

        try {
            encodingService.encodeVideo(event);
        } catch (Exception e) {
            log.error("failed to process encoding for: {} - {}", event.getMovieId(), e.getMessage());
        }

    }

}
