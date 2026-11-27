package br.com.stuxnet.command.produto;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.service.ProdutoService;
import java.io.IOException;

public final class ListarProdutosCommand implements Command {
    private final ProdutoService servico;

    public ListarProdutosCommand(ProdutoService servico) { this.servico = servico; }

    @Override
    public RespostaHttp executar(Requisicao requisicao) throws IOException {
        return RespostaHttp.ok(servico.listar());
    }
}
