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

    public CsrService(@Value("${keymanager.privatekey.path:./private_keys}") String path) throws IOException {
        this.privateKeyRootLocation = Paths.get(path);
        Files.createDirectories(this.privateKeyRootLocation);
    }

    public CreateCsrResponse createCsr(CreateCsrRequest request) throws Exception {
        // 1. Generate Key Pair based on algorithm
        KeyPairGenerator keyPairGenerator;
        String signatureAlgorithm;
        if ("EC".equalsIgnoreCase(request.keyAlgorithm())) {
            keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new ECGenParameterSpec("secp384r1"));
            signatureAlgorithm = "SHA384withECDSA";
        } else {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(4096);
            signatureAlgorithm = "SHA256WithRSA";
        }
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // 2. Build DN
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, request.commonName());
        nameBuilder.addRDN(BCStyle.O, request.organization());
        nameBuilder.addRDN(BCStyle.OU, request.organizationalUnit());
        nameBuilder.addRDN(BCStyle.L, request.locality());
        nameBuilder.addRDN(BCStyle.ST, request.state());
        nameBuilder.addRDN(BCStyle.C, request.country());
        X500Name subjectDn = nameBuilder.build();

        // 3. Create CSR
        JcaPKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subjectDn, publicKey);
        ContentSigner csrContentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        // 4. Create a temporary self-signed certificate
        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(128, new SecureRandom());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subjectDn, serial, notBefore, notAfter, subjectDn, publicKey);

        ContentSigner certContentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);
        X509Certificate dummyCert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(certContentSigner));

        // 5. Store the private key and dummy certificate in a PKCS#12 file
        String filename = "private-key-" + request.commonName().replaceAll("\\s+", "_").toLowerCase() + ".p12";
        Path privateKeyPath = this.privateKeyRootLocation.resolve(filename);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("private-key", privateKey, request.password().toCharArray(), new java.security.cert.Certificate[]{dummyCert});

        try (FileOutputStream fos = new FileOutputStream(privateKeyPath.toFile())) {
            keyStore.store(fos, request.password().toCharArray());
        }

        // 6. Prepare response
        String csrPem = PemUtils.toPem(csr);
        String downloadUrl = "/api/csr/download/" + filename;

        return new CreateCsrResponse(csrPem, downloadUrl);
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
}
