package com.project.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.domain.senior.SeniorStateHistory;

public interface SeniorStateHistoryRepository extends JpaRepository<SeniorStateHistory, Long> {
	List<SeniorStateHistory> findBySeniorIdOrderByChangedAtDesc(Long seniorId);

	@Query("SELECT h.senior.id, MAX(h.changedAt) FROM SeniorStateHistory h GROUP BY h.senior.id")
    List<Object[]> findLatestStateChangeTimestampForEachSenior();	
}
