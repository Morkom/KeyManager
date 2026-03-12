package dev.morkom.keymanager.dto;

/**
 * Represents a single entry within a keystore.
 * @param alias The alias of the entry.
 * @param entryType The type of the entry ("Key Pair" or "Trusted Certificate").
 * @param certificateDetails A summary of the certificate details.
 */
public record KeystoreEntry(
    String alias,
    String entryType,
    String certificateDetails
) {}
