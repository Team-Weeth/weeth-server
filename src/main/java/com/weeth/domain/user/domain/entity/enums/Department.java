package com.weeth.domain.user.domain.entity.enums;

import com.weeth.domain.user.application.exception.DepartmentNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Department {

    SW("소프트웨어전공"),
    AI("인공지능전공"),
    COMPUTER_SCIENCE("컴퓨터공학과"),
    INDUSTRIAL_ENGINEERING("산업공학과"),
    VISUAL_DESIGN("시각디자인학과"),
    BUSINESS("경영학과"),
    ECONOMICS("경제학과"),
    KOREAN_LANGUAGE("한국어문학과"),
    URBAN_PLANNING("도시계획학전공"),
    GLOBAL_BUSINESS("글로벌경영학과"),
    FINANCIAL_MATHEMATICS("금융수학전공"),
    HEALTHCARE_MANAGEMENT("의료산업경영학과"); // 더 필요한 학과는 추후 추가할 예정

    private final String value;

    public static Department to(String before) {
        return Arrays.stream(Department.values())
                .filter(department -> department.getValue().equals(before))
                .findAny()
                .orElseThrow(DepartmentNotFoundException::new);
    }
}
