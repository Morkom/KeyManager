package dev.morkom.keymanager.dto;

public record GenerateSshKeyRequest(
    String keyAlgorithm,
    String comment
) {}
