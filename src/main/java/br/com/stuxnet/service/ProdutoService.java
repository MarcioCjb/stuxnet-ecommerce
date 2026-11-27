package br.com.stuxnet.service;

import br.com.stuxnet.model.Produto;
import br.com.stuxnet.dto.ProdutoDTO;
import br.com.stuxnet.exception.ApiException;
import br.com.stuxnet.dao.PedidoDAO;
import br.com.stuxnet.dao.ProdutoDAO;
import java.math.BigDecimal;
import java.util.List;

public final class ProdutoService {
    private final ProdutoDAO produtos;
    private final PedidoDAO pedidos;
    private final Object transacao;

    public ProdutoService(ProdutoDAO produtos, PedidoDAO pedidos, Object transacao) {
        this.produtos = produtos;
        this.pedidos = pedidos;
        this.transacao = transacao;
    }

    public List<ProdutoDTO.Resposta> listar() {
        synchronized (transacao) { return produtos.listar().stream().map(ProdutoDTO.Resposta::de).toList(); }
    }
    public ProdutoDTO.Resposta buscar(long id) {
        synchronized (transacao) { return ProdutoDTO.Resposta.de(encontrar(id)); }
    }
    public ProdutoDTO.Resposta salvar(Long id, ProdutoDTO.Requisicao dto) {
        String nome = Validacao.texto(dto.nome(), "nome", 150);
        String descricao = dto.descricao() == null ? "" : dto.descricao().strip();
        Validacao.exigir(descricao.length() <= 2000, "descricao deve ter até 2000 caracteres");
        Produto.Categoria categoria;
        try { categoria = Produto.Categoria.valueOf(dto.categoria() == null ? "" : dto.categoria()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("categoria deve ser HARDWARE ou PERIFERICO"); }
        BigDecimal preco = Validacao.dinheiro(dto.preco());
        Validacao.exigir(dto.estoque() != null && dto.estoque() >= 0 && dto.estoque() <= 1_000_000,
                "estoque deve estar entre 0 e 1000000");
        synchronized (transacao) {
            if (id != null) encontrar(id);
            Produto produto = new Produto(id == null ? produtos.proximoId() : id, nome, descricao,
                    categoria, preco, dto.estoque());
            return ProdutoDTO.Resposta.de(produtos.salvar(produto));
        }
    }
    public void excluir(long id) {
        synchronized (transacao) {
            encontrar(id);
            if (pedidos.listar().stream().anyMatch(p -> p.itens().stream().anyMatch(i -> i.produtoId() == id))) {
                throw ApiException.conflito("Produto possui histórico de pedidos e não pode ser excluído");
            }
            produtos.excluir(id);
        }
    }
    private Produto encontrar(long id) {
        return produtos.buscar(id).orElseThrow(() -> ApiException.naoEncontrado("Produto"));
    }
}
