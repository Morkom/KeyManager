package dev.morkom.keymanager.dto;

public record BcryptVerifyRequest(
    String password,
    String hash
) {}
