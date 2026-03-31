package com.checkin.config;

import com.checkin.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Authorization"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Actuator health for load balancer / container probes (no auth required)
						.requestMatchers("/actuator/**", "/checkin/actuator/**").permitAll()
						.requestMatchers(HttpMethod.POST,
								"/api/user/register",
								"/api/user/login",
								"/api/user/refresh",
								"/api/user/forgot-password",
								"/api/user/reset-password",
								"/api/user/resend-verification-email",
								"/api/login/login",
								"/checkin/api/user/register",
								"/checkin/api/user/login",
								"/checkin/api/user/refresh",
								"/checkin/api/user/forgot-password",
								"/checkin/api/user/reset-password",
								"/checkin/api/user/resend-verification-email",
								"/checkin/api/login/login")
						.permitAll()
						.requestMatchers(HttpMethod.GET,
								"/api/user/verify-email/**",
								"/api/emergency-contacts/verify/**", "/api/emergency-contacts/opt-out/**",
								"/checkin/api/user/verify-email/**",
								"/checkin/api/emergency-contacts/verify/**", "/checkin/api/emergency-contacts/opt-out/**")
						.permitAll()
						.requestMatchers("/error", "/checkin/error").permitAll()
						.requestMatchers("/api/admin/**", "/checkin/api/admin/**").hasRole("SUPER_USER")
						.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
