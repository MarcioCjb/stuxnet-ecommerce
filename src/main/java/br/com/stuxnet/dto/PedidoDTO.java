package br.com.stuxnet.dto;

import br.com.stuxnet.model.Pedido;
import java.math.BigDecimal;
import java.util.List;

public final class PedidoDTO {
    private PedidoDTO() {}
    public record ItemRequisicao(Long produtoId, Integer quantidade) {}
    public record Requisicao(Long clienteId, Long enderecoId, List<ItemRequisicao> itens, String modalidadeFrete) {}
    public record ItemResposta(long produtoId, String nome, int quantidade, BigDecimal precoUnitario, BigDecimal total) {}
    public record Resposta(long id, long clienteId, EnderecoClienteDTO.Resposta endereco,
                           List<ItemResposta> itens, BigDecimal subtotal, BigDecimal frete, BigDecimal total,
                           String modalidadeFrete, int prazoDiasUteis, String status) {
        public static Resposta de(Pedido pedido) {
            return new Resposta(pedido.id(), pedido.clienteId(), EnderecoClienteDTO.Resposta.de(pedido.endereco()),
                    pedido.itens().stream().map(i -> new ItemResposta(i.produtoId(), i.nome(), i.quantidade(),
                            i.precoUnitario(), i.total())).toList(), pedido.subtotal(), pedido.frete(), pedido.total(),
                    pedido.modalidadeFrete(), pedido.prazoDiasUteis(), pedido.status().name());
        }
    }
}
