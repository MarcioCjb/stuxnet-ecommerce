package br.com.stuxnet.command.produto;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.dto.ProdutoDTO;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.ProdutoService;
import java.io.IOException;

public final class AtualizarProdutoCommand implements Command {
    private final ProdutoService servico;

    public AtualizarProdutoCommand(ProdutoService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        return RespostaHttp.ok(servico.salvar(requisicao.id("id"), requisicao.corpo(ProdutoDTO.Requisicao.class)));
    }
}
