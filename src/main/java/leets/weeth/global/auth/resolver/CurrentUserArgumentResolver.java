package leets.weeth.global.auth.resolver;

import leets.weeth.global.auth.annotation.CurrentUser;
import leets.weeth.global.auth.jwt.exception.AnonymousAuthenticationException;
import leets.weeth.global.auth.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtService jwtService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {   // parameter가 해당 resolver를 지원하는 여부 확인
        boolean hasAnnotation = parameter.hasParameterAnnotation(CurrentUser.class);    // @CurrentUser이 존재하는가?
        boolean parameterType = Long.class.isAssignableFrom(parameter.getParameterType());  // 파라미터 타입이 Long을 상속하거나 구현하였는가?
        return hasAnnotation && parameterType;  // 둘 다 충족할 시 true
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();     // 인증 객체 가져오기

        if (authentication instanceof AnonymousAuthenticationToken) {   // 익명 인증 토큰의 인스턴스라면 0 반환
            throw new AnonymousAuthenticationException();
        }

        String token = Optional.ofNullable(webRequest.getHeader("Authorization"))
                .map(accessToken -> accessToken.replace("Bearer ", "")).get();

        return jwtService.extractId(token).get();  // 토큰에서 userId 조회
    }
}