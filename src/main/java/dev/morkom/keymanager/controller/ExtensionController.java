package dev.morkom.keymanager.controller;

import dev.morkom.keymanager.dto.ExtensionDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/extensions")
public class ExtensionController {

    @GetMapping
    public List<ExtensionDto> getAvailableExtensions() {
        return List.of(
                new ExtensionDto("subjectKeyIdentifier", "Subject Key Identifier", "Identifies the public key being certified."),
                new ExtensionDto("authorityKeyIdentifier", "Authority Key Identifier", "Identifies the public key that signed the certificate."),
                new ExtensionDto("digitalSignature", "Digital Signature", "Ensures data integrity and non-repudiation."),
                new ExtensionDto("nonRepudiation", "Non-Repudiation", "Prevents the denial of an action."),
                new ExtensionDto("keyEncipherment", "Key Encipherment", "Protects keys by encrypting them."),
                new ExtensionDto("dataEncipherment", "Data Encipherment", "For encrypting user data, not keys."),
                new ExtensionDto("keyAgreement", "Key Agreement", "Enables the establishment of a shared secret key."),
                new ExtensionDto("keyCertSign", "Certificate Signing", "Allows this certificate to sign other certificates."),
                new ExtensionDto("crlSign", "CRL Signing", "Allows this certificate to sign Certificate Revocation Lists."),
                new ExtensionDto("encipherOnly", "Encipher Only", "Restricts key usage to data encryption only."),
                new ExtensionDto("decipherOnly", "Decipher Only", "Restricts key usage to data decryption only."),
                new ExtensionDto("serverAuth", "TLS Server Authentication", "Identifies a server in a TLS session."),
                new ExtensionDto("clientAuth", "TLS Client Authentication", "Identifies a client in a TLS session."),
                new ExtensionDto("codeSigning", "Code Signing", "For signing executable code."),
                new ExtensionDto("emailProtection", "Email Protection", "Secures email messages (S/MIME)."),
                new ExtensionDto("timeStamping", "Time Stamping", "Binds a hash of a document to a time."),
                new ExtensionDto("ocspSigning", "OCSP Signing", "For signing OCSP responses.")
        );
    }
}