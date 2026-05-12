package com.adbr.contentservice.repository;

import com.adbr.contentservice.dto.MovieResponse;
import com.adbr.contentservice.model.Genre;
import com.adbr.contentservice.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, String> {

    List<Movie> findByGenre(Genre genre);

    List<Movie> findByTitleContainingIgnoreCase(String title);

}
