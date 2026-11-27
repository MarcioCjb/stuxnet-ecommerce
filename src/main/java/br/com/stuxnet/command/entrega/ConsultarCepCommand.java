package br.com.stuxnet.command.entrega;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.ConsultaCep;
import java.io.IOException;

public final class ConsultarCepCommand implements Command {
    private final ConsultaCep servico;

    public ConsultarCepCommand(ConsultaCep servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        return RespostaHttp.ok(servico.consultarCEP(requisicao.parametro("cep")));
    }
}
