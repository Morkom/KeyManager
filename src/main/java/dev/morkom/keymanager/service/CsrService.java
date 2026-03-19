package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.CreateCsrRequest;
import dev.morkom.keymanager.dto.CreateCsrResponse;
import dev.morkom.keymanager.util.PemUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class CsrService {

    private final Path privateKeyRootLocation;
    private final AuditEventService auditEventService;

    public CsrService(@Value("${keymanager.privatekey.path:./private_keys}") String path, AuditEventService auditEventService) throws IOException {
        this.privateKeyRootLocation = Paths.get(path);
        this.auditEventService = auditEventService;
        Files.createDirectories(this.privateKeyRootLocation);
    }

    public CreateCsrResponse createCsr(CreateCsrRequest request) throws Exception {
        try {
            KeyPair keyPair = generateKeyPair(request.keyAlgorithm());
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            X500Name subjectDn = createX500Name(request);

            JcaPKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subjectDn, publicKey);
            ContentSigner csrContentSigner = new JcaContentSignerBuilder(getSignatureAlgorithm(privateKey)).build(privateKey);
            PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

            X509Certificate dummyCert = createDummyCertificate(subjectDn, publicKey, privateKey);

            String filename = "private-key-" + request.commonName().replaceAll("\\s+", "_").toLowerCase() + ".p12";
            Path privateKeyPath = this.privateKeyRootLocation.resolve(filename);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("private-key", privateKey, request.password().toCharArray(), new java.security.cert.Certificate[]{dummyCert});

            try (FileOutputStream fos = new FileOutputStream(privateKeyPath.toFile())) {
                keyStore.store(fos, request.password().toCharArray());
            }

            auditEventService.logEvent("CREATE_CSR", "Successfully created CSR for: " + request.commonName(), true);
            String csrPem = PemUtils.toPem(csr);
            String downloadUrl = "/api/csr/download/" + filename;
            return new CreateCsrResponse(csrPem, downloadUrl);
        } catch (Exception e) {
            auditEventService.logEvent("CREATE_CSR", "Failed to create CSR for: " + request.commonName() + " - " + e.getMessage(), false);
            throw e;
        }
    }

    public Resource generateEncryptedPemKey(CreateCsrRequest request) throws Exception {
        try {
            KeyPair keyPair = generateKeyPair(request.keyAlgorithm());
            byte[] pemBytes = PemUtils.toEncryptedPem(keyPair.getPrivate(), request.password(), request.pemAlgorithm());
            auditEventService.logEvent("GENERATE_ENCRYPTED_PEM", "Successfully generated Encrypted PEM for: " + request.commonName(), true);
            return new ByteArrayResource(pemBytes);
        } catch (Exception e) {
            auditEventService.logEvent("GENERATE_ENCRYPTED_PEM", "Failed to generate Encrypted PEM for: " + request.commonName() + " - " + e.getMessage(), false);
            throw e;
        }
    }

    public Resource loadPrivateKeyAsResource(String filename) throws MalformedURLException {
        String sanitizedFilename = StringUtils.cleanPath(filename);
        Path file = privateKeyRootLocation.resolve(sanitizedFilename);
        Resource resource = new UrlResource(file.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read the file: " + filename);
        }
    }

    private KeyPair generateKeyPair(String algorithm) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator;
        if ("EC".equalsIgnoreCase(algorithm)) {
            keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new ECGenParameterSpec("secp384r1"));
        } else {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(4096);
        }
        return keyPairGenerator.generateKeyPair();
    }

    private X500Name createX500Name(CreateCsrRequest request) {
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, request.commonName());
        nameBuilder.addRDN(BCStyle.O, request.organization());
        nameBuilder.addRDN(BCStyle.OU, request.organizationalUnit());
        nameBuilder.addRDN(BCStyle.L, request.locality());
        nameBuilder.addRDN(BCStyle.ST, request.state());
        nameBuilder.addRDN(BCStyle.C, request.country());
        return nameBuilder.build();
    }

    private String getSignatureAlgorithm(PrivateKey privateKey) {
        return privateKey.getAlgorithm().equals("EC") ? "SHA384withECDSA" : "SHA256WithRSA";
    }

    private X509Certificate createDummyCertificate(X500Name subjectDn, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(128, new SecureRandom());
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(subjectDn, serial, notBefore, notAfter, subjectDn, publicKey);
        ContentSigner certContentSigner = new JcaContentSignerBuilder(getSignatureAlgorithm(privateKey)).build(privateKey);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(certContentSigner));
    }
}
