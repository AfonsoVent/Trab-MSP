package pt.unl.fct.msp.elderlyhealth.service;

import java.util.Set;

/** Validações usadas pelo CPS (NIF português e telefone português). */
public final class ValidadorDados {

    private static final Set<String> PREFIXOS_NIF_DOIS_DIGITOS = Set.of("45", "70", "71", "72", "74", "75", "77", "79");
    private static final Set<Character> PREFIXOS_NIF_UM_DIGITO = Set.of('1', '2', '3', '5', '6', '8', '9');

    private ValidadorDados() {
    }

    public static String normalizar(String valor) {
        return valor == null ? "" : valor.replaceAll("[\\s.-]", "");
    }

    /** NIF: 9 dígitos, prefixo válido e dígito de controlo (módulo 11). */
    public static boolean nifValido(String nifBruto) {
        String nif = normalizar(nifBruto);
        if (!nif.matches("\\d{9}")) {
            return false;
        }
        if (!PREFIXOS_NIF_UM_DIGITO.contains(nif.charAt(0)) && !PREFIXOS_NIF_DOIS_DIGITOS.contains(nif.substring(0, 2))) {
            return false;
        }
        int soma = 0;
        for (int i = 0; i < 8; i++) {
            soma += (nif.charAt(i) - '0') * (9 - i);
        }
        int controlo = 11 - (soma % 11);
        if (controlo >= 10) {
            controlo = 0;
        }
        return controlo == nif.charAt(8) - '0';
    }

    /** Telefone português: móvel (91/92/93/96) ou fixo (2x), com ou sem +351. */
    public static boolean telefoneValido(String telefoneBruto) {
        String tel = normalizar(telefoneBruto);
        if (tel.startsWith("+351")) {
            tel = tel.substring(4);
        } else if (tel.startsWith("00351")) {
            tel = tel.substring(5);
        }
        return tel.matches("9[1236]\\d{7}|2\\d{8}");
    }

    public static String normalizarTelefone(String telefoneBruto) {
        String tel = normalizar(telefoneBruto);
        if (tel.startsWith("+351")) {
            return tel.substring(4);
        }
        if (tel.startsWith("00351")) {
            return tel.substring(5);
        }
        return tel;
    }
}
