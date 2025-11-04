package com.project.domain.analysis;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.project.domain.senior.Doll;
import com.project.domain.senior.Senior;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OverallResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senior_id")
	private Senior senior;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doll_id")
	private Doll doll;
	
	@CreationTimestamp
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    private Risk label;
    
    @Embedded
    private ConfidenceScores confidenceScores;
    
    @Embedded
    private Reason reason;
    
    private String treatmentPlan;
    
    private boolean isResolved;
    
    @Enumerated(EnumType.STRING)
    private Risk resolvedLabel;
        
    @OneToMany(mappedBy = "overallResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dialogue> dialogues = new ArrayList<>();
    
    @Builder
    public OverallResult(Doll doll, Senior senior, Risk label, ConfidenceScores confidenceScores, Reason reason, String treatmentPlan) {
        this.doll = doll;
        this.senior = senior;
        this.label = label;
        this.confidenceScores = confidenceScores;
        this.reason = reason;
        this.treatmentPlan = treatmentPlan;
    }
    
    public void resolveWithLabel(Risk resolvedLabel) {
        this.isResolved = true;
        this.resolvedLabel = resolvedLabel;
    }
    
    public void addDialogue(Dialogue dialogue) {
        this.dialogues.add(dialogue);
        dialogue.setOverallResult(this);
    }
}