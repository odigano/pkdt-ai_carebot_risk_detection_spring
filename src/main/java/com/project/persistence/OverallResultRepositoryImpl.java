package com.project.persistence;

import static com.project.domain.analysis.QOverallResult.overallResult;
import static com.project.domain.senior.QDoll.doll;
import static com.project.domain.senior.QSenior.senior;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import com.project.domain.analysis.Risk;
import com.project.domain.senior.Gu;
import com.project.domain.senior.Beopjeongdong;
import com.project.domain.senior.Sex;
import com.project.dto.request.OverallResultSearchCondition;
import com.project.dto.response.OverallResultListResponseDto;
import com.project.dto.response.QOverallResultListResponseDto;
import com.project.dto.response.QRecentUrgentResultDto;
import com.project.dto.response.RecentUrgentResultDto;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OverallResultRepositoryImpl implements OverallResultRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OverallResultListResponseDto> searchOverallResults(OverallResultSearchCondition condition, Pageable pageable) {
        List<OverallResultListResponseDto> content = queryFactory
                .select(new QOverallResultListResponseDto(
                        overallResult.id,
                        overallResult.label,
                        overallResult.reason.summary,
                        overallResult.timestamp,
                        doll.id,
                        senior.id,
                        senior.name,
                        senior.birthDate,
                        senior.sex,
                        senior.address.gu,
                        senior.address.dong,
                        overallResult.isResolved
                ))
                .from(overallResult)
                .join(overallResult.senior, senior)
                .join(overallResult.doll, doll)
                .where(
                        seniorIdEq(condition.getSeniorId()),
                        nameContains(condition.getName()),
                        sexEq(condition.getSex()),
                        guEq(condition.getGu()),
                        dongEq(condition.getDong()),
                        ageGroupEq(condition.getAgeGroup()),
                        dollIdEq(condition.getDollId()),
                        labelEq(condition.getLabel()),
                        timestampBetween(condition.getStartDate(), condition.getEndDate())
                )
                .orderBy(overallResult.timestamp.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(overallResult.count())
                .from(overallResult)
                .join(overallResult.senior, senior)
                .join(overallResult.doll, doll)
                .where(
                        seniorIdEq(condition.getSeniorId()),
                        nameContains(condition.getName()),
                        sexEq(condition.getSex()),
                        guEq(condition.getGu()),
                        dongEq(condition.getDong()),
                        ageGroupEq(condition.getAgeGroup()),
                        dollIdEq(condition.getDollId()),
                        labelEq(condition.getLabel()),
                        timestampBetween(condition.getStartDate(), condition.getEndDate())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
    
    @Override
    public List<RecentUrgentResultDto> findRecentUrgentResults() {
        NumberExpression<Integer> caseOrder = new CaseBuilder()
                .when(overallResult.label.eq(Risk.EMERGENCY)).then(1)
                .when(overallResult.label.eq(Risk.CRITICAL)).then(2)
                .when(overallResult.label.eq(Risk.DANGER)).then(3)
                .otherwise(4);

        return queryFactory
                .select(new QRecentUrgentResultDto(
                        overallResult.id,
                        overallResult.label,
                        senior.name,
                        senior.birthDate,
                        senior.sex,
                        senior.address.gu,
                        senior.address.dong,
                        overallResult.reason.summary,
                        overallResult.treatmentPlan,
                        overallResult.timestamp,
                        overallResult.isResolved
                ))
                .from(overallResult)
                .join(overallResult.senior, senior)
                .where(overallResult.label.in(Risk.EMERGENCY, Risk.CRITICAL, Risk.DANGER))
                .orderBy(caseOrder.asc(), overallResult.timestamp.desc())
                .limit(10)
                .fetch();
    }

    private BooleanExpression labelEq(Risk label) {
        return label != null ? overallResult.label.eq(label) : null;
    }

    private BooleanExpression timestampBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return null;
        }
        return overallResult.timestamp.between(
                LocalDateTime.of(startDate, LocalTime.MIN),
                LocalDateTime.of(endDate, LocalTime.MAX)
        );
    }

    private BooleanExpression seniorIdEq(Long seniorId) {
        return seniorId != null ? senior.id.eq(seniorId) : null;
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? senior.name.contains(name) : null;
    }
    
    private BooleanExpression sexEq(Sex sex) {
        return sex != null ? senior.sex.eq(sex) : null;
    }

    private BooleanExpression guEq(Gu gu) {
        return gu != null ? senior.address.gu.eq(gu) : null;
    }

    private BooleanExpression dongEq(Beopjeongdong dong) {
        return dong != null ? senior.address.dong.eq(dong) : null;
    }
    
    private BooleanExpression dollIdEq(String dollId) {
        return StringUtils.hasText(dollId) ? doll.id.eq(dollId) : null;
    }
    
    private BooleanExpression ageGroupEq(Integer ageGroup) {
        if (ageGroup == null) {
            return null;
        }
        LocalDate now = LocalDate.now();
        if (ageGroup == 100) {
            LocalDate birthDateLimit = now.minusYears(100);
            return senior.birthDate.loe(birthDateLimit);
        } else {
            LocalDate startDate = now.minusYears(ageGroup + 9).minusDays(1);
            LocalDate endDate = now.minusYears(ageGroup);
            return senior.birthDate.between(startDate, endDate);
        }
    }
}