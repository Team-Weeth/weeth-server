package leets.weeth.global.auth.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leets.weeth.global.common.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        setResponse(response);
        log.error("ExceptionClass: {}, Message: {}", accessDeniedException.getClass().getSimpleName(), accessDeniedException.getMessage());
    }

    private void setResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = new ObjectMapper().writeValueAsString(CommonResponse.createFailure(ErrorMessage.FORBIDDEN.getCode(), ErrorMessage.FORBIDDEN.getMessage()));
        response.getWriter().write(message);
    }
}
