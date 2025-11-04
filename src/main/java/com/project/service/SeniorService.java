package com.project.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.domain.analysis.OverallResult;
import com.project.domain.analysis.Risk;
import com.project.domain.senior.Address;
import com.project.domain.senior.Doll;
import com.project.domain.senior.Guardian;
import com.project.domain.senior.MedicalInfo;
import com.project.domain.senior.Senior;
import com.project.dto.request.SeniorRequestDto;
import com.project.dto.request.SeniorSearchCondition;
import com.project.dto.request.UpdateSeniorStateRequestDto;
import com.project.dto.response.RecentOverallResultDto;
import com.project.dto.response.SeniorDetailResponseDto;
import com.project.dto.response.SeniorListResponseDto;
import com.project.dto.response.SeniorResponseDto;
import com.project.dto.response.SeniorStateHistoryResponseDto;
import com.project.event.SeniorStateChangedEvent;
import com.project.persistence.DollRepository;
import com.project.persistence.OverallResultRepository;
import com.project.persistence.SeniorRepository;
import com.project.persistence.SeniorStateHistoryRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeniorService {
	private final SeniorRepository seniorRepository;
	private final DollRepository dollRepository;
	private final OverallResultRepository overallResultRepository;
	private final SeniorStateHistoryRepository seniorStateHistoryRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Value("${senior.photo.upload-path}")
    private String uploadPath;
	
	@Transactional
	public SeniorResponseDto createSenior(SeniorRequestDto requestDto, MultipartFile photo) {
		log.info("신규 시니어 등록 시작: name={}", requestDto.name());
		Doll doll = dollRepository.findByIdWithSenior(requestDto.dollId())
				.orElseThrow(() -> new EntityNotFoundException("인형 " + requestDto.dollId() + " 없음."));
		
		if (doll.getSenior() != null)
			throw new IllegalArgumentException("해당 인형은 이미 사용 중.");

		String photoFilename = savePhoto(photo);
		
		Senior senior = dtoToSenior(requestDto, photoFilename);
		senior.changeDoll(doll);
		Senior savedSenior = seniorRepository.save(senior);
		SeniorStateChangedEvent event = new SeniorStateChangedEvent(
	            savedSenior,
	            null,
	            savedSenior.getState(),
	            "신규 등록"
	        );
	        eventPublisher.publishEvent(event);
	        log.info("신규 시니어 등록 완료: seniorId={}, dollId={}", savedSenior.getId(), doll.getId());
		return new SeniorResponseDto(senior);
	}

	@Transactional(readOnly = true)
    public Page<SeniorListResponseDto> searchSeniors(SeniorSearchCondition condition, Pageable pageable) {
		log.info("시니어 목록 검색: condition={}, pageable={}", condition, pageable);
        return seniorRepository.searchSeniors(condition, pageable);
    }
	
	@Transactional(readOnly = true)
    public SeniorDetailResponseDto getSeniorDetails(Long id) {
		log.info("특정 시니어 상세 정보 조회: seniorId={}", id);
		
		Senior senior = seniorRepository.findByIdWithDoll(id)
	            .orElseThrow(() -> new EntityNotFoundException("시니어 " + id + "는 없음."));

	    List<RecentOverallResultDto> recentResults = overallResultRepository.findTop5BySeniorIdOrderByTimestampDesc(id).stream()
	            .map(RecentOverallResultDto::from)
	            .collect(Collectors.toList());

	    return new SeniorDetailResponseDto(senior, recentResults);
    }
	
	@Transactional(readOnly = true)
    public List<SeniorStateHistoryResponseDto> getSeniorStateHistory(Long seniorId) {
        log.info("시니어 상태 변경 이력 조회 요청: seniorId={}", seniorId);
        if (!seniorRepository.existsById(seniorId))
            throw new EntityNotFoundException("시니어 " + seniorId + "는 없음.");
        return seniorStateHistoryRepository.findBySeniorIdOrderByChangedAtDesc(seniorId)
                .stream()
                .map(SeniorStateHistoryResponseDto::from)
                .collect(Collectors.toList());
    }

	@Transactional
	public SeniorResponseDto updateSenior(Long id, SeniorRequestDto seniorDto, MultipartFile photo) {
		log.info("시니어 정보 수정 시작: seniorId={}", id);
		Senior existingSenior = seniorRepository.findByIdWithDoll(id)
				.orElseThrow(() -> new EntityNotFoundException("시니어 " + id + "는 없음."));

		if (!existingSenior.getDoll().getId().equals(seniorDto.dollId())) {
			log.info("시니어 인형 변경: seniorId={}, oldDoll={}, newDoll={}", id, existingSenior.getDoll().getId(), seniorDto.dollId());
			Doll newDoll = dollRepository.findById(seniorDto.dollId()).orElseThrow(
					() -> new EntityNotFoundException("인형 " + seniorDto.dollId() + "없음."));
			if (newDoll.getSenior() != null)
				throw new IllegalArgumentException("해당 인형은 이미 사용 중.");
			existingSenior.changeDoll(newDoll);
		}
		
		String newPhotoFilename = existingSenior.getPhoto();
        if (photo != null && !photo.isEmpty()) {
            if (newPhotoFilename != null) {
                deletePhoto(newPhotoFilename);
            }
            newPhotoFilename = savePhoto(photo);
        }

		updateSenior(existingSenior, seniorDto, newPhotoFilename);
		log.info("시니어 정보 수정 완료: seniorId={}", id);
		return new SeniorResponseDto(existingSenior);
	}
	
	@Transactional
	public void updateSeniorState(Long seniorId, UpdateSeniorStateRequestDto requestDto) {
		log.info("관리자에 의한 시니어 상태 변경 요청: seniorId={}, newState={}, reason={}", seniorId, requestDto.newState(), requestDto.reason());
	    Senior senior = seniorRepository.findById(seniorId)
	            .orElseThrow(() -> new EntityNotFoundException("시니어 " + seniorId + "는 없음."));

	    Risk newState = requestDto.newState();
	    
	    if (requestDto.overallResultId() != null) {
            OverallResult overallResult = overallResultRepository.findById(requestDto.overallResultId())
                    .orElseThrow(() -> new EntityNotFoundException("분석 ID " + requestDto.overallResultId() + "를 찾을 수 없습니다."));
            if (!overallResult.getSenior().getId().equals(seniorId))
                throw new IllegalArgumentException("해당 분석 결과는 senior ID " + seniorId + "에 속하지 않습니다.");
            overallResult.resolveWithLabel(newState);
            log.info("분석 ID {}가 조치 완료 처리되었습니다.", requestDto.overallResultId());
        }
	    
	    Risk previousState = senior.getState();

	    if (previousState != newState) {
	        senior.updateState(newState);
	        
	        SeniorStateChangedEvent event = new SeniorStateChangedEvent(
	            senior,
	            previousState,
	            newState,
	            requestDto.reason()
	        );
	        eventPublisher.publishEvent(event);
	        log.info("관리자에 의해 시니어 #{}의 상태가 {} -> {}로 변경되었습니다. 사유: {}", 
	                seniorId, previousState, newState, requestDto.reason());
	    }
	}

	@Transactional
	public void deleteSenior(Long id) {
		log.info("시니어 삭제 요청: seniorId={}", id);
		Senior senior = seniorRepository.findByIdWithDoll(id)
				.orElseThrow(() -> new EntityNotFoundException("시니어 " + id + "는 없음."));
		
		if (senior.getPhoto() != null) {
            deletePhoto(senior.getPhoto());
        }
		
		if (senior.getDoll() != null) {
			log.info("시니어 삭제로 인한 인형 할당 해제: dollId={}", senior.getDoll().getId());
	        senior.changeDoll(null);
		}
		seniorRepository.deleteById(id);
		log.info("시니어 삭제 완료: seniorId={}", id);
	}
	
	private String savePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return null;
        }

        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFilename = photo.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFilename = UUID.randomUUID().toString() + extension;

            Path destination = Paths.get(uploadPath, savedFilename);
            photo.transferTo(destination);

            return savedFilename;
        } catch (IOException e) {
            log.error("사진 파일 저장 실패", e);
            throw new RuntimeException("사진 파일을 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    private void deletePhoto(String filename) {
        if (filename == null) return;
        try {
            Path fileToDelete = Paths.get(uploadPath, filename);
            Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            log.error("사진 파일 삭제 실패: " + filename, e);
        }
    }

    private MedicalInfo dtoToMedicalInfo(SeniorRequestDto dto) {
        return MedicalInfo.builder()
        		.diseases(dto.diseases())
        		.medications(dto.medications())
        		.note(dto.diseaseNote())
        		.build();
    }
	
    private Guardian dtoToGuardian(SeniorRequestDto dto) {
        return Guardian.builder()
        		.guardianName(dto.guardianName())
        		.guardianPhone(dto.guardianPhone())
        		.relationship(dto.relationship())
        		.guardianNote(dto.guardianNote())
        		.build();
    }
    
    private Address dtoToAddress(SeniorRequestDto dto) {
        return Address.builder()
        		.address(dto.address())
        		.detail(dto.addressDetail())
        		.gu(dto.gu())
        		.dong(dto.dong())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
        		.build();
    }
	
    private Senior dtoToSenior(SeniorRequestDto dto, String photoFilename) {
        if (dto == null)
            return null;
        Guardian guardian = dtoToGuardian(dto);
        MedicalInfo medicalInfo = dtoToMedicalInfo(dto);
        Address address = dtoToAddress(dto);
        Senior senior = Senior.builder()
        		.name(dto.name())
        		.photo(photoFilename)
        		.birthDate(dto.birthDate())
        		.sex(dto.sex())
        		.residence(dto.residence())
        		.phone(dto.phone())
        		.address(address)
        		.note(dto.note())
        		.guardian(guardian)
        		.medicalInfo(medicalInfo)
        		.build();
        return senior;
    }
    
    private void updateSenior(Senior senior, SeniorRequestDto dto, String newPhotoFilename) {
        Address address = dtoToAddress(dto);
    	senior.updatePersonalInfo(dto.name(), newPhotoFilename, dto.birthDate(), dto.sex(), dto.residence(),
    			dto.phone(), address, dto.note());
    	senior.updateGuardianInfo(dtoToGuardian(dto));
    	senior.updateMedicalInfo(dtoToMedicalInfo(dto));
    }
}
