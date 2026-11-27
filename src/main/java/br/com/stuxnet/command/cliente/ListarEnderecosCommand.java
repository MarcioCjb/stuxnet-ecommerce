package br.com.stuxnet.command.cliente;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.ClienteService;
import java.io.IOException;

public final class ListarEnderecosCommand implements Command {
    private final ClienteService servico;

    public ListarEnderecosCommand(ClienteService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        return RespostaHttp.ok(servico.enderecos(requisicao.id("id")));
    }
}
