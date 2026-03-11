package dev.morkom.keymanager.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;

public class PemUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String toPem(X509Certificate certificate) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
        }
        return stringWriter.toString();
    }

    public static String toPem(PKCS10CertificationRequest csr) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded()));
        }
        return stringWriter.toString();
    }

    public static PKCS10CertificationRequest csrFromPem(String pem) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(pem))) {
            Object parsedObj = pemParser.readObject();
            if (parsedObj instanceof PKCS10CertificationRequest) {
                return (PKCS10CertificationRequest) parsedObj;
            }
            throw new IllegalArgumentException("PEM content is not a valid CSR");
        }
    }

    public static PublicKey getPublicKeyFromCsr(PKCS10CertificationRequest csr) throws Exception {
        return new JcaPEMKeyConverter().setProvider("BC").getPublicKey(csr.getSubjectPublicKeyInfo());
    }
}
