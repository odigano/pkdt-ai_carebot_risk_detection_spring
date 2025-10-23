package com.project.event;

import com.project.domain.analysis.Risk;
import com.project.domain.senior.Senior;

public record SeniorStateChangedEvent(
        Senior senior,
        Risk previousState,
        Risk newState,
        String reason
) {
}