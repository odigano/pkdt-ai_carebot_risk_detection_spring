package com.project.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.domain.senior.SeniorStateHistory;

public interface SeniorStateHistoryRepository extends JpaRepository<SeniorStateHistory, Long> {
	List<SeniorStateHistory> findBySeniorIdOrderByChangedAtDesc(Long seniorId);
}
