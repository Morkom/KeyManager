package dev.morkom.keymanager.dto;

public record CreateCaRequest(
    String commonName,
    String organization,
    String organizationalUnit,
    String country,
    int validityDays,
    String keyAlgorithm,
    String password
) {}
