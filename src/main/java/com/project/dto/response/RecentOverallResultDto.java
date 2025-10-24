package com.project.dto.response;

import com.project.domain.analysis.OverallResult;
import com.project.domain.analysis.Risk;
import java.time.LocalDateTime;

public record RecentOverallResultDto(
        Long id,
        Risk label,
        String summary,
        LocalDateTime timestamp,
        boolean isResolved
) {
    public static RecentOverallResultDto from(OverallResult overallResult) {
        return new RecentOverallResultDto(
                overallResult.getId(),
                overallResult.getLabel(),
                overallResult.getReason().getSummary(),
                overallResult.getTimestamp(),
                overallResult.isResolved()
        );
    }
}