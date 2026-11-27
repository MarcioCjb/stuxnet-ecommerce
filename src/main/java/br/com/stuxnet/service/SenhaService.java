package br.com.stuxnet.service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class SenhaService {
    public String gerarHash(String senha) {
        Validacao.exigir(senha != null && senha.length() >= 8 && senha.length() <= 128,
                "senha deve ter entre 8 e 128 caracteres");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        PBEKeySpec chave = new PBEKeySpec(senha.toCharArray(), salt, 210_000, 256);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(chave).getEncoded();
            return "pbkdf2-sha256$210000$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo de senha indisponível", e);
        } finally {
            chave.clearPassword();
        }
    }
}
