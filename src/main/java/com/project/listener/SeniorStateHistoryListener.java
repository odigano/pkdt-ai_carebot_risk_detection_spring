package com.project.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.senior.SeniorStateHistory;
import com.project.event.SeniorStateChangedEvent;
import com.project.persistence.SeniorStateHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeniorStateHistoryListener {
    private final SeniorStateHistoryRepository historyRepository;

    @EventListener
    @Transactional
    public void handleSeniorStateChangedEvent(SeniorStateChangedEvent event) {
    	log.info("SeniorStateChangedEvent 수신: seniorId={}, newState={}, reason={}",
                event.senior().getId(), event.newState(), event.reason());
        try {            
            SeniorStateHistory history = SeniorStateHistory.builder()
                    .senior(event.senior())
                    .previousState(event.previousState())
                    .newState(event.newState())
                    .reason(event.reason())
                    .build();

            historyRepository.save(history);
            log.info("시니어 #{} 상태 변경 로그 기록 완료: {} -> {}",
                    event.senior().getId(), event.previousState(), event.newState());
        } catch (Exception e) {
            log.error("시니어 상태 변경 로그 기록 중 예외 발생", e);
        }
    }
}