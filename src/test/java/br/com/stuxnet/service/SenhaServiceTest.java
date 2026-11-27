package br.com.stuxnet.service;

import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SenhaServiceTest {
    @Test void hashPodeSerConferidoEUsaSaltAleatorio() throws Exception {
        SenhaService service = new SenhaService();
        String primeiro = service.gerarHash("segredo123");
        assertNotEquals(primeiro, service.gerarHash("segredo123"));
        String[] partes = primeiro.split("\\$");
        byte[] salt = Base64.getDecoder().decode(partes[2]);
        PBEKeySpec chave = new PBEKeySpec("segredo123".toCharArray(), salt, Integer.parseInt(partes[1]), 256);
        byte[] recalculado = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(chave).getEncoded();
        assertArrayEquals(Base64.getDecoder().decode(partes[3]), recalculado);
        assertFalse(primeiro.contains("segredo123"));
    }
}
