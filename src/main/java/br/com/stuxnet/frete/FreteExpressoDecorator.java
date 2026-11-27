package br.com.stuxnet.frete;

import java.math.BigDecimal;

/** Decorator: mantém o contrato e acrescenta entrega expressa à política regional. */
public final class FreteExpressoDecorator implements FreteStrategy {
    private final FreteStrategy base;

    public FreteExpressoDecorator(FreteStrategy base) { this.base = base; }
    public Cotacao calcular() {
        Cotacao cotacao = base.calcular();
        return new Cotacao(cotacao.valor().add(new BigDecimal("15.00")), Math.max(1, cotacao.prazoDiasUteis() - 2));
    }
}
