package com.project.dto.response;

import com.project.domain.analysis.Risk;
import com.project.domain.senior.SeniorStateHistory;

import java.time.LocalDateTime;

public record SeniorStateHistoryResponseDto(
        Long id,
        Risk previousState,
        Risk newState,
        String reason,
        LocalDateTime changedAt
) {
    public static SeniorStateHistoryResponseDto from(SeniorStateHistory history) {
        return new SeniorStateHistoryResponseDto(
                history.getId(),
                history.getPreviousState(),
                history.getNewState(),
                history.getReason(),
                history.getChangedAt()
        );
    }
}