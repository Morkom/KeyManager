package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.CreateCaRequest;
import dev.morkom.keymanager.dto.CreateCaResponse;
import dev.morkom.keymanager.dto.SignCertificateRequest;
import dev.morkom.keymanager.dto.SignCertificateResponse;
import dev.morkom.keymanager.util.PemUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CaService {

    private final Path keystoreRootLocation;

    public CaService(@Value("${keymanager.keystore.path:./keystores}") String path) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        this.keystoreRootLocation = Paths.get(path);
        Files.createDirectories(this.keystoreRootLocation);
    }

    public CreateCaResponse createRootCa(CreateCaRequest request) throws Exception {
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
        X500Name subjectDn = new X500Name("CN=" + request.commonName() + ", O=" + request.organization() + ", OU=" + request.organizationalUnit() + ", C=" + request.country());

        // 3. Create Certificate
        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(request.validityDays(), ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(128, new SecureRandom());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subjectDn, serial, notBefore, notAfter, subjectDn, publicKey);

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

        // 4. Sign the certificate
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(contentSigner));

        // 5. Store in Keystore
        String filename = "root-ca-" + request.commonName().replaceAll("\\s+", "_").toLowerCase() + ".p12";
        Path keystorePath = this.keystoreRootLocation.resolve(filename);
        
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("ca-key", privateKey, request.password().toCharArray(), new java.security.cert.Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream(keystorePath.toFile())) {
            keyStore.store(fos, request.password().toCharArray());
        }

        // 6. Prepare response
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
        Path caKeystorePath = keystoreRootLocation.resolve(request.caFilename());
        KeyStore caKeyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(caKeystorePath.toFile())) {
            caKeyStore.load(fis, request.caPassword().toCharArray());
        }

        String alias = caKeyStore.aliases().nextElement();
        PrivateKey caPrivateKey = (PrivateKey) caKeyStore.getKey(alias, request.caPassword().toCharArray());
        X509Certificate caCertificate = (X509Certificate) caKeyStore.getCertificate(alias);

        // Determine signature algorithm based on CA's key type
        String signatureAlgorithm;
        if (caPrivateKey.getAlgorithm().equals("EC")) {
            signatureAlgorithm = "SHA384withECDSA";
        } else {
            signatureAlgorithm = "SHA256WithRSA";
        }

        PKCS10CertificationRequest csr = PemUtils.csrFromPem(request.csrPem());

        X500Name issuer = new X500Name(caCertificate.getSubjectX500Principal().getName());
        X500Name subject = csr.getSubject();
        PublicKey subjectPublicKey = PemUtils.getPublicKeyFromCsr(csr);

        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(128, new SecureRandom());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, subjectPublicKey);

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(caPrivateKey);
        X509Certificate signedCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(contentSigner));

        String signedCertPem = PemUtils.toPem(signedCertificate);
        return new SignCertificateResponse(signedCertPem);
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
    
    public Resource loadKeystoreAsResource(String filename) throws MalformedURLException {
        String sanitizedFilename = StringUtils.cleanPath(filename);
        Path file = keystoreRootLocation.resolve(sanitizedFilename);
        Resource resource = new UrlResource(file.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read the file: " + filename);
        }
    }
}
