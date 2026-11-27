package br.com.stuxnet.command.cliente;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.dto.ClienteDTO;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.ClienteService;
import java.io.IOException;

public final class CadastrarClienteCommand implements Command {
    private final ClienteService servico;

    public CadastrarClienteCommand(ClienteService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        ClienteDTO.Resposta dto = servico.criar(requisicao.corpo(ClienteDTO.Cadastro.class));
        return RespostaHttp.criado(dto, "/api/clientes/" + dto.id());
    }
}
