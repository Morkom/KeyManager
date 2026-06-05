package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.CreateCaRequest;
import dev.morkom.keymanager.dto.CreateCaResponse;
import dev.morkom.keymanager.dto.SignCertificateRequest;
import dev.morkom.keymanager.dto.SignCertificateResponse;
import dev.morkom.keymanager.util.PemUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CaService {

    private final Path keystoreRootLocation;
    private final PrivateKeyCache privateKeyCache;

    public CaService(@Value("${keymanager.keystore.path:./keystores}") String path, PrivateKeyCache privateKeyCache) throws IOException {
        this.privateKeyCache = privateKeyCache;
        Security.addProvider(new BouncyCastleProvider());
        this.keystoreRootLocation = Paths.get(path);
        Files.createDirectories(this.keystoreRootLocation);
    }

    public byte[] getKeystoreAsBytes(String filename) throws IOException {
        String sanitizedFilename = StringUtils.cleanPath(filename);
        Path file = keystoreRootLocation.resolve(sanitizedFilename);
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new RuntimeException("Could not read the file: " + filename);
        }
        return Files.readAllBytes(file);
    }

    public byte[] getPublicCertificateAsPem(SignCertificateRequest request) throws Exception {
        String sanitizedFilename = StringUtils.cleanPath(request.caFilename());
        Path file = keystoreRootLocation.resolve(sanitizedFilename);
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new RuntimeException("Could not read the file: " + request.caFilename());
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(file.toFile())) {
            keyStore.load(is, request.caPassword().toCharArray());
        }
        
        String alias = keyStore.aliases().nextElement();
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
        return PemUtils.toPem(certificate).getBytes();
    }

    public CreateCaResponse createRootCa(CreateCaRequest request) throws Exception {

        KeyPair keyPair = generateKeyPair(request.keyAlgorithm());
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        X500Name subjectDn = new X500Name("CN=" + request.commonName() +
                ", O=" + request.organization() +
                ", OU=" + request.organizationalUnit() +
                ", C=" + request.country());

        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(request.validityDays(), ChronoUnit.DAYS));

        BigInteger serial = new BigInteger(128, new SecureRandom());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subjectDn, serial, notBefore, notAfter, subjectDn, publicKey);

        Extensions extensions = generateExtensions(request.extensions(), publicKey, null);
        if (extensions != null) {
            for (ASN1ObjectIdentifier oid : extensions.getExtensionOIDs()) {
                certBuilder.addExtension(extensions.getExtension(oid));
            }
        }

        String signatureAlgorithm = getSignatureAlgorithm(privateKey, request.keyAlgorithm());
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).setProvider("BC").build(privateKey);
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(contentSigner));

        String filename = "root-ca-" + request.commonName().replaceAll("\\s+", "_").toLowerCase() + ".p12";
        Path keystorePath = this.keystoreRootLocation.resolve(filename);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("ca-key", privateKey, request.password().toCharArray(), new java.security.cert.Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream(keystorePath.toFile())) {
            keyStore.store(fos, request.password().toCharArray());
        }

        String certPem = PemUtils.toPem(certificate);
        String downloadUrl = "/api/ca/download/" + filename;

        return new CreateCaResponse(certPem, downloadUrl);
    }

    public void storeCaKeystore(MultipartFile file) throws IOException {
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (file.isEmpty() || filename.contains("..")) {
            throw new IOException("Failed to store empty or invalid file " + filename);
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, this.keystoreRootLocation.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }


    public SignCertificateResponse signCertificate(SignCertificateRequest request) throws Exception {
        CaMaterial ca = loadCaMaterial(request);
        X509Certificate signedCertificate = createSignedCertificate(
                request,
                ca.privateKey(),
                ca.certificate()
        );
        return new SignCertificateResponse(PemUtils.toPem(signedCertificate));
    }

    public byte[] signAndCreateP12(SignCertificateRequest request) throws Exception {
        CaMaterial ca = loadCaMaterial(request);

        X509Certificate signedCertificate = createSignedCertificate(
                request,
                ca.privateKey(),
                ca.certificate()
        );

        PrivateKey privateKey = privateKeyCache.get(request.privateKeyCacheId());
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key not found in cache or expired.");
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        if (!verifyKeyPair(privateKey, signedCertificate.getPublicKey())) {
            throw new IllegalStateException("Private key does not match signed certificate public key.");
        }

        Certificate[] newChain = new Certificate[ca.chain().length + 1];
        newChain[0] = signedCertificate;
        System.arraycopy(ca.chain(), 0, newChain, 1, ca.chain().length);

        signedCertificate.verify(ca.certificate().getPublicKey(), "BC");
        keyStore.setKeyEntry(
                "user-key",
                privateKey,
                request.caPassword().toCharArray(),
                newChain
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyStore.store(baos, request.caPassword().toCharArray());

        privateKeyCache.remove(request.privateKeyCacheId());
        return baos.toByteArray();
    }


    private X509Certificate createSignedCertificate(SignCertificateRequest request,
                                                    PrivateKey caPrivateKey,
                                                    X509Certificate caCertificate) throws Exception {
        PKCS10CertificationRequest csr = PemUtils.csrFromPem(request.csrPem());

        X500Name issuer = X500Name.getInstance(caCertificate.getSubjectX500Principal().getEncoded());
        X500Name subject = csr.getSubject();
        PublicKey subjectPublicKey = PemUtils.getPublicKeyFromCsr(csr);

        Instant now = Instant.now();
        Date notBefore = Date.from(now.minus(5, ChronoUnit.MINUTES));
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(128, new SecureRandom()).abs();

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                subjectPublicKey
        );

        Extensions extensions = generateExtensions(request.extensions(), subjectPublicKey, caCertificate.getPublicKey());
        if (extensions != null) {
            for (ASN1ObjectIdentifier oid : extensions.getExtensionOIDs()) {
                certBuilder.addExtension(extensions.getExtension(oid));
            }
        }

        String signatureAlgorithm = getSignatureAlgorithm(caPrivateKey, caCertificate.getPublicKey().getAlgorithm());
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider("BC")
                .build(caPrivateKey);

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(contentSigner));
    }

    public List<String> listCaKeystores() throws IOException {
        try (Stream<Path> stream = Files.list(this.keystoreRootLocation)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".p12"))
                    .collect(Collectors.toList());
        }
    }

    public void deleteCaKeystore(String filename) throws IOException {
        String sanitizedFilename = StringUtils.cleanPath(filename);
        Path file = this.keystoreRootLocation.resolve(sanitizedFilename);
        if (!file.startsWith(this.keystoreRootLocation)) {
            throw new SecurityException("Access denied: Invalid path");
        }
        if (Files.exists(file)) {
            Files.delete(file);
        }
    }

    private record CaMaterial(String alias, PrivateKey privateKey, X509Certificate certificate, Certificate[] chain) {}

    private CaMaterial loadCaMaterial(SignCertificateRequest request) throws Exception {
        Path caKeystorePath = keystoreRootLocation.resolve(request.caFilename());
        KeyStore caKeyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(caKeystorePath.toFile())) {
            caKeyStore.load(fis, request.caPassword().toCharArray());
        }

        String caAlias = null;
        Enumeration<String> aliases = caKeyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (caKeyStore.isKeyEntry(alias)) {
                caAlias = alias;
                break;
            }
        }
        if (caAlias == null) {
            throw new KeyStoreException("No private key entry found in the CA keystore.");
        }

        PrivateKey caPrivateKey = (PrivateKey) caKeyStore.getKey(caAlias, request.caPassword().toCharArray());
        X509Certificate caCertificate = (X509Certificate) caKeyStore.getCertificate(caAlias);
        Certificate[] caChain = caKeyStore.getCertificateChain(caAlias);
        if (caChain == null || caChain.length == 0) {
            caChain = new Certificate[] { caCertificate };
        }

        return new CaMaterial(caAlias, caPrivateKey, caCertificate, caChain);
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
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(4096);
        }
        return keyPairGenerator.generateKeyPair();
    }

    private String getSignatureAlgorithm(PrivateKey privateKey, String keyAlgorithm) {
        if ("EC".equals(privateKey.getAlgorithm())) {
            return "SHA384withECDSA";
        }
        return "SHA256withRSA";
    }

    private boolean verifyKeyPair(PrivateKey privateKey, PublicKey publicKey) {
        try {
            String keyAlgorithm = publicKey.getAlgorithm();
            String signatureAlgorithm;
            if ("RSA".equals(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withRSA";
            } else if ("EC".equals(keyAlgorithm)) {
                signatureAlgorithm = "SHA256withECDSA";
            } else {
                return false; // Unsupported algorithm
            }

            Signature sig = Signature.getInstance(signatureAlgorithm, "BC");
            sig.initSign(privateKey);
            byte[] data = "verification".getBytes();
            sig.update(data);
            byte[] signature = sig.sign();

            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private Extensions generateExtensions(List<String> requestedExtensions, PublicKey subjectPublicKey, PublicKey caPublicKey) throws IOException {
        if (requestedExtensions == null || requestedExtensions.isEmpty()) {
            return null;
        }

        ExtensionsGenerator extGen = new ExtensionsGenerator();
        int keyUsageFlags = 0;
        boolean hasKeyUsage = false;
        List<KeyPurposeId> ekuList = new ArrayList<>();
        JcaX509ExtensionUtils extUtils;
        try {
            extUtils = new JcaX509ExtensionUtils();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        for (String ext : requestedExtensions) {
            switch (ext.toLowerCase()) {
                case "subjectkeyidentifier":
                    if (subjectPublicKey != null) {
                        extGen.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(subjectPublicKey));
                    }
                    break;
                case "authoritykeyidentifier":
                    if (caPublicKey != null) {
                        extGen.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caPublicKey));
                    } else if (subjectPublicKey != null) { // For self-signed root CA
                         extGen.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(subjectPublicKey));
                    }
                    break;
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

        if (hasKeyUsage) {
            extGen.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsageFlags));
        }
        if (!ekuList.isEmpty()) {
            extGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(ekuList.toArray(new KeyPurposeId[0])));
        }
        
        return extGen.isEmpty() ? null : extGen.generate();
    }
}