package com.adbr.encodingservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// consumed from kafka topic: video.uploaded

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
