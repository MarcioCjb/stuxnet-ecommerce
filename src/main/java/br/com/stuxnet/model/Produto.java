package br.com.stuxnet.model;

import java.math.BigDecimal;

public record Produto(long id, String nome, String descricao, Categoria categoria,
                      BigDecimal preco, int estoque) implements Identificavel {
    public enum Categoria { HARDWARE, PERIFERICO }

    public Produto comEstoque(int quantidade) {
        return new Produto(id, nome, descricao, categoria, preco, quantidade);
    }
}
