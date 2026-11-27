package br.com.stuxnet.frete;

import java.math.BigDecimal;

public final class FreteOutrasRegioes implements FreteStrategy {
    public Cotacao calcular() { return new Cotacao(new BigDecimal("44.90"), 8); }
}
