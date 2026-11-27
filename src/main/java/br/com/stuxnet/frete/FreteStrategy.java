package br.com.stuxnet.frete;

import java.math.BigDecimal;

/** Strategy: cada região fornece uma política de preço e prazo substituível. */
public interface FreteStrategy {
    record Cotacao(BigDecimal valor, int prazoDiasUteis) {}
    Cotacao calcular();
}
