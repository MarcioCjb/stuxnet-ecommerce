package br.com.stuxnet.dto;

import br.com.stuxnet.model.Produto;
import java.math.BigDecimal;

public final class ProdutoDTO {
    private ProdutoDTO() {}
    public record Requisicao(String nome, String descricao, String categoria, BigDecimal preco, Integer estoque) {}
    public record Resposta(long id, String nome, String descricao, String categoria, BigDecimal preco, int estoque) {
        public static Resposta de(Produto produto) {
            return new Resposta(produto.id(), produto.nome(), produto.descricao(), produto.categoria().name(),
                    produto.preco(), produto.estoque());
        }
    }
}
