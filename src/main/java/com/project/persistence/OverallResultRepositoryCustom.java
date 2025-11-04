package com.project.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project.dto.request.OverallResultSearchCondition;
import com.project.dto.response.OverallResultListResponseDto;

public interface OverallResultRepositoryCustom {
    Page<OverallResultListResponseDto> searchOverallResults(OverallResultSearchCondition condition, Pageable pageable);
}