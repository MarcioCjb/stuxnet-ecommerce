package br.com.stuxnet.command.cliente;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.ClienteService;
import java.io.IOException;

public final class ExcluirClienteCommand implements Command {
    private final ClienteService servico;

    public ExcluirClienteCommand(ClienteService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        servico.excluir(requisicao.id("id"));
        return RespostaHttp.semConteudo();
    }
}
