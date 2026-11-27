package br.com.stuxnet.frete;

import java.math.BigDecimal;

public final class FreteSudeste implements FreteStrategy {
    public Cotacao calcular() { return new Cotacao(new BigDecimal("34.90"), 6); }
}
