package com.project.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.domain.analysis.OverallResult;

public interface OverallResultRepository extends JpaRepository<OverallResult, Long>, OverallResultRepositoryCustom  {
	@Query("SELECT DISTINCT o FROM OverallResult o " +
            "JOIN FETCH o.doll d " +
            "LEFT JOIN FETCH o.dialogues")
	List<OverallResult> findAllWithDetails();
	 
    @Query("SELECT o FROM OverallResult o " +
            "JOIN FETCH o.senior s " +
            "JOIN FETCH o.doll d " +
            "LEFT JOIN FETCH o.dialogues " +
            "WHERE o.id = :id")
    Optional<OverallResult> findByIdWithDetails(@Param("id") Long id);
    
    List<OverallResult> findTop5BySeniorIdOrderByTimestampDesc(Long seniorId);
    
    boolean existsBySeniorIdAndTimestampAfter(Long seniorId, LocalDateTime timestamp);
    
    @Query("SELECT o FROM OverallResult o WHERE o.id IN " +
           "(SELECT o2.id FROM OverallResult o2 WHERE o2.timestamp = " +
           "(SELECT MAX(o3.timestamp) FROM OverallResult o3 WHERE o3.senior = o2.senior))")
    List<OverallResult> findLatestOverallResultForEachSenior();
}
