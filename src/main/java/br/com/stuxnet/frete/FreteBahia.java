package br.com.stuxnet.frete;

import java.math.BigDecimal;

public final class FreteBahia implements FreteStrategy {
    public Cotacao calcular() { return new Cotacao(new BigDecimal("22.90"), 2); }
}
