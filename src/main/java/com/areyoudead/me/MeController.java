package com.areyoudead.me;

import com.areyoudead.security.UserPrincipal;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {
	@GetMapping("/me")
	public Map<String, Object> me(Authentication authentication) {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		return Map.of(
			"id", principal.getUserId(),
			"email", principal.getEmail()
		);
	}
}

