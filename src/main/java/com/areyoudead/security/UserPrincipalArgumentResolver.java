package com.areyoudead.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves controller method parameters of type {@link UserPrincipal} that are
 * annotated with {@link CurrentUser}.
 *
 * <p>
 * Spring Security has already validated the JWT and populated the
 * {@link SecurityContextHolder} by the time a controller method is invoked, so
 * this resolver simply extracts and casts the principal — it does not perform
 * any additional authentication.
 */
public class UserPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UserPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public UserPrincipal resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException(
                    "No authenticated UserPrincipal found in SecurityContext. "
                            + "Ensure the endpoint is secured and the JWT filter has run.");
        }
        return principal;
    }
}
