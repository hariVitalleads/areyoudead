package com.checkin.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limits auth-related endpoints (login, register, forgot-password, resend-verification, refresh)
 * per client IP. Returns 429 Too Many Requests when limit exceeded.
 */
@Component
@Order(-100)
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimitProperties props;
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	public RateLimitFilter(RateLimitProperties props) {
		this.props = props;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		if (!isAuthPath(path)) {
			chain.doFilter(request, response);
			return;
		}

		String clientKey = clientKey(request);
		Bucket bucket = buckets.computeIfAbsent(clientKey, k -> createBucket());

		if (bucket.tryConsume(1)) {
			chain.doFilter(request, response);
		} else {
			response.setStatus(429);
			response.setContentType("application/json");
			response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
		}
	}

	private boolean isAuthPath(String path) {
		if (path == null) {
			return false;
		}
		return path.startsWith("/api/login/login")
				|| path.startsWith("/checkin/api/login/login")
				|| path.startsWith("/api/user/register")
				|| path.startsWith("/checkin/api/user/register")
				|| path.startsWith("/api/user/login")
				|| path.startsWith("/checkin/api/user/login")
				|| path.startsWith("/api/user/refresh")
				|| path.startsWith("/checkin/api/user/refresh")
				|| path.startsWith("/api/user/forgot-password")
				|| path.startsWith("/checkin/api/user/forgot-password")
				|| path.startsWith("/api/user/reset-password")
				|| path.startsWith("/checkin/api/user/reset-password")
				|| path.startsWith("/api/user/resend-verification-email")
				|| path.startsWith("/checkin/api/user/resend-verification-email");
	}

	private String clientKey(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isEmpty()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
	}

	private Bucket createBucket() {
		Bandwidth limit = Bandwidth.classic(props.getAuthRequestsPerWindow(),
				Refill.greedy(props.getAuthRequestsPerWindow(), Duration.ofSeconds(props.getWindowSeconds())));
		return Bucket.builder().addLimit(limit).build();
	}
}
