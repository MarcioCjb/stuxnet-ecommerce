package br.com.stuxnet.command;

import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import java.io.IOException;

/** Encapsula uma operação HTTP que o Front Controller pode executar pelo mesmo contrato. */
@FunctionalInterface
public interface Command {
    RespostaHttp executar(Requisicao requisicao) throws IOException;
}
