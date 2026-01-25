package com.areyoudead.dto;

import java.util.UUID;

public class EmergencyContactResponse {
    private final UUID id;
    private final String mobileNumber;
    private final String email;
    private final short contactIndex;

    public EmergencyContactResponse(UUID id, String mobileNumber, String email, short contactIndex) {
        this.id = id;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.contactIndex = contactIndex;
    }

    public UUID getId() {
        return id;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public short getContactIndex() {
        return contactIndex;
    }
}
