package dev.morkom.keymanager.dto;

public record XmlValidationResponse(
    boolean valid,
    String message
) {}
