package com.adbr.streamingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResponse {

    private String movieId;
//    presigned HLS master playlist URL
    private String streamingURL;
    private String quality;
//    URL expiry time
    private long expiresInMinutes;

}
