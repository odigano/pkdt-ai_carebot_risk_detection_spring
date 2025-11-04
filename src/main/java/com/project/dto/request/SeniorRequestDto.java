package com.project.dto.request;

import java.time.LocalDate;

import com.project.domain.senior.Gu;
import com.project.domain.senior.Beopjeongdong;
import com.project.domain.senior.Residence;
import com.project.domain.senior.Sex;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

public record SeniorRequestDto(
		@NotBlank(message = "인형 ID는 필수입니다.")
		String dollId,
		
		@NotBlank(message = "이름은 필수입니다.")
		String name, 
		
		@NotNull(message = "생년월일은 필수입니다.")
		@Past(message = "생년월일은 과거 날짜여야 합니다.")
		LocalDate birthDate,
		
		@NotNull(message = "성별은 필수입니다.")
		Sex sex, 
		
		@NotNull(message = "거주 형태는 필수입니다.")
		Residence residence,
		
		@NotBlank(message = "전화번호는 필수입니다.")
		@Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "유효한 전화번호 형식이 아닙니다 (예: 010-1234-5678).")
		String phone,

		@NotBlank(message = "주소는 필수입니다.")
		String address,
		String addressDetail,
		@NotNull(message = "구 정보는 필수입니다.")
		Gu gu,
		@NotNull(message = "동 정보는 필수입니다.")
		Beopjeongdong dong,
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,
        @NotNull(message = "경도는 필수입니다.")
        Double longitude,
		
		String note, 
		
		@NotBlank(message = "보호자 이름은 필수입니다.")
		String guardianName,
		
		@NotBlank(message = "보호자 전화번호는 필수입니다.")
		@Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "유효한 전화번호 형식이 아닙니다 (예: 010-1234-5678).")
		String guardianPhone,
		
		@NotBlank(message = "보호자와의 관계는 필수입니다.")
		String relationship,
		
		String guardianNote,
		String diseases, 
		String medications,
		String diseaseNote
) {
}