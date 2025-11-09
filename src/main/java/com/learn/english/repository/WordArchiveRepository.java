package com.learn.english.repository;

import com.learn.english.entity.WordsArchiveEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WordArchiveRepository extends JpaRepository<WordsArchiveEO, UUID> {
}
