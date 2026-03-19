package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.GenerateSshKeyRequest;
import dev.morkom.keymanager.dto.GenerateSshKeyResponse;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Service
public class SshKeyService {

    public GenerateSshKeyResponse generateSshKey(GenerateSshKeyRequest request) throws Exception {
        KeyPairGenerator keyPairGenerator;
        String keyType;

        if ("EC".equalsIgnoreCase(request.keyAlgorithm())) {
            keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            keyType = "ecdsa-sha2-nistp256";
        } else {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(4096);
            keyType = "ssh-rsa";
        }

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // **THE FIX:** Convert the Java PublicKey to a Bouncy Castle AsymmetricKeyParameter first.
        AsymmetricKeyParameter publicKeyParameters = PublicKeyFactory.createKey(keyPair.getPublic().getEncoded());
        byte[] openSshPublicKeyBytes = OpenSSHPublicKeyUtil.encodePublicKey(publicKeyParameters);
        String publicKeyString = keyType + " " + Base64.getEncoder().encodeToString(openSshPublicKeyBytes) + " " + request.comment();

        // Format the private key in standard PEM format
        StringWriter privateKeyWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(privateKeyWriter)) {
            pemWriter.writeObject(keyPair.getPrivate());
        }
        String privateKeyPem = privateKeyWriter.toString();

        return new GenerateSshKeyResponse(publicKeyString, privateKeyPem);
    }
}
