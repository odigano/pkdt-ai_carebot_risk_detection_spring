package com.project.listener;

import com.project.event.SeniorStateChangedEvent;
import com.project.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeniorStateChangeNotificationListener {
    private final NotificationService notificationService;

    @EventListener
    @Transactional
    public void handleSeniorStateChangedEvent(SeniorStateChangedEvent event) {
        log.info("SeniorStateChangedEvent 수신 (알림 발송용): seniorId={}", event.senior().getId());
        try {
            notificationService.sendStateChangeNotificationToAdmins(
                    event.senior(),
                    event.previousState(),
                    event.newState(),
                    event.reason()
            );
            log.info("시니어 #{} 상태 변경 알림 발송 완료", event.senior().getId());
        } catch (Exception e) {
            log.error("시니어 상태 변경 알림 발송 중 예외 발생", e);
        }
    }
}