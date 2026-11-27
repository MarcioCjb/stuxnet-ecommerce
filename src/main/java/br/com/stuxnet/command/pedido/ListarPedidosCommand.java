package br.com.stuxnet.command.pedido;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.PedidoService;
import java.io.IOException;

public final class ListarPedidosCommand implements Command {
    private final PedidoService servico;

    public ListarPedidosCommand(PedidoService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        return RespostaHttp.ok(servico.listar());
    }
}
