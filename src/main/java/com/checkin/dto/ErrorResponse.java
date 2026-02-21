package com.checkin.dto;

/**
 * Standard error response returned to API callers.
 * Avoids exposing technical details; uses user-friendly messages.
 */
public record ErrorResponse(String message) {
}
