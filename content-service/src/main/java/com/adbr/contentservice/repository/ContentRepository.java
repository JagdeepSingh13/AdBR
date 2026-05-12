package com.adbr.contentservice.repository;

import com.adbr.contentservice.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Movie, String> {



}
