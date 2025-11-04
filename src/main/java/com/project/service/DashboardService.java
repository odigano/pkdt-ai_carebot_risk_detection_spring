package com.project.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.analysis.OverallResult;
import com.project.domain.analysis.Risk;
import com.project.domain.senior.Senior;
import com.project.dto.response.DashboardResponseDto;
import com.project.dto.response.DashboardSeniorDto;
import com.project.dto.response.SeniorsByStateDto;
import com.project.dto.response.StateCountDto;
import com.project.persistence.OverallResultRepository;
import com.project.persistence.SeniorRepository;
import com.project.persistence.SeniorStateHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final SeniorRepository seniorRepository;
    private final OverallResultRepository overallResultRepository;
    private final SeniorStateHistoryRepository seniorStateHistoryRepository;

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboardData() {
        long totalSeniors = seniorRepository.count();
        Map<Risk, Long> countsMap = seniorRepository.countSeniorsByState().stream()
                .collect(Collectors.toMap(row -> (Risk) row[0], row -> (Long) row[1]));

        StateCountDto stateCountDto = new StateCountDto(
                totalSeniors,
                countsMap.getOrDefault(Risk.POSITIVE, 0L),
                countsMap.getOrDefault(Risk.DANGER, 0L),
                countsMap.getOrDefault(Risk.CRITICAL, 0L),
                countsMap.getOrDefault(Risk.EMERGENCY, 0L)
        );

        Map<Long, LocalDateTime> latestStateChangeMap = seniorStateHistoryRepository.findLatestStateChangeTimestampForEachSenior()
                .stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (LocalDateTime) result[1]
                ));
        
        Map<Long, OverallResult> latestOverallResultMap = overallResultRepository.findLatestOverallResultForEachSenior()
                .stream()
                .collect(Collectors.toMap(
                        result -> result.getSenior().getId(),
                        Function.identity()
                ));

        Map<Risk, List<DashboardSeniorDto>> seniorsByState = new EnumMap<>(Risk.class);
        for (Risk risk : Risk.values()) {
            seniorsByState.put(risk, new ArrayList<>());
        }

        List<Senior> allSeniors = seniorRepository.findAll();
        for (Senior senior : allSeniors) {
            LocalDateTime lastStateChangedAt = latestStateChangeMap.get(senior.getId());
            OverallResult latestResult = latestOverallResultMap.get(senior.getId());
            
            DashboardSeniorDto dto = DashboardSeniorDto.from(senior, lastStateChangedAt, latestResult);
            
            if (seniorsByState.containsKey(senior.getState())) {
                seniorsByState.get(senior.getState()).add(dto);
            }
        }
        
        SeniorsByStateDto seniorsByStateDto = new SeniorsByStateDto(
                seniorsByState.get(Risk.POSITIVE),
                seniorsByState.get(Risk.DANGER),
                seniorsByState.get(Risk.CRITICAL),
                seniorsByState.get(Risk.EMERGENCY)
        );

        return new DashboardResponseDto(stateCountDto, seniorsByStateDto);
    }
}