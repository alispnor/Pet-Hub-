package com.alispnor.pethub.customer.infrastructure.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash determinístico (SHA-256) usado apenas para detecção de duplicidade do CPF
 * sem precisar descriptografar todos os registros. Não substitui o ciphertext
 * AES-GCM, que continua sendo a fonte da informação.
 */
@Component
public class CpfHasher {

    public String hash(String cpfDigitsOnly) {
        if (cpfDigitsOnly == null) {
            return null;
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(cpfDigitsOnly.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder(bytes.length * 2);
            for (var b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
