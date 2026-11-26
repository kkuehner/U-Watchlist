package com.example.Investment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Investment.entity.WatchlistEntry;

public interface WatchlistRepository extends JpaRepository<WatchlistEntry, Long> {
    Optional<WatchlistEntry> findBySymbol(String symbol);
    boolean existsBySymbol(String symbol);
}

