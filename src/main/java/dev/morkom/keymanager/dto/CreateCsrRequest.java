package dev.morkom.keymanager.dto;

public record CreateCsrRequest(
    String commonName,
    String organization,
    String organizationalUnit,
    String locality,
    String state,
    String country,
    String keyAlgorithm,
    String password
) {}
