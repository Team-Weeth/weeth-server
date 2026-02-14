package leets.weeth.domain.user.application.mapper;

import leets.weeth.domain.user.application.dto.response.UserResponseDto;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import leets.weeth.domain.user.domain.entity.enums.Department;
import leets.weeth.global.auth.jwt.application.dto.JwtDto;
import org.mapstruct.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static leets.weeth.domain.user.application.dto.request.UserRequestDto.Register;
import static leets.weeth.domain.user.application.dto.request.UserRequestDto.SignUp;
import static leets.weeth.domain.user.application.dto.response.UserResponseDto.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mappings({
            @Mapping(target = "password", expression = "java( passwordEncoder.encode(dto.password()) )"),
            @Mapping(target = "department", expression = "java( leets.weeth.domain.user.domain.entity.enums.Department.to(dto.department()) )")
    })
    User from(SignUp dto, @Context PasswordEncoder passwordEncoder);

    @Mappings({
            @Mapping(target = "department", expression = "java( leets.weeth.domain.user.domain.entity.enums.Department.to(dto.department()) )")
    })
    User from(Register dto);

    @Mapping(target = "department", expression = "java( toString(user.getDepartment()) )")
    @Mapping(target = "cardinals", expression = "java( toCardinalNumbers(userCardinals) )")
    Response to(User user, List<UserCardinal> userCardinals);

    @Mappings({
            // 수정: 출석률, 출석 횟수, 결석 횟수 매핑 추후 추가 예정
            @Mapping(target = "cardinals", expression = "java( toCardinalNumbers(userCardinals) )")
    })
    AdminResponse toAdminResponse(User user, List<UserCardinal> userCardinals);

    @Mapping(target = "cardinals", expression = "java( toCardinalNumbers(userCardinals) )")
    SummaryResponse toSummaryResponse(User user, List<UserCardinal> userCardinals);

    SocialAuthResponse toSocialAuthResponse(Long kakaoId);

    @Mappings({
            @Mapping(target = "status", expression = "java(LoginStatus.LOGIN)"),
            @Mapping(target = "id", source = "user.id"),
            @Mapping(target = "kakaoId", source = "user.kakaoId"),
            @Mapping(target = "appleIdToken", expression = "java(null)")
    })
    SocialLoginResponse toLoginResponse(User user, JwtDto dto);

    @Mappings({
            @Mapping(target = "status", expression = "java(LoginStatus.INTEGRATE)"),
            @Mapping(target = "appleIdToken", expression = "java(null)"),
            @Mapping(target = "accessToken", expression = "java(null)"),
            @Mapping(target = "refreshToken", expression = "java(null)")
    })
    SocialLoginResponse toIntegrateResponse(Long kakaoId);

    @Mappings({
            // 상세 데이터 매핑
            @Mapping(target = "cardinals", expression = "java( toCardinalNumbers(userCardinals) )")
    })
    UserResponse toUserResponse(User user, List<UserCardinal> userCardinals);

    @Mapping(target = "cardinals", expression = "java( toCardinalNumbers(userCardinals) )")
    UserResponseDto.UserInfo toUserInfoDto(User user, List<UserCardinal> userCardinals);

    @Mappings({
            @Mapping(target = "status", expression = "java(LoginStatus.LOGIN)"),
            @Mapping(target = "id", source = "user.id"),
            @Mapping(target = "appleIdToken", expression = "java(null)"),
            @Mapping(target = "kakaoId", expression = "java(null)")
    })
    SocialLoginResponse toAppleLoginResponse(User user, JwtDto dto);

    @Mappings({
            @Mapping(target = "status", expression = "java(LoginStatus.INTEGRATE)"),
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "appleIdToken", source = "appleIdToken"),
            @Mapping(target = "kakaoId", expression = "java(null)"),
            @Mapping(target = "accessToken", expression = "java(null)"),
            @Mapping(target = "refreshToken", expression = "java(null)")
    })
    SocialLoginResponse toAppleIntegrateResponse(String appleIdToken);

    default String toString(Department department) {
        return department.getValue();
    }

    default List<Integer> toCardinalNumbers(List<UserCardinal> userCardinals) {
        if (userCardinals == null || userCardinals.isEmpty()) {
            return Collections.emptyList();
        }

        return userCardinals.stream()
                .map(uc -> uc.getCardinal().getCardinalNumber())
                .collect(Collectors.toList());
    }
}

