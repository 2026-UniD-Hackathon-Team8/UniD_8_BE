package com.unid8.damideo.common.auth;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * /api 요청의 X-Device-Id 헤더를 검사하고, 형식이 맞으면 요청 속성에 UUID로 담아둔다.
 */
@Component
public class DeviceIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod) || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(DeviceId.HEADER);
        if (!StringUtils.hasText(header)) {
            throw new BusinessException(CommonErrorCode.USER_NOT_FOUND);
        }

        request.setAttribute(DeviceId.REQUEST_ATTRIBUTE, parse(header.trim()));
        return true;
    }

    private UUID parse(String value) {
        try {
            UUID uuid = UUID.fromString(value);
            if (uuid.toString().equalsIgnoreCase(value)) {
                return uuid;
            }
        } catch (IllegalArgumentException ignored) {
            // 아래에서 공통 에러로 응답한다.
        }
        throw new BusinessException(CommonErrorCode.INVALID_INPUT,
                Map.of(DeviceId.HEADER, "기기 ID 형식이 올바르지 않습니다."));
    }
}
