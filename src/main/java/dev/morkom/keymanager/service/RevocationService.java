package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.GenerateCrlRequest;
import dev.morkom.keymanager.dto.RevokeCertificateRequest;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RevocationService {

    private final Path keystoreRootLocation;
    private final Path crlDbLocation;

    public RevocationService(@Value("${keymanager.keystore.path:./keystores}") String keystorePath,
                             @Value("${keymanager.crldb.path:./crl_db}") String crlDbPath) throws IOException {
        this.keystoreRootLocation = Paths.get(keystorePath);
        this.crlDbLocation = Paths.get(crlDbPath);
        Files.createDirectories(this.crlDbLocation);
    }

    public void revokeCertificate(RevokeCertificateRequest request) throws Exception {
        Path dbPath = crlDbLocation.resolve(request.caFilename() + ".crl.db");
        Map<String, String> db = loadCrlDb(dbPath);

        String sanitizedSerial = request.serialNumber()
                .replaceAll("0x", "")
                .replaceAll("\\s", "")
                .replaceAll(":", "");

        BigInteger serial = new BigInteger(sanitizedSerial, 16);
        db.put(serial.toString(), new Date().getTime() + ":" + request.revocationReason());

        saveCrlDb(dbPath, db);
    }

    public Resource generateCrl(GenerateCrlRequest request) throws Exception {
        // 1. Load CA Keystore
        Path caKeystorePath = keystoreRootLocation.resolve(request.caFilename());
        KeyStore caKeyStore = KeyStore.getInstance("PKCS12");
        char[] password = request.caPassword().toCharArray();
        try (FileInputStream fis = new FileInputStream(caKeystorePath.toFile())) {
            caKeyStore.load(fis, password);
        } catch (Exception e) {
            throw new IOException("Could not load CA keystore. Is the password correct?", e);
        }

        String alias = caKeyStore.aliases().nextElement();
        PrivateKey caPrivateKey = (PrivateKey) caKeyStore.getKey(alias, password);
        X509Certificate caCertificate = (X509Certificate) caKeyStore.getCertificate(alias);

        // Determine signature algorithm based on CA's key type
        String signatureAlgorithm;
        if (caPrivateKey.getAlgorithm().equals("EC")) {
            signatureAlgorithm = "SHA384withECDSA";
        } else {
            signatureAlgorithm = "SHA256WithRSA";
        }

        // 2. Create CRL Builder
        JcaX509v2CRLBuilder crlBuilder = new JcaX509v2CRLBuilder(caCertificate.getSubjectX500Principal(), new Date());
        crlBuilder.setNextUpdate(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));

        // 3. Add revoked certificates from our DB
        Path dbPath = crlDbLocation.resolve(request.caFilename() + ".crl.db");
        Map<String, String> db = loadCrlDb(dbPath);
        for (Map.Entry<String, String> entry : db.entrySet()) {
            BigInteger serial = new BigInteger(entry.getKey());
            String[] parts = entry.getValue().split(":");
            Date revocationDate = new Date(Long.parseLong(parts[0]));
            int reason = Integer.parseInt(parts[1]);
            crlBuilder.addCRLEntry(serial, revocationDate, reason);
        }

        // 4. Sign the CRL
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(caPrivateKey);
        X509CRL crl = new JcaX509CRLConverter().getCRL(crlBuilder.build(contentSigner));

        return new ByteArrayResource(crl.getEncoded());
    }

    private Map<String, String> loadCrlDb(Path dbPath) throws IOException {
        Map<String, String> db = new HashMap<>();
        if (Files.exists(dbPath)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(dbPath.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        db.put(parts[0], parts[1]);
                    }
                }
            }
        }
        return db;
    }

    private void saveCrlDb(Path dbPath, Map<String, String> db) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbPath.toFile()))) {
            for (Map.Entry<String, String> entry : db.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        }
    }
}
