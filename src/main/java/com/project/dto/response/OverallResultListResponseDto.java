package com.project.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import com.project.domain.analysis.Risk;
import com.project.domain.senior.Gu;
import com.project.domain.senior.Beopjeongdong;
import com.project.domain.senior.Sex;
import com.querydsl.core.annotations.QueryProjection;

public record OverallResultListResponseDto(
        Long overallResultId,
        Risk label,
        String summary,
        LocalDateTime timestamp,
        String dollId,
        Long seniorId,
        String name,
        int age,
        Sex sex,
        String gu,
        String dong,
        boolean isResolved
) {
    @QueryProjection
    public OverallResultListResponseDto(Long overallResultId, Risk label, String summary, LocalDateTime timestamp, 
    		String dollId, Long seniorId, String name, LocalDate birthDate, Sex sex, Gu gu, Beopjeongdong dong, boolean isResolved) {
        this(
            overallResultId,
            label,
            summary,
            timestamp,
            dollId,
            seniorId,
            name,
            Period.between(birthDate, LocalDate.now()).getYears(),
            sex,
            gu.getKoreanName(),
            dong.getKoreanName(),
            isResolved
        );
    }
}