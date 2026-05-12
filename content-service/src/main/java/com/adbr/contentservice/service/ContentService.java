package com.adbr.contentservice.service;

import com.adbr.contentservice.dto.MovieRequest;
import com.adbr.contentservice.dto.MovieResponse;
import com.adbr.contentservice.model.Genre;
import com.adbr.contentservice.model.Movie;
import com.adbr.contentservice.model.VideoStatus;
import com.adbr.contentservice.repository.MovieRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {

//    add a movie to catalog
//    but it is not uploaded yet at this stage

    private final MovieRepository movieRepository;

    public MovieResponse addMovie(@Valid MovieRequest movieRequest) {
        log.info("adding new movie: {}", movieRequest.getTitle());

        Movie movie = new Movie();
        movie.setTitle(movieRequest.getTitle());
        movie.setDescription(movie.getDescription());
        movie.setCast(movie.getCast());
        movie.setGenre(movieRequest.getGenre());
        movie.setDirector(movieRequest.getDirector());
        movie.setReleaseYear(movieRequest.getReleaseYear());
        movie.setRating(movieRequest.getRating());
        movie.setThumbnailUrl(movie.getThumbnailUrl());
        movie.setDurationminutes(movie.getDurationminutes());
        movie.setVideoStatus(VideoStatus.PENDING);

        Movie savedMovie = movieRepository.save(movie);
        log.info("saved movie: {}", savedMovie.getId());

        return mapToResponse(savedMovie);
    }

    private MovieResponse mapToResponse(Movie savedMovie) {
        MovieResponse response = new MovieResponse();
        response.setTitle(savedMovie.getTitle());
        response.setDescription(savedMovie.getDescription());
        response.setCast(savedMovie.getCast());
        response.setGenre(savedMovie.getGenre());
        response.setDirector(savedMovie.getDirector());
        response.setReleaseYear(savedMovie.getReleaseYear());
        response.setRating(savedMovie.getRating());
        response.setThumbnailUrl(savedMovie.getThumbnailUrl());
        response.setDurationminutes(savedMovie.getDurationminutes());
        response.setVideoStatus(savedMovie.getVideoStatus());
        response.setVideoKey(savedMovie.getVideoKey());
        response.setHlsUrl(savedMovie.getHlsUrl());
        response.setCreatedAt(savedMovie.getCreatedAt());

        return response;
    }

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MovieResponse> getMoviesByGenre(Genre genre) {
        return movieRepository.findByGenre(genre)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MovieResponse getMovieById(String id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(()->new RuntimeException("movie not found"+id));

        return mapToResponse(movie);
    }

    public List<MovieResponse> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

//    video-service uploads the video to s3 and gives the Key, we update the DB
    public void updateViewKey(String movieId, String videoKey) {
        log.info("updating video key for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new RuntimeException("movie not found"+movieId));

        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
    }

//    once the vieo is encoded we get HLS url in encoding-service
    public void updateHlsUrl(String movieId, String HlsUrl) {
        log.info("updating HLS url for: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(()-> new RuntimeException("movie not found"+movieId));

        movie.setHlsUrl(HlsUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);

        log.info("movie: {} is ready for streaming", movieId);
    }

}
