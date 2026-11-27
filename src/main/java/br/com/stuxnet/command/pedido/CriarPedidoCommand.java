package br.com.stuxnet.command.pedido;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.dto.PedidoDTO;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.PedidoService;
import java.io.IOException;

public final class CriarPedidoCommand implements Command {
    private final PedidoService servico;

    public CriarPedidoCommand(PedidoService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        PedidoDTO.Resposta dto = servico.criar(requisicao.corpo(PedidoDTO.Requisicao.class));
        return RespostaHttp.criado(dto, "/api/pedidos/" + dto.id());
    }
}
