package com.adbr.contentservice.service;

import com.adbr.contentservice.dto.MovieRequest;
import com.adbr.contentservice.dto.MovieResponse;
import com.adbr.contentservice.model.Genre;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ContentService {

    public MovieResponse addMovie(@Valid MovieRequest movieRequest) {

    }

    public List<MovieResponse> getAllMovies() {

    }

    public List<MovieResponse> getMoviesByGenre(Genre genre) {

    }

    public MovieResponse getMovieById(String id) {

    }

    public List<MovieResponse> searchMovies(String title) {

    }
}
