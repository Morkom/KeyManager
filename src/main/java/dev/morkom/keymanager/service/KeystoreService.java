package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.CertificateDetailsDto;
import dev.morkom.keymanager.dto.KeystoreEntryDetails;
import dev.morkom.keymanager.dto.KeystoreModificationRequest;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class KeystoreService {

    public List<KeystoreEntryDetails> viewKeystore(MultipartFile file, String password) throws Exception {
        if (file.isEmpty()) {
            return Collections.emptyList();
        }
        KeyStore keyStore = loadKeystore(file, password);
        List<KeystoreEntryDetails> entries = new ArrayList<>();
        List<String> aliases = Collections.list(keyStore.aliases());
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        for (String alias : aliases) {
            String entryType;
            String algorithm;
            int chainLength = 0;

            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain == null) {
                chain = new Certificate[]{keyStore.getCertificate(alias)};
            }
            chainLength = chain.length;

            X509Certificate mainCert = (X509Certificate) chain[0];
            String subject = mainCert.getSubjectX500Principal().getName();
            String expiry = "Expires: " + sdf.format(mainCert.getNotAfter());
            String summary = subject + " (" + expiry + ")";
            boolean isExpired = mainCert.getNotAfter().before(new Date());

            if (keyStore.isKeyEntry(alias)) {
                entryType = "PRIVATE_KEY";
                KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(password.toCharArray());
                KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, protParam);
                algorithm = pkEntry.getPrivateKey().getAlgorithm();
            } else {
                entryType = "TRUSTED_CERTIFICATE";
                algorithm = mainCert.getPublicKey().getAlgorithm();
            }

            entries.add(new KeystoreEntryDetails(alias, entryType, chainLength, algorithm, summary, isExpired));
        }
        return entries;
    }

    public CertificateDetailsDto getCertificateDetails(MultipartFile file, String password, String alias) throws Exception {
        KeyStore keyStore = loadKeystore(file, password);
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
        if (cert == null) throw new IllegalArgumentException("Alias not found");
        return convertToDto(cert);
    }

    public List<CertificateDetailsDto> getCertificateChainDetails(MultipartFile file, String password, String alias) throws Exception {
        KeyStore keyStore = loadKeystore(file, password);
        Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain == null) {
            Certificate cert = keyStore.getCertificate(alias);
            if (cert == null) throw new IllegalArgumentException("Alias not found");
            chain = new Certificate[]{cert};
        }

        List<CertificateDetailsDto> chainDetails = new ArrayList<>();
        for (Certificate cert : chain) {
            if (cert instanceof X509Certificate) {
                chainDetails.add(convertToDto((X509Certificate) cert));
            }
        }
        return chainDetails;
    }

    public Resource applyModifications(MultipartFile file, KeystoreModificationRequest request) throws Exception {
        KeyStore keyStore = loadKeystore(file, request.password());

        for (KeystoreModificationRequest.Modification mod : request.modifications()) {
            switch (mod.type()) {
                case "DELETE":
                    if (keyStore.containsAlias(mod.alias())) {
                        keyStore.deleteEntry(mod.alias());
                    }
                    break;
                case "RENAME":
                    if (keyStore.containsAlias(mod.alias()) && !keyStore.containsAlias(mod.newAlias())) {
                        Certificate cert = keyStore.getCertificate(mod.alias());
                        if (keyStore.isKeyEntry(mod.alias())) {
                            KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(request.password().toCharArray());
                            KeyStore.Entry entry = keyStore.getEntry(mod.alias(), protParam);
                            keyStore.setEntry(mod.newAlias(), entry, protParam);
                        } else {
                            keyStore.setCertificateEntry(mod.newAlias(), cert);
                        }
                        keyStore.deleteEntry(mod.alias());
                    }
                    break;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyStore.store(baos, request.password().toCharArray());
        return new ByteArrayResource(baos.toByteArray());
    }

    private CertificateDetailsDto convertToDto(X509Certificate cert) throws Exception {
        JcaX509CertificateHolder certHolder = new JcaX509CertificateHolder(cert);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        return new CertificateDetailsDto(
                x500NameToMap(certHolder.getSubject()),
                x500NameToMap(certHolder.getIssuer()),
                "0x" + cert.getSerialNumber().toString(16),
                cert.getVersion(),
                sdf.format(cert.getNotBefore()),
                sdf.format(cert.getNotAfter()),
                cert.getSigAlgName(),
                cert.getPublicKey().getAlgorithm(),
                getFingerprint(cert, "SHA-256"),
                getFingerprint(cert, "SHA-1")
        );
    }

    private KeyStore loadKeystore(MultipartFile file, String password) throws Exception {
        String type = "JKS";
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.endsWith(".p12") || filename.endsWith(".pfx"))) {
            type = "PKCS12";
        }
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream is = file.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }

    private Map<String, String> x500NameToMap(X500Name name) {
        Map<String, String> map = new LinkedHashMap<>();
        for (org.bouncycastle.asn1.x500.RDN rdn : name.getRDNs()) {
            map.put(IETFUtils.valueToString(rdn.getFirst().getType()), IETFUtils.valueToString(rdn.getFirst().getValue()));
        }
        return map;
    }

    private String getFingerprint(X509Certificate cert, String algorithm) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(cert.getEncoded());
        byte[] digest = md.digest();
        StringBuilder hexString = new StringBuilder(2 * digest.length);
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
