package leets.weeth.global.auth.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leets.weeth.global.common.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        setResponse(response);
        log.error("ExceptionClass: {}, Message: {}", authException.getClass().getSimpleName(), authException.getMessage());
    }

    private void setResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = new ObjectMapper().writeValueAsString(CommonResponse.createFailure(ErrorMessage.UNAUTHORIZED.getCode(), ErrorMessage.UNAUTHORIZED.getMessage()));
        response.getWriter().write(message);
    }
}
