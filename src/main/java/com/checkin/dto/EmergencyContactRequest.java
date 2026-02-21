package com.checkin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmergencyContactRequest {
    @NotBlank
    @Size(min = 6, max = 20)
    @Pattern(regexp = "^[0-9+\\-()\\s]+$", message = "mobileNumber has invalid characters")
    private String mobileNumber;

    @NotBlank
    @Email
    private String email;

    @Size(max = 50)
    private String label;

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
