package com.checkin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds request ID to MDC for structured logging.
 * User ID is set by JwtAuthenticationFilter when authenticated.
 * Use %X{requestId} and %X{userId} in logback pattern.
 */
@Component
@Order(-200)
public class MdcFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID = "requestId";
	public static final String USER_ID = "userId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = request.getHeader("X-Request-ID");
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		}
		response.setHeader("X-Request-ID", requestId);

		try {
			MDC.put(REQUEST_ID, requestId);
			chain.doFilter(request, response);
		} finally {
			MDC.remove(REQUEST_ID);
			MDC.remove(USER_ID);
		}
	}
}
