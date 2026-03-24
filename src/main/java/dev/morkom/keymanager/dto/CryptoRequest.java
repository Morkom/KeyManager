package dev.morkom.keymanager.dto;

public record CryptoRequest(
    String data, // Base64 encoded data
    String password
) {}
