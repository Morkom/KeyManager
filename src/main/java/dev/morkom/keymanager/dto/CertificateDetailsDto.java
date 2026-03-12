package dev.morkom.keymanager.dto;

import java.util.Map;

/**
 * A detailed representation of an X.509 Certificate.
 * @param subject A map of the Subject's distinguished name components.
 * @param issuer A map of the Issuer's distinguished name components.
 * @param serialNumber The serial number of the certificate.
 * @param version The version of the certificate (e.g., 3).
 * @param validFrom The start date of the certificate's validity.
 * @param validUntil The end date of the certificate's validity.
 * @param signatureAlgorithm The algorithm used to sign the certificate.
 * @param publicKeyAlgorithm The algorithm of the certificate's public key.
 * @param sha256Fingerprint The SHA-256 fingerprint of the certificate.
 * @param sha1Fingerprint The SHA-1 fingerprint of the certificate.
 */
public record CertificateDetailsDto(
    Map<String, String> subject,
    Map<String, String> issuer,
    String serialNumber,
    int version,
    String validFrom,
    String validUntil,
    String signatureAlgorithm,
    String publicKeyAlgorithm,
    String sha256Fingerprint,
    String sha1Fingerprint
) {}
