package com.adbr.videoservice.event;

// event published to kafka when a video is uploaded to kafka
// encoding service consumes this

// TOPIC: video.uploaded

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {

    private String movieId;
    private String videoKey;
    private String buckeyName;
    private String originalFileName;
    private long fileSizeBytes;

}
