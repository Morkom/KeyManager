package dev.morkom.keymanager.dto;

public record CreateCsrResponse(
    String csrPem,
    String privateKeyDownloadUrl,
    String privateKeyCacheId // New field for caching private key
) {}
