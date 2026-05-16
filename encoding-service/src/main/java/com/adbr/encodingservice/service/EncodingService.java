package com.adbr.encodingservice.service;

import com.adbr.encodingservice.event.VideoEncodedEvent;
import com.adbr.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video.encoded";

//    video qualities to encode
//    format: {resolution, bitrate, height}
//    bitrate -> how much data processed per sec
    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
            new int[]{1920, 5000, 1080}, // 5000 kbps bitrate
            new int[]{1280, 2000, 720},
            new int[]{854, 1200, 480},
            new int[]{640, 800, 360}
    );

//    encoding pipeline
    /*
    * 1. download raw video from s3 (which was uploaded by video service)
    * 2. encode to multiple qualities using FFmpeg
    * 3. generate HLS playlist (.m3u8 for each quality)
    * 4. create master playlist
    * 5. upload all encoded files to s3
    * 6. publish video encoded event to kafka
    */
    public void encodeVideo(VideoUploadedEvent event) {
        log.info("starting encoding for movie: {}", event.getMovieId());

//        create unique path for a video (where-in we the download video)
        String jobPath = basePath + "/" + event.getMovieId();

        try {
//            create tmp directories
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded"));

//            download raw video from s3
            String localVideoPath = jobPath + "/raw_video.mp4";
            downloadFromS3(event.getVideoKey(), localVideoPath);

            log.info("raw video downloaded to: {}", localVideoPath);

//            encode to qualities and gen. HLS
            for (int[] qualities: VIDEO_QUALITIES) {
                int wd = qualities[0];
                int bitrate =qualities[1];
                int ht = qualities[2];

                String qualityDir = jobPath + "/encoded" + ht + "p";
                Files.createDirectories(Paths.get(qualityDir));

                encodeToHLS(localVideoPath, qualityDir, wd, ht, bitrate);
                log.info("encoded: {}p successfully", ht);
            }

//            generate master playlist
            String masterPlaylistPath = jobPath + "/encoded/master.m3u8";
            generateMasterPlaylist(masterPlaylistPath);
            log.info("master playlist generated");

//            upload all resources files to S3
            String encodedPrefix = "encoded/" + event.getMovieId() + "/";
            uploadEncodedFilesToS3(jobPath + "/encoded", encodedPrefix);
            log.info("all encoded files uploaded to S3");

//            publish video.encoded event
            String masterPlaylistKey = encodedPrefix + "master.m3u8";
            String hlsUrl = "https://" + bucketName + ".s3.amazonaws.com/" + masterPlaylistKey;

            VideoEncodedEvent encodedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    hlsUrl,
                    masterPlaylistKey,
                    true,
                    null
            );

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), encodedEvent);
            log.info("videoEncodedEvent published for: {}", event.getMovieId());

        } catch(Exception e) {
            log.error("encoding failed for: {} - {}", event.getMovieId(), e.getMessage());

//            publish failure event
            VideoEncodedEvent failureEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    null,
                    null,
                    false,
                    e.getMessage()
            );
            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), failureEvent);
        } finally {
//            cleanup tmp files
            cleanupTempFiles(jobPath);
        }

    }

}








