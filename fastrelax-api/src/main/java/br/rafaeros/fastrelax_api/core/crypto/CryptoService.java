package br.rafaeros.fastrelax_api.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Two-way encryption plus a deterministic blind index for sensitive columns.
 *
 * <p>
 * {@link #encrypt(String)} uses AES-GCM with a random IV, so the same input
 * never produces the same ciphertext. That protects the data at rest but makes
 * the column useless for lookups and unique constraints. {@link #blindIndex(String)}
 * fills that gap: it is a keyed HMAC, so equal inputs always map to the same
 * digest and the database can index and constrain it.
 *
 * <p>
 * Both keys are derived from {@code app.crypto.secret} with distinct labels, so
 * the ciphertext key and the index key are independent even though the
 * deployment only configures one secret.
 */
@Service
public class CryptoService {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private static final String AES_KEY_LABEL = "fastrelax:aes-gcm:v1";
    private static final String BLIND_INDEX_KEY_LABEL = "fastrelax:blind-index:v1";

    private final SecretKeySpec aesKey;
    private final SecretKeySpec blindIndexKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoService(@Value("${app.crypto.secret}") String masterSecret) {
        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalStateException("app.crypto.secret não configurado (variável AES_SECRET)");
        }
        this.aesKey = new SecretKeySpec(deriveKey(masterSecret, AES_KEY_LABEL), "AES");
        this.blindIndexKey = new SecretKeySpec(deriveKey(masterSecret, BLIND_INDEX_KEY_LABEL), MAC_ALGORITHM);
    }

    /** Encrypts to Base64 of {@code IV || ciphertext || GCM tag}. */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar dado sensível", e);
        }
    }

    /** Reverses {@link #encrypt(String)}. Throws if the payload was tampered with. */
    public String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Payload criptografado inválido");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(payload, IV_LENGTH_BYTES, payload.length - IV_LENGTH_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Falha ao descriptografar dado sensível", e);
        }
    }

    /**
     * Deterministic keyed digest used for equality lookups and unique constraints
     * on encrypted columns. Never reversible — it is an index, not storage.
     */
    public String blindIndex(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(blindIndexKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao gerar blind index", e);
        }
    }

    private static byte[] deriveKey(String masterSecret, String label) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(masterSecret.getBytes(StandardCharsets.UTF_8), MAC_ALGORITHM));
            return mac.doFinal(label.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao derivar chave de criptografia", e);
        }
    }
}
