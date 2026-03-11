package dev.morkom.keymanager.dto;

public record CreateCsrResponse(
    String csrPem,
    String privateKeyDownloadUrl
) {}
