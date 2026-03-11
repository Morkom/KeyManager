package dev.morkom.keymanager.dto;

public record CreateCaResponse(
    String certificatePem,
    String keystoreDownloadUrl
) {}
