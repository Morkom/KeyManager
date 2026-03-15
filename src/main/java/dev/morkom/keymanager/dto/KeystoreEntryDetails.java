package dev.morkom.keymanager.dto;

// Converted from a record to a class for robust JSON serialization.
public class KeystoreEntryDetails {
    private final String alias;
    private final String entryType;
    private final int certificateChainLength;
    private final String algorithm;
    private final String certificateSummary;
    private final boolean isExpired;

    public KeystoreEntryDetails(String alias, String entryType, int certificateChainLength, String algorithm, String certificateSummary, boolean isExpired) {
        this.alias = alias;
        this.entryType = entryType;
        this.certificateChainLength = certificateChainLength;
        this.algorithm = algorithm;
        this.certificateSummary = certificateSummary;
        this.isExpired = isExpired;
    }

    // Standard getters are required for Jackson to serialize the fields.
    public String getAlias() { return alias; }
    public String getEntryType() { return entryType; }
    public int getCertificateChainLength() { return certificateChainLength; }
    public String getAlgorithm() { return algorithm; }
    public String getCertificateSummary() { return certificateSummary; }
    public boolean isExpired() { return isExpired; }
}
