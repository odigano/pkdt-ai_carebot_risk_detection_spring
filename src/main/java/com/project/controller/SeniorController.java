package com.project.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.project.dto.request.SeniorRequestDto;
import com.project.dto.request.SeniorSearchCondition;
import com.project.dto.request.UpdateSeniorStateRequestDto;
import com.project.dto.response.CustomPageDto;
import com.project.dto.response.SeniorDetailResponseDto;
import com.project.dto.response.SeniorListResponseDto;
import com.project.dto.response.SeniorResponseDto;
import com.project.dto.response.SeniorStateHistoryResponseDto;
import com.project.service.SeniorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seniors")
@RequiredArgsConstructor
public class SeniorController {
    private final SeniorService seniorService;
    @Value("${senior.photo.upload-path}")
    private String uploadPath;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SeniorResponseDto> createSenior(
            @Valid @RequestPart("senior") SeniorRequestDto requestDto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
    	SeniorResponseDto senior = seniorService.createSenior(requestDto, photo);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(senior.id())
				.toUri();
        return ResponseEntity.created(location).body(senior);
    }
	
    @GetMapping
    public ResponseEntity<CustomPageDto<SeniorListResponseDto>> searchSeniors(
            @Valid @ModelAttribute SeniorSearchCondition condition, Pageable pageable) {
        Page<SeniorListResponseDto> results = seniorService.searchSeniors(condition, pageable);
        return ResponseEntity.ok(CustomPageDto.from(results));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SeniorDetailResponseDto> getSeniorDetails(@PathVariable Long id) {
        SeniorDetailResponseDto seniorDetails = seniorService.getSeniorDetails(id);
        return ResponseEntity.ok(seniorDetails);
    }
    
    @GetMapping("/{id}/state-history")
    public ResponseEntity<List<SeniorStateHistoryResponseDto>> getSeniorStateHistory(@PathVariable Long id) {
        List<SeniorStateHistoryResponseDto> history = seniorService.getSeniorStateHistory(id);
        return ResponseEntity.ok(history);
    }
    
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SeniorResponseDto> updateSenior(
            @PathVariable Long id, 
            @RequestPart("senior") SeniorRequestDto seniorDto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
    	SeniorResponseDto senior = seniorService.updateSenior(id, seniorDto, photo);
        return ResponseEntity.ok(senior);
    }
    
    @PostMapping("/{id}/state")
    public ResponseEntity<Void> updateSeniorState(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSeniorStateRequestDto requestDto) {
        seniorService.updateSeniorState(id, requestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSenior(@PathVariable Long id) {
        seniorService.deleteSenior(id);
        return ResponseEntity.noContent().build();
    }
}