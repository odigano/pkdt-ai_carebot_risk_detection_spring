package com.project.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequestDto(
    @NotBlank(message = "새 비밀번호는 비워둘 수 없습니다.")
    String newPassword
) {}