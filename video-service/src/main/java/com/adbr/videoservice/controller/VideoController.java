package com.adbr.videoservice.controller;

import com.adbr.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/videos")
@Slf4j
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

//    upload video file for a movie
//    accepts multipart file upload
    @PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        log.info("video upload request for: {}, file size: {}MB", movieId, file.getSize()/(1024*1024));
        if(file.isEmpty()) {
            return ResponseEntity.badRequest().body("file is empty");
        }

        String videoKey = videoService.uploadVideo(movieId, file);

        return ResponseEntity.ok(
                "video uploaded successfully, key: "+ videoKey+ "encoding started"
        );
    }

}
