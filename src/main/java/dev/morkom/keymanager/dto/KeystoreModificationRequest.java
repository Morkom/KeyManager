package dev.morkom.keymanager.dto;

import java.util.List;

/**
 * Represents a set of modifications to be applied to a keystore.
 * @param password The password for the keystore.
 * @param modifications A list of modifications to perform.
 */
public record KeystoreModificationRequest(
    String password,
    List<Modification> modifications
) {
    public record Modification(
        String type, // "DELETE", "RENAME"
        String alias,
        String newAlias // Only for RENAME
    ) {}
}
