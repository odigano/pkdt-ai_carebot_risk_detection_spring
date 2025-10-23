package com.project.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.analysis.Risk;
import com.project.domain.member.Member;
import com.project.domain.member.Role;
import com.project.domain.notification.Notification;
import com.project.domain.notification.NotificationType;
import com.project.domain.senior.Senior;
import com.project.dto.response.AnalysisResponseWithIdDto;
import com.project.dto.response.NotificationResponseDto;
import com.project.dto.sse.SseNotificationPayload;
import com.project.persistence.MemberRepository;
import com.project.persistence.NotificationRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000;

    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(username, emitter);

        emitter.onCompletion(() -> emitters.remove(username));
        emitter.onTimeout(() -> emitters.remove(username));

        try {
            emitter.send(SseEmitter.event().id(username).name("connect").data("SSE 연결완료: " + username));
        } catch (IOException e) {
            log.error("{}에 대한 첫 SSE 연결 실패: {}", username, e.getMessage());
            emitters.remove(username);
        }

        log.info("새로운 SSE 구독자: {}", username);
        return emitter;
    }

    @Transactional
    public void sendAnalysisCompleteNotificationToAdmins(AnalysisResponseWithIdDto analysisResult) {
    	List<Member> admins = memberRepository.findByRole(Role.ROLE_ADMIN);

        String message = String.format("인형 '%s'의 분석이 완료되었습니다. (결과: %s)",
                analysisResult.overallResult().dollId(),
                analysisResult.overallResult().label());
        String resourceId = String.valueOf(analysisResult.id());

        for (Member admin : admins) {
            if (!admin.isEnabled()) continue;
            Notification notification = Notification.builder()
                    .recipient(admin)
                    .notificationType(NotificationType.ANALYSIS_COMPLETE)
                    .message(message)
                    .relatedResourceId(resourceId)
                    .build();
            notificationRepository.save(notification);

            sendNotificationToUser(admin.getUsername(), "notification", notification);
        }
    }
    
    @Transactional
    public void sendStateChangeNotificationToAdmins(Senior senior, Risk previousState, Risk newState, String reason) {
        List<Member> admins = memberRepository.findByRole(Role.ROLE_ADMIN);

        String message = String.format("'%s'님의 상태가 %s에서 %s로 변경되었습니다. (사유: %s)",
                senior.getName(),
                previousState != null ? previousState.name() : "신규",
                newState.name(),
                reason);
        String resourceId = String.valueOf(senior.getId());

        for (Member admin : admins) {
            if (!admin.isEnabled()) continue;
            Notification notification = Notification.builder()
                    .recipient(admin)
                    .notificationType(NotificationType.SENIOR_STATE_CHANGED)
                    .message(message)
                    .relatedResourceId(resourceId)
                    .build();
            notificationRepository.save(notification);

            sendNotificationToUser(admin.getUsername(), "notification", notification);
        }
    }

    private void sendNotificationToUser(String username, String eventName, Notification notification) {
        SseEmitter emitter = emitters.get(username);
        if (emitter != null) {
            try {
                SseNotificationPayload payload = SseNotificationPayload.from(notification);
                String payloadJson = objectMapper.writeValueAsString(payload);

                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payloadJson));
                log.info("'{}'로 알림 전송 완료: {}", username, payloadJson);
            } catch (Exception e) {
                log.error("'{}'로 알림 전송 실패: {}", username, e.getMessage());
                emitters.remove(username);
            }
        } else {
            log.info("'{}'는 오프라인, DB에 저장", username);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsForUser(String username) {
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + notificationId + "인 알림 없음"));
        notification.markAsRead();
    }
}