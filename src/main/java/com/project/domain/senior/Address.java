package com.project.domain.senior;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {
	private String address;
	private String detail;
	
	@Enumerated(EnumType.STRING)
	private Gu gu;

	@Enumerated(EnumType.STRING)
	private Beopjeongdong dong;
	
    private Double latitude;
    private Double longitude;
    
    @Builder
    public Address(String address, String detail, Gu gu, Beopjeongdong dong, Double latitude, Double longitude) {
        this.address = address;
        this.detail = detail;
        this.gu = gu;
        this.dong = dong;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}