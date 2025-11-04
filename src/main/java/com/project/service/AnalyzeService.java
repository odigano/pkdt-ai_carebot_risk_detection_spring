package com.project.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.project.domain.analysis.ConfidenceScores;
import com.project.domain.analysis.Dialogue;
import com.project.domain.analysis.OverallResult;
import com.project.domain.analysis.Reason;
import com.project.domain.analysis.Risk;
import com.project.domain.senior.Doll;
import com.project.domain.senior.Senior;
import com.project.dto.ConfidenceScoresDto;
import com.project.dto.request.DialogueAnalysisRequestDto;
import com.project.dto.request.OverallResultSearchCondition;
import com.project.dto.response.AnalysisDetailResponseDto;
import com.project.dto.response.AnalysisResponseDto;
import com.project.dto.response.AnalysisResponseWithIdDto;
import com.project.dto.response.DialogueAnalysisResponseDto;
import com.project.dto.response.OverallResultListResponseDto;
import com.project.event.SeniorStateChangedEvent;
import com.project.exception.InvalidFileException;
import com.project.persistence.DollRepository;
import com.project.persistence.OverallResultRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeService {
    private final RestTemplate restTemplate;
    private final DollRepository dollRepository;
    private final OverallResultRepository overallResultRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${python.server.url}")
    private String pythonServerUrl;

    @Transactional
    public AnalysisResponseWithIdDto analyzeAndSave(MultipartFile file) {
    	log.info("대화 분석 요청 수신: fileName={}", file.getOriginalFilename());
        if (file == null || file.isEmpty()) {
        	log.warn("분석 요청 파일이 비어있음");
            throw new InvalidFileException("파일이 없거나 비어있습니다.");
        }
        log.info("대화 분석 시작: fileName={}", file.getOriginalFilename());
        List<DialogueAnalysisRequestDto> reqeustDialogues = new ArrayList<>();

        DateTimeFormatter csvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");
        boolean firstCheck = true;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.readNext();
            String[] columns;
            while ((columns = reader.readNext()) != null) {
                String dollId = columns[0].trim();
                String text = columns[1].trim();
                String utteredAtCsv = columns[2].trim();
                
                if (dollId.isEmpty() || text.isEmpty() || utteredAtCsv.isEmpty()) {
                    log.warn("비었거나 잘못된 라인 스킵: doll_id={}, text={}, uttered_at={}", dollId, text, utteredAtCsv);
                    continue;
                }
                
                if (firstCheck) {
                    Doll doll = dollRepository.findByIdWithSenior(dollId)
                            .orElseThrow(() -> new EntityNotFoundException("인형 " + dollId + "가 없음."));
                    if(doll.getSenior() == null)
                    	throw new EntityNotFoundException("인형에 할당된 시니어가 없음.");
                    log.info("CSV 파일 검증 완료: dollId={}, seniorId={}", doll.getId(), doll.getSenior().getId());
                    firstCheck = false;
                }
                
                LocalDateTime dateTime = LocalDateTime.parse(utteredAtCsv, csvFormatter);
                reqeustDialogues.add(new DialogueAnalysisRequestDto(dollId, text, dateTime));
            }
        } catch (DateTimeParseException e) {
            throw new InvalidFileException("날짜 포맷은 YYYY-MM-DD HH:MM:SS이어야 합니다.", e);
        } catch (CsvValidationException e) {
            throw new InvalidFileException("잘못된 형식의 CSV 파일입니다.", e);
        } catch (IOException e) {
            throw new InvalidFileException("파일을 읽는 중 오류가 발생했습니다.", e);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<DialogueAnalysisRequestDto>> requestEntity = new HttpEntity<>(reqeustDialogues, headers);
        log.info("Python 서버로 분석 요청: url={}, dialogueCount={}", pythonServerUrl, reqeustDialogues.size());
        AnalysisResponseDto apiResponse = restTemplate.postForObject(
                pythonServerUrl + "/analyze",
                requestEntity,
                AnalysisResponseDto.class);
        log.info("Python 서버로부터 분석 결과 수신 완료");
        AnalysisResponseWithIdDto result = saveAnalysisResult(apiResponse);
        notificationService.sendAnalysisCompleteNotificationToAdmins(result);
        return result;
    }
    
    @Transactional
    private AnalysisResponseWithIdDto saveAnalysisResult(AnalysisResponseDto responseDto) {
        String responseDollId = responseDto.overallResult().dollId();
        log.info("분석 결과 저장 시작: dollId={}", responseDollId);
        
        Doll doll = dollRepository.findByIdWithSenior(responseDollId)
                .orElseThrow(() -> new EntityNotFoundException("인형 " + responseDollId + "가 없음."));
        
        ConfidenceScoresDto overallScoresDto = responseDto.overallResult().confidenceScores();
        ConfidenceScores overallScores = dtoToConfidenceScores(overallScoresDto);
        
        List<String> evidenceTexts = responseDto.overallResult().reason().evidence().stream()
                .map(evidenceDto -> evidenceDto.text())
                .collect(Collectors.toList());
        
        Reason reason = Reason.builder()
                .reasons(evidenceTexts)
                .summary(responseDto.overallResult().reason().summary())
                .build();
        
        OverallResult overallResult = OverallResult.builder()
                .doll(doll)
                .senior(doll.getSenior())
                .label(responseDto.overallResult().label())
                .confidenceScores(overallScores)
                .reason(reason)
                .treatmentPlan(responseDto.overallResult().treatmentPlan())
                .build();
        
        for (DialogueAnalysisResponseDto dialogueDto : responseDto.dialogueResult()) {
            ConfidenceScoresDto dialogueScoresDto = dialogueDto.confidenceScores();
            ConfidenceScores dialogueScores = dtoToConfidenceScores(dialogueScoresDto);
            
            Dialogue dialogue = Dialogue.builder()
                    .text(dialogueDto.text())
                    .utteredAt(dialogueDto.utteredAt())
                    .label(dialogueDto.label())
                    .confidenceScores(dialogueScores)
                    .build();
            
            overallResult.addDialogue(dialogue);
        }
        
        Senior senior = overallResult.getSenior();
        Risk previousState = senior.getState();
        Risk newState = overallResult.getLabel();
        
        senior.updateState(newState);

        OverallResult savedResult = overallResultRepository.save(overallResult);

        if (previousState != newState) {
        	String changeReason = String.format("분석 ID: %d의 결과로 상태 변경", savedResult.getId());
            SeniorStateChangedEvent event = new SeniorStateChangedEvent(
                senior, 
                previousState, 
                newState, 
                changeReason
            );
            log.info("SeniorStateChangedEvent 발행: seniorId={}, reason={}", senior.getId(), changeReason);
            eventPublisher.publishEvent(event);
        }
        
        log.info("분석 결과 저장 완료: overallResultId={}, dollId={}", savedResult.getId(), responseDollId);

        return new AnalysisResponseWithIdDto(savedResult.getId(), responseDto.overallResult(), responseDto.dialogueResult());
    }

    @Transactional(readOnly = true)
    public Page<OverallResultListResponseDto> searchOverallResults(OverallResultSearchCondition condition, Pageable pageable) {
    	log.info("분석 결과 목록 검색: condition={}, pageable={}", condition, pageable);
    	return overallResultRepository.searchOverallResults(condition, pageable);
    }
    
    @Transactional(readOnly = true)
    public AnalysisDetailResponseDto getAnalysisDetails(Long id) {
    	log.info("특정 분석 상세 결과 조회: overallResultId={}", id);
        OverallResult overallResult = overallResultRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("ID: " + id + " 분석을 찾을 수 없습니다."));
        
        boolean hasNewerResult = overallResultRepository.existsBySeniorIdAndTimestampAfter(
                overallResult.getSenior().getId(), 
                overallResult.getTimestamp()
        );

        boolean isEditable = !hasNewerResult;
        
        return AnalysisDetailResponseDto.from(overallResult, isEditable);
    }
    
    @Transactional
	public void deleteAnalysis(Long id) {
    	log.info("분석 결과 삭제 요청: overallResultId={}", id);
    	if(!overallResultRepository.existsById(id))
    		throw new EntityNotFoundException("ID: " + id + " 분석을 찾을 수 없습니다.");
    	overallResultRepository.deleteById(id);
    	log.info("분석 결과 삭제 완료: overallResultId={}", id);
	}
	
    private ConfidenceScores dtoToConfidenceScores(ConfidenceScoresDto dto) {
        if (dto == null)
            return null;
        return ConfidenceScores.builder()
                .positive(dto.positive())
                .danger(dto.danger())
                .critical(dto.critical())
                .emergency(dto.emergency())
                .build();
    }
}