package com.project.dto.request;

import com.project.domain.member.Role;

public record MemberUpdateRequestDto(
    Role role,
    Boolean enabled
) {}