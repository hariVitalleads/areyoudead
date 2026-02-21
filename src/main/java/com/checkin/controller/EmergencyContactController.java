package com.checkin.controller;

import com.checkin.dto.EmergencyContactRequest;
import com.checkin.dto.EmergencyContactResponse;
import com.checkin.dto.MessageResponse;
import com.checkin.security.CurrentUser;
import com.checkin.security.UserPrincipal;
import com.checkin.service.EmergencyContactService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergency-contacts")
public class EmergencyContactController {
    private final EmergencyContactService emergencyContactService;

    public EmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    @GetMapping
    public List<EmergencyContactResponse> getContacts(@CurrentUser UserPrincipal principal) {
        return emergencyContactService.getContacts(principal.getUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyContactResponse addContact(@CurrentUser UserPrincipal principal,
            @Valid @RequestBody EmergencyContactRequest req) {
        return emergencyContactService.addContact(principal.getUserId(), req);
    }

    @PutMapping("/{id}")
    public EmergencyContactResponse updateContact(@CurrentUser UserPrincipal principal,
            @PathVariable UUID id, @Valid @RequestBody EmergencyContactRequest req) {
        return emergencyContactService.updateContact(principal.getUserId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContact(@CurrentUser UserPrincipal principal, @PathVariable UUID id) {
        emergencyContactService.deleteContact(principal.getUserId(), id);
    }

    /**
     * Public endpoint: verify contact email. Called when contact clicks verification link.
     */
    @GetMapping("/verify/{token}")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse verify(@PathVariable String token) {
        emergencyContactService.verifyByToken(token);
        return new MessageResponse("Your email has been verified. You will receive inactivity alerts for this contact.");
    }

    /**
     * Public endpoint: opt out from receiving future inactivity alerts.
     * Called when emergency contact clicks the opt-out link in an alert email.
     */
    @GetMapping("/opt-out/{token}")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse optOut(@PathVariable UUID token) {
        emergencyContactService.optOutByToken(token);
        return new MessageResponse("You have been opted out of future alerts.");
    }
}
