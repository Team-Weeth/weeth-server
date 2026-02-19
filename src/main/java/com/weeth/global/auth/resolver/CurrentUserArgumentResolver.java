package com.weeth.global.auth.resolver;

import com.weeth.global.auth.annotation.CurrentUser;
import com.weeth.global.auth.jwt.exception.AnonymousAuthenticationException;
import com.weeth.global.auth.model.AuthenticatedUser;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {   // parameter가 해당 resolver를 지원하는 여부 확인
        boolean hasAnnotation = parameter.hasParameterAnnotation(CurrentUser.class);    // @CurrentUser이 존재하는가?
        boolean parameterType = Long.class.isAssignableFrom(parameter.getParameterType());  // 파라미터 타입이 Long을 상속하거나 구현하였는가?
        return hasAnnotation && parameterType;  // 둘 다 충족할 시 true
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();     // 인증 객체 가져오기

        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new AnonymousAuthenticationException();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.id();
        }

        throw new AnonymousAuthenticationException();
    }
}
