package com.project.domain.senior;

import com.project.domain.analysis.Risk;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeniorStateHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senior_id", nullable = false)
    private Senior senior;

    @Enumerated(EnumType.STRING)
    private Risk previousState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Risk newState;

    @Column(nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Builder
    public SeniorStateHistory(Senior senior, Risk previousState, Risk newState, String reason) {
        this.senior = senior;
        this.previousState = previousState;
        this.newState = newState;
        this.reason = reason;
    }
}