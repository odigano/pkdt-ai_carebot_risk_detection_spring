package com.project.dto.response;

import com.project.domain.analysis.OverallResult;
import com.project.domain.analysis.Risk;
import com.project.domain.senior.Senior;
import com.project.domain.senior.Sex;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public record DashboardSeniorDto(
        Long seniorId,
        String name,
        int age,
        Sex sex,
        String address,
        Double latitude,
        Double longitude,
        LocalDateTime lastStateChangedAt,

        Long latestOverallResultId,
        String summary,
        String treatmentPlan,
        Risk preResolvedLabel,
        Risk resolvedLabel,
        boolean isResolved
) {
    public static DashboardSeniorDto from(Senior senior, LocalDateTime lastStateChangedAt, OverallResult latestResult) {
        Long latestOverallResultId = latestResult != null ? latestResult.getId() : null;
        String summary = latestResult != null ? latestResult.getReason().getSummary() : null;
        String treatmentPlan = latestResult != null ? latestResult.getTreatmentPlan() : null;
        Risk preResolvedLabel = latestResult != null ? latestResult.getLabel() : null;
        Risk resolvedLabel = latestResult != null ? latestResult.getResolvedLabel() : null;
        boolean isResolved = latestResult != null && latestResult.isResolved();

        return new DashboardSeniorDto(
                senior.getId(),
                senior.getName(),
                Period.between(senior.getBirthDate(), LocalDate.now()).getYears(),
                senior.getSex(),
                senior.getAddress().getAddress(),
                senior.getAddress().getLatitude(),
                senior.getAddress().getLongitude(),
                lastStateChangedAt,
                latestOverallResultId,
                summary,
                treatmentPlan,
                preResolvedLabel,
                resolvedLabel,
                isResolved
        );
    }
}