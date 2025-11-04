package com.project.dto.response;

public record DashboardResponseDto(
        StateCountDto stateCount,
        SeniorsByStateDto seniorsByState
) {
}