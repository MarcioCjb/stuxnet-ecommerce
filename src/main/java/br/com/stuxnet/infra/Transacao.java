package br.com.stuxnet.infra;

import java.util.function.Supplier;

/** Coordena uma operação de negócio inteira, incluindo todos os seus DAOs. */
public final class Transacao {
    private final BancoDeDados banco;

    public Transacao() { this.banco = null; }
    public Transacao(BancoDeDados banco) { this.banco = banco; }

    public synchronized <T> T executar(Supplier<T> operacao) {
        return banco == null ? operacao.get() : banco.transacionar(operacao);
    }
}
