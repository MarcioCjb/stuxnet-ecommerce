package br.com.stuxnet.service;

import java.math.BigDecimal;

final class Validacao {
    private Validacao() {}
    static String texto(String valor, String campo, int maximo) {
        exigir(valor != null && !valor.isBlank() && valor.strip().length() <= maximo,
                campo + " é obrigatório e deve ter até " + maximo + " caracteres");
        return valor.strip();
    }
    static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new IllegalArgumentException(mensagem);
    }
    static BigDecimal dinheiro(BigDecimal valor) {
        exigir(valor != null && valor.signum() > 0 && valor.compareTo(new BigDecimal("99999999.99")) <= 0,
                "preco deve ser positivo e até 99999999.99");
        try { return valor.setScale(2, java.math.RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("preco deve ter até duas casas decimais"); }
    }
}
