package com.unid8.damideo.common.auth;

import com.unid8.damideo.common.exception.BusinessException;
import com.unid8.damideo.common.exception.CommonErrorCode;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectProvider<DeviceUserReader> deviceUserReader;

    public CurrentUserArgumentResolver(ObjectProvider<DeviceUserReader> deviceUserReader) {
        this.deviceUserReader = deviceUserReader;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object deviceId = webRequest.getAttribute(DeviceId.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        DeviceUserReader reader = deviceUserReader.getIfAvailable();
        if (!(deviceId instanceof UUID uuid) || reader == null) {
            throw new BusinessException(CommonErrorCode.USER_NOT_FOUND);
        }
        return reader.findUserIdByDeviceId(uuid)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
    }
}
