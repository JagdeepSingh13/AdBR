package com.adbr.contentservice.controllere;

import com.adbr.contentservice.dto.MovieRequest;
import com.adbr.contentservice.dto.MovieResponse;
import com.adbr.contentservice.model.Genre;
import com.adbr.contentservice.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

//    add movie to catalog
    @PostMapping
    public ResponseEntity<MovieResponse> addMovie(
            @Valid @RequestBody MovieRequest movieRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.addMovie(movieRequest));
    }

//    get all movies
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(contentService.getAllMovies());
    }

//    get movies by genre
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<MovieResponse>> getMoviesByGenre(
            @PathVariable Genre genre
    ) {
        return ResponseEntity.ok(contentService.getMoviesByGenre(genre));
    }

//     get movie by id
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(contentService.getMovieById(id));
    }

//    search movies
    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(
            @RequestParam String title
    ) {
        return ResponseEntity.ok(contentService.searchMovies(title));
    }

}
