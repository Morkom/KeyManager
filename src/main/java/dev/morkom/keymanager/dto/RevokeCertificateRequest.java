package dev.morkom.keymanager.dto;

public record RevokeCertificateRequest(
    String caFilename,
    String caPassword,
    String serialNumber,
    int revocationReason
) {}
