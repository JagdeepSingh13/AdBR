package com.adbr.videoservice.service;

import com.adbr.videoservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC = "video.uploaded";

//    upload video to AWS S3 and publish VideoUploadedEvent to Kafka
    /*
    * FLOW:
    * 1. receive file
    * 2. generate unique s3 key
    * 3. upload to s3
    * 4. publish VideoUploadedEvent to kafka
    * 5. encoding service picks up and start Ffmpeg
    */
    public String uploadVideo(String movieId, MultipartFile file) throws IOException {
        log.info("starting video upload for: {} and file: {}", movieId, file.getOriginalFilename());

//        generate unique s3 key, format: raw/movieId/uuid_filename
        String videoKey = "raw/" + movieId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(videoKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("video is uploaded to s3, key: {}", videoKey);

//        publish event to kafka
//        encodeing-service will consume this
        VideoUploadedEvent event = new VideoUploadedEvent(
                movieId,
                videoKey,
                bucketName,
                file.getOriginalFilename(),
                file.getSize()
        );

        kafkaTemplate.send(VIDEO_UPLOADED_TOPIC, movieId, event);
        log.info("VideoUploadedEvent published for: {}", movieId);

        return videoKey;
    }

}
