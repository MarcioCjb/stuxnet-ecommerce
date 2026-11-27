package br.com.stuxnet.command.entrega;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.FreteService;
import java.io.IOException;

public final class CalcularFreteCommand implements Command {
    private final FreteService servico;

    public CalcularFreteCommand(FreteService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        return RespostaHttp.ok(servico.consultar(requisicao.query("cep"), requisicao.query("modalidade")));
    }
}
