package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.CreateCsrRequest;
import dev.morkom.keymanager.dto.CreateCsrResponse;
import dev.morkom.keymanager.util.PemUtils;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

@Service
public class CsrService {

    private final Path privateKeyRootLocation;
    private final PrivateKeyCache privateKeyCache;

    public CsrService(@Value("${keymanager.privatekey.path:./private_keys}") String path, PrivateKeyCache privateKeyCache) throws IOException {
        this.privateKeyRootLocation = Paths.get(path);
        this.privateKeyCache = privateKeyCache;
        Files.createDirectories(this.privateKeyRootLocation);
    }

    public byte[] getPrivateKeyAsBytes(String filename) throws IOException {
        String sanitizedFilename = StringUtils.cleanPath(filename);
        Path file = privateKeyRootLocation.resolve(sanitizedFilename);
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new RuntimeException("Could not read the file: " + filename);
        }
        return Files.readAllBytes(file);
    }

    public CreateCsrResponse createCsr(CreateCsrRequest request) throws Exception {
        KeyPair keyPair = generateKeyPair(request.keyAlgorithm());
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        X500Name subjectDn = createX500Name(request);

        JcaPKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subjectDn, publicKey);
        
        Extensions extensions = generateExtensions(request.extensions());
        if (extensions != null) {
            p10Builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);
        }

        ContentSigner csrContentSigner = new JcaContentSignerBuilder(getSignatureAlgorithm(privateKey)).setProvider("BC").build(privateKey);
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        X509Certificate dummyCert = createDummyCertificate(subjectDn, publicKey, privateKey, request.extensions());

        String filename = "private-key-" + request.commonName().replaceAll("\\s+", "_").toLowerCase() + ".p12";
        Path privateKeyPath = this.privateKeyRootLocation.resolve(filename);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("private-key", privateKey, request.password().toCharArray(), new java.security.cert.Certificate[]{dummyCert});

        try (FileOutputStream fos = new FileOutputStream(privateKeyPath.toFile())) {
            keyStore.store(fos, request.password().toCharArray());
        }

        String csrPem = PemUtils.toPem(csr);
        String downloadUrl = "/api/csr/download/" + filename;
        String cacheId = privateKeyCache.put(privateKey); // Cache the private key

        return new CreateCsrResponse(csrPem, downloadUrl, cacheId);
    }

    public byte[] generateEncryptedPemKey(CreateCsrRequest request) throws Exception {
        KeyPair keyPair = generateKeyPair(request.keyAlgorithm());
        return PemUtils.toEncryptedPem(keyPair.getPrivate(), request.password(), request.pemAlgorithm());
    }

    public byte[] generatePublicKey(CreateCsrRequest request) throws Exception {
        KeyPair keyPair = generateKeyPair(request.keyAlgorithm());
        return PemUtils.toPem(keyPair.getPublic());
    }

    private KeyPair generateKeyPair(String algorithmId) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator;
        if (algorithmId.startsWith("EC-") || algorithmId.startsWith("BP-")) {
            keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            String curveName = switch (algorithmId) {
                case "EC-P256" -> "secp256r1";
                case "EC-P384" -> "secp384r1";
                case "EC-P521" -> "secp521r1";
                case "BP-256R1" -> "brainpoolP256r1";
                case "BP-384R1" -> "brainpoolP384r1";
                case "BP-512R1" -> "brainpoolP512r1";
                default -> "secp384r1";
            };
            keyPairGenerator.initialize(new ECGenParameterSpec(curveName));
        } else if (algorithmId.startsWith("RSA-")) {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            int keySize = switch (algorithmId) {
                case "RSA-2048" -> 2048;
                case "RSA-3072" -> 3072;
                case "RSA-4096" -> 4096;
                default -> 4096;
            };
            keyPairGenerator.initialize(keySize);
        } else {
            // Default fallback just in case
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

    private Extensions generateExtensions(List<String> requestedExtensions) {
        if (requestedExtensions == null || requestedExtensions.isEmpty()) {
            return null;
        }

        ExtensionsGenerator extGen = new ExtensionsGenerator();
        int keyUsageFlags = 0;
        boolean hasKeyUsage = false;
        List<KeyPurposeId> ekuList = new ArrayList<>();

        for (String ext : requestedExtensions) {
            switch (ext.toLowerCase()) {
                case "digitalsignature":
                    keyUsageFlags |= KeyUsage.digitalSignature;
                    hasKeyUsage = true;
                    break;
                case "nonrepudiation":
                    keyUsageFlags |= KeyUsage.nonRepudiation;
                    hasKeyUsage = true;
                    break;
                case "keyencipherment":
                    keyUsageFlags |= KeyUsage.keyEncipherment;
                    hasKeyUsage = true;
                    break;
                case "dataencipherment":
                    keyUsageFlags |= KeyUsage.dataEncipherment;
                    hasKeyUsage = true;
                    break;
                case "keyagreement":
                    keyUsageFlags |= KeyUsage.keyAgreement;
                    hasKeyUsage = true;
                    break;
                case "keycertsign":
                    keyUsageFlags |= KeyUsage.keyCertSign;
                    hasKeyUsage = true;
                    break;
                case "crlsign":
                    keyUsageFlags |= KeyUsage.cRLSign;
                    hasKeyUsage = true;
                    break;
                case "encipheronly":
                    keyUsageFlags |= KeyUsage.encipherOnly;
                    hasKeyUsage = true;
                    break;
                case "decipheronly":
                    keyUsageFlags |= KeyUsage.decipherOnly;
                    hasKeyUsage = true;
                    break;
                case "serverauth":
                    ekuList.add(KeyPurposeId.id_kp_serverAuth);
                    break;
                case "clientauth":
                    ekuList.add(KeyPurposeId.id_kp_clientAuth);
                    break;
                case "codesigning":
                    ekuList.add(KeyPurposeId.id_kp_codeSigning);
                    break;
                case "emailprotection":
                    ekuList.add(KeyPurposeId.id_kp_emailProtection);
                    break;
                case "timestamping":
                    ekuList.add(KeyPurposeId.id_kp_timeStamping);
                    break;
                case "ocspsigning":
                    ekuList.add(KeyPurposeId.id_kp_OCSPSigning);
                    break;
            }
        }

        try {
            if (hasKeyUsage) {
                extGen.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsageFlags));
            }
            if (!ekuList.isEmpty()) {
                extGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(ekuList.toArray(new KeyPurposeId[0])));
            }
            
            if (!hasKeyUsage && ekuList.isEmpty()) {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate extensions", e);
        }

        return extGen.generate();
    }

    private X509Certificate createDummyCertificate(X500Name subjectDn, PublicKey publicKey, PrivateKey privateKey, List<String> requestedExtensions) throws Exception {
        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(128, new SecureRandom());
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(subjectDn, serial, notBefore, notAfter, subjectDn, publicKey);
        
        Extensions extensions = generateExtensions(requestedExtensions);
        if (extensions != null) {
            extensions.oids().asIterator();
            for (Enumeration e = extensions.oids(); e.hasMoreElements(); ) {
                Extension ext = extensions.getExtension((org.bouncycastle.asn1.ASN1ObjectIdentifier) e.nextElement());
                certBuilder.addExtension(ext);
            }
        }

        ContentSigner certContentSigner = new JcaContentSignerBuilder(getSignatureAlgorithm(privateKey)).setProvider("BC").build(privateKey);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(certContentSigner));
    }
}