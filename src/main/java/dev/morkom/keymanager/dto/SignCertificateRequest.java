package dev.morkom.keymanager.dto;

import java.util.List;

public record SignCertificateRequest(
    String csrPem,
    String caFilename,
    String caPassword,
    String privateKeyCacheId,
    List<String> extensions
) {}
