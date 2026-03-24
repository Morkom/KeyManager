package dev.morkom.keymanager.service;

import dev.morkom.keymanager.dto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final String SECRET_KEY_ALGO = "AES";

    private final PasswordEncoder passwordEncoder;

    public CryptoService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public CryptoResponse encrypt(CryptoRequest request) throws Exception {
        byte[] salt = getRandomNonce(SALT_LENGTH_BYTE);
        byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

        SecretKey aesKey = getAESKeyFromPassword(request.password().toCharArray(), salt);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] plainText = Base64.getDecoder().decode(request.data());
        byte[] cipherText = cipher.doFinal(plainText);

        byte[] cipherTextWithIvSalt = ByteBuffer.allocate(salt.length + iv.length + cipherText.length)
                .put(salt)
                .put(iv)
                .put(cipherText)
                .array();

        return new CryptoResponse(Base64.getEncoder().encodeToString(cipherTextWithIvSalt));
    }

    public CryptoResponse decrypt(CryptoRequest request) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(request.data());
        ByteBuffer bb = ByteBuffer.wrap(decodedData);

        byte[] salt = new byte[SALT_LENGTH_BYTE];
        bb.get(salt);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        SecretKey aesKey = getAESKeyFromPassword(request.password().toCharArray(), salt);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] plainText = cipher.doFinal(cipherText);

        return new CryptoResponse(Base64.getEncoder().encodeToString(plainText));
    }

    public BcryptHashResponse bcryptHash(BcryptHashRequest request) {
        String hash = passwordEncoder.encode(request.password());
        return new BcryptHashResponse(hash);
    }

    public BcryptVerifyResponse bcryptVerify(BcryptVerifyRequest request) {
        boolean matches = passwordEncoder.matches(request.password(), request.hash());
        return new BcryptVerifyResponse(matches);
    }

    private static byte[] getRandomNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    private static SecretKey getAESKeyFromPassword(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), SECRET_KEY_ALGO);
        return secret;
    }
}
