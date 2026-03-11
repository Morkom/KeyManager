package dev.morkom.keymanager.dto;

public record SignCertificateRequest(
    String csrPem,
    String caFilename,
    String caPassword
) {}
