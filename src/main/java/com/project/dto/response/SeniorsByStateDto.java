package com.project.dto.response;

import java.util.List;

public record SeniorsByStateDto(
        List<DashboardSeniorDto> positive,
        List<DashboardSeniorDto> danger,
        List<DashboardSeniorDto> critical,
        List<DashboardSeniorDto> emergency
) {
}