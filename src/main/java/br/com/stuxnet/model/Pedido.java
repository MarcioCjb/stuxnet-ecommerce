package br.com.stuxnet.model;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(long id, long clienteId, Endereco endereco, List<Item> itens,
                     BigDecimal subtotal, BigDecimal frete, BigDecimal total,
                     String modalidadeFrete, int prazoDiasUteis, Status status) implements Identificavel {
    public enum Status { CRIADO, CANCELADO }
    public record Item(long produtoId, String nome, int quantidade, BigDecimal precoUnitario,
                       BigDecimal total) {}
    public Pedido { itens = List.copyOf(itens); }

    public Pedido cancelar() {
        return new Pedido(id, clienteId, endereco, itens, subtotal, frete, total,
                modalidadeFrete, prazoDiasUteis, Status.CANCELADO);
    }
}
