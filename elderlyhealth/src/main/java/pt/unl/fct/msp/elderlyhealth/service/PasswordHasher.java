package pt.unl.fct.msp.elderlyhealth.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de palavras-passe com PBKDF2-HMAC-SHA256 (só JDK, sem dependências).
 * Formato guardado: pbkdf2$iteracoes$salt$hash (Base64). Nunca se guarda a palavra-passe.
 */
public final class PasswordHasher {

    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_SALT = 16;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[TAMANHO_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERACOES + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    public static boolean verificar(String password, String guardado) {
        if (password == null || guardado == null) {
            return false;
        }
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) {
            return false;
        }
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] salt = b64.decode(partes[2]);
        byte[] esperado = b64.decode(partes[3]);
        byte[] obtido = pbkdf2(password.toCharArray(), salt, Integer.parseInt(partes[1]));
        return MessageDigest.isEqual(esperado, obtido); // comparação em tempo constante
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iteracoes, TAMANHO_HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponível", e);
        }
    }
}
