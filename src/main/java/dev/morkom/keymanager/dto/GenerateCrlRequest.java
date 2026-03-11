package dev.morkom.keymanager.dto;

public record GenerateCrlRequest(
    String caFilename,
    String caPassword
) {}
