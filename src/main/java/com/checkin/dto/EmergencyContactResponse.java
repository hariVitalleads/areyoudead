package com.checkin.dto;

import java.util.UUID;

public class EmergencyContactResponse {
    private final UUID id;
    private final String mobileNumber;
    private final String email;
    private final short contactIndex;
    private final boolean verified;
    private final String label;

    public EmergencyContactResponse(UUID id, String mobileNumber, String email, short contactIndex) {
        this(id, mobileNumber, email, contactIndex, false, null);
    }

    public EmergencyContactResponse(UUID id, String mobileNumber, String email, short contactIndex, boolean verified) {
        this(id, mobileNumber, email, contactIndex, verified, null);
    }

    public EmergencyContactResponse(UUID id, String mobileNumber, String email, short contactIndex, boolean verified, String label) {
        this.id = id;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.contactIndex = contactIndex;
        this.verified = verified;
        this.label = label;
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

    public boolean isVerified() {
        return verified;
    }

    public String getLabel() {
        return label;
    }
}
