package br.com.stuxnet.command.cliente;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.dto.EnderecoClienteDTO;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.ClienteService;
import java.io.IOException;

public final class CadastrarEnderecoCommand implements Command {
    private final ClienteService servico;

    public CadastrarEnderecoCommand(ClienteService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        long clienteId = requisicao.id("id");
        EnderecoClienteDTO.Resposta dto = servico.adicionarEndereco(clienteId,
                requisicao.corpo(EnderecoClienteDTO.Requisicao.class));
        return RespostaHttp.criado(dto, "/api/clientes/" + clienteId + "/enderecos");
    }
}
