package dev.morkom.keymanager.dto;

public record GenerateSshKeyResponse(
    String publicKey,
    String privateKeyPem
) {}
