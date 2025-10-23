package com.project.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.member.Member;
import com.project.domain.member.Role;
import com.project.dto.MemberDto;
import com.project.dto.request.SignUpRequestDto;
import com.project.persistence.MemberRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
	private final MemberRepository memberRepository;
	private final PasswordEncoder encoder;

	@Transactional
	public MemberDto register(SignUpRequestDto requestDto) {
		log.info("회원 가입 시도: username={}", requestDto.username());
		if (memberRepository.existsById(requestDto.username()))
			throw new IllegalArgumentException("이미 존재하는 사용자 이름입니다.");
		
		Member member = Member.builder()
				.username(requestDto.username())
				.password(encoder.encode(requestDto.password()))
				.role(Role.ROLE_ADMIN)
				.enabled(true)
				.build();
		
		memberRepository.save(member);
		log.info("회원 가입 성공: username={}", member.getUsername());
		return new MemberDto(member);
	}
	
	@Transactional(readOnly = true)
    public List<MemberDto> findAllMembers() {
		log.info("전체 회원 목록 조회");
        List <Member> members = memberRepository.findAll();
        return members.stream()
        		.map(MemberDto::new)
        		.collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MemberDto findMemberByUsername(String username) {
    	log.info("특정 회원 정보 조회: username={}", username);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return new MemberDto(member);
    }

    @Transactional
    public MemberDto updateMember(String username, MemberDto requestDto) {
    	log.info("회원 정보 수정 시도: username={}, role={}, enabled={}", username, requestDto.role(), requestDto.enabled());
        Member member = memberRepository.findByUsername(username)
        		.orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + username));;
        member.update(requestDto.role(), requestDto.enabled());
        log.info("회원 정보 수정 완료: username={}", username);
        return new MemberDto(member);
    }

    @Transactional
    public void deleteMember(String username) {
    	log.info("회원 삭제 시도: username={}", username);
        if (!memberRepository.existsByUsername(username))
            throw new EntityNotFoundException("사용자를 찾을 수 없습니다: " + username);
        memberRepository.deleteById(username);
        log.info("회원 삭제 완료: username={}", username);
    }
}