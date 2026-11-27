package br.com.stuxnet.frete;

import java.math.BigDecimal;

/** Política compartilhada pelas UFs do Nordeste, exceto a Bahia. */
public final class FreteNordeste implements FreteStrategy {
    public Cotacao calcular() { return new Cotacao(new BigDecimal("22.90"), 4); }
}
