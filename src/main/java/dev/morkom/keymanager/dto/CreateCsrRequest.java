package dev.morkom.keymanager.dto;

import java.util.Set;

public record CreateCsrRequest(
    String commonName,
    String organization,
    String organizationalUnit,
    String locality,
    String state,
    String country,
    String keyAlgorithm,
    String password,
    String pemAlgorithm // e.g., "AES_256_CBC"
) {}
