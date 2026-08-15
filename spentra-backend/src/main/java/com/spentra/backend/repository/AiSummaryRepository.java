package com.spentra.backend.repository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spentra.backend.model.entity.AiSummary;

@Repository
public interface AiSummaryRepository extends JpaRepository<AiSummary, UUID> {
    Optional<AiSummary> findByUserIdAndYearMonth(UUID userId, YearMonth yearMonth);

    void deleteByUserIdAndYearMonth(UUID userId, YearMonth yearMonth);
}
