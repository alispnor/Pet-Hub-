package com.alispnor.pethub.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@EnableConfigurationProperties(EncryptionProperties.class)
public class AesGcmCipher {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCipher(EncryptionProperties properties) {
        var raw = Base64.getDecoder().decode(properties.key());
        if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
            throw new IllegalStateException(
                    "app.encryption.key deve ser AES-128/192/256 (16/24/32 bytes em base64); recebido " + raw.length);
        }
        this.key = new SecretKeySpec(raw, ALGORITHM);
    }

    public String encrypt(String plaintext) {
        log.debug("Encrypt enter (len={})", plaintext == null ? 0 : plaintext.length());
        if (plaintext == null) {
            return null;
        }
        try {
            var iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            var cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            var packed = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
            var encoded = Base64.getEncoder().encodeToString(packed);
            log.debug("Encrypt exit (ciphertext len={})", encoded.length());
            return encoded;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar valor", e);
        }
    }

    public String decrypt(String ciphertext) {
        log.debug("Decrypt enter (len={})", ciphertext == null ? 0 : ciphertext.length());
        if (ciphertext == null) {
            return null;
        }
        try {
            var packed = Base64.getDecoder().decode(ciphertext);
            if (packed.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Ciphertext inválido: tamanho insuficiente");
            }
            var iv = new byte[IV_LENGTH_BYTES];
            var data = new byte[packed.length - IV_LENGTH_BYTES];
            System.arraycopy(packed, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(packed, IV_LENGTH_BYTES, data, 0, data.length);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            var plain = new String(cipher.doFinal(data), StandardCharsets.UTF_8);
            log.debug("Decrypt exit (plain len={})", plain.length());
            return plain;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar valor", e);
        }
    }
}
