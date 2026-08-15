package com.spentra.backend.repository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spentra.backend.model.entity.AiSummary;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AiSummaryRepository extends JpaRepository<AiSummary, UUID> {
    Optional<AiSummary> findFirstByUserIdAndYearMonthOrderByGeneratedAtDesc(UUID userId, YearMonth yearMonth);

    Optional<AiSummary> findByUserIdAndYearMonth(UUID userId, YearMonth yearMonth);

    @Modifying
    @Transactional
    void deleteByUserIdAndYearMonth(UUID userId, YearMonth yearMonth);
}
