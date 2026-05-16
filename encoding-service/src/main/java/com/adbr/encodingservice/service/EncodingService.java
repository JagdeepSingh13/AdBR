package com.adbr.encodingservice.service;

import com.adbr.encodingservice.event.VideoEncodedEvent;
import com.adbr.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

                String qualityDir = jobPath + "/encoded/" + ht + "p";
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

    private void cleanupTempFiles(String jobPath) {
        try {
            Path dirPath = Paths.get(jobPath);
            if(Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                log.info("tmp files cleaned up for: {}", jobPath);
            }
        } catch (IOException e) {
            log.warn("failed to clean up: {} - {}", jobPath, e.getMessage());
        }

    }

    private void uploadEncodedFilesToS3(String localDir, String s3Prefix) throws IOException {
        File dir = new File(localDir);
        uploadDirToS3(dir, localDir, s3Prefix);
    }

    private void uploadDirToS3(File dir, String localDir, String s3Prefix) throws IOException {
        for(File file: dir.listFiles()) {
            if(file.isDirectory()) {
                uploadDirToS3(file, localDir, s3Prefix);
            } else {
                String relativePath = file.getAbsolutePath()
                        .substring(localDir.length()+1)
                        .replace("\\", "/");

                String s3Key = s3Prefix + relativePath;

                String contentType = file.getName().endsWith(".m3u8")
                        ? "application/x-mpegURL"
                        : "video/MP2T";

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
                log.debug("uploaded: {}", s3Key);
            }
        }

    }

    //    generates the master playlist that references all qualities playlists
    private void generateMasterPlaylist(String masterPlaylistPath) throws IOException {
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n");        // extended m3u playlist
        master.append("#EXT-X-VERSION:3\n\n");

//        add each quality to master playlist
        int[][] qualities = {{1920, 5000, 1080},{1280, 2000, 720}, {854, 1200, 480}, {640, 800, 360}};
        for (int[] q: qualities) {
            int wd = q[0], bitrate = q[1], ht = q[2];

            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bitrate*1000)
                    .append(",RESOLUTION=").append(wd).append("x").append(ht)
                    .append(",CODECS=\"avc1.42e01e,mp4a.40.2\"\n");

            master.append(ht).append("p/playlist.m3u8\n\n");
        }

        Files.writeString(Paths.get(masterPlaylistPath), master.toString());
    }

    private void encodeToHLS(String localVideoPath, String outputDir, int wd, int ht, int bitrate) throws IOException, InterruptedException {
        String playlistPath = outputDir + "/playlist.m3u8";
        String segmentPattern = outputDir + "/segment_%03d.ts";

//        FFmpeg command for HLS encoding
        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i", localVideoPath,
                "-vf", "scale=" + wd + ":" + ht,
                "-c:v", "libx264",
                "-b:v", bitrate + "k",
                "-c:a", "aac",
                "-b:a", "128k",            // audio bitrate
                "-hls_time", "10",         // 10s segments
                "-hls_list_size", "0",     // keep all segments
                "-hls_segment_filename", segmentPattern,
                "-f", "hls",
                playlistPath
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        int exitCode = process.waitFor();
        if(exitCode != 0) {
            throw new RuntimeException("FFmpeg encoding failed with exit code: " + exitCode);
        }
    }

    private void downloadFromS3(String s3Key, String localVideoPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.getObject(getObjectRequest, Paths.get(localVideoPath));
    }

}



