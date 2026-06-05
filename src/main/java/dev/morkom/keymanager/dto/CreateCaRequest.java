package dev.morkom.keymanager.dto;

import java.util.List;

public record CreateCaRequest(
    String commonName,
    String organization,
    String organizationalUnit,
    String country,
    int validityDays,
    String keyAlgorithm,
    String password,
    List<String> extensions
) {}
