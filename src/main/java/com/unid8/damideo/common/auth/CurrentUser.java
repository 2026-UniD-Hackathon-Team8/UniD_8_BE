package com.unid8.damideo.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 X-Device-Id로 찾은 사용자 id(Long)를 넣어준다.
 *
 * <pre>
 * public ApiResponse&lt;ProfileResponse&gt; profile(@CurrentUser Long userId)
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
