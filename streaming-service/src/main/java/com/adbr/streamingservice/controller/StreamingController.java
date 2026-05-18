package com.adbr.streamingservice.controller;

import com.adbr.streamingservice.dto.StreamingResponse;
import com.adbr.streamingservice.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stream")
@Slf4j
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

    //    get streaming URL for a video
//    presigned master HLS playlist URL
    @GetMapping("/{movieId}")
    public ResponseEntity<StreamingResponse> getStreamingUrl(
            @PathVariable String movieId
    ) {
        log.info("streaming req. for: {}", movieId);

//        get master playlist key from redis
        String playlistKey = redisTemplate.opsForValue()
                .get(MASTER_PLAYLIST_KEY_PREFIX + movieId);

        if (playlistKey == null) {
            return ResponseEntity.notFound().build();
        }

        StreamingResponse response = streamingService.getStreamingUrl(movieId, playlistKey);
        return ResponseEntity.ok(response);
    }

//    signed m3u8 playlist content, called by HLS player for each quality
    @GetMapping("/{movieId}/playlist")
    public ResponseEntity<String> getSignedPlaylist(
            @PathVariable String movieId,
            @RequestParam String path
    ) {
        String signedPlaylist = streamingService.getSignedPlaylist(movieId, path);

        return ResponseEntity.ok()
                .header("Content-Type", "application/x-mpegURL")
                .body(signedPlaylist);
    }

}
