package br.com.stuxnet.service;

import br.com.stuxnet.infra.Transacao;

import br.com.stuxnet.model.*;
import br.com.stuxnet.dto.FreteDTO;
import br.com.stuxnet.dto.PedidoDTO;
import br.com.stuxnet.exception.ApiException;
import br.com.stuxnet.dao.ClienteDAO;
import br.com.stuxnet.dao.PedidoDAO;
import br.com.stuxnet.dao.ProdutoDAO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PedidoService {
    private final PedidoDAO pedidos;
    private final ProdutoDAO produtos;
    private final ClienteDAO clientes;
    private final FreteService frete;
    private final Transacao transacao;

    public PedidoService(PedidoDAO pedidos, ProdutoDAO produtos,
                         ClienteDAO clientes, FreteService frete, Transacao transacao) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.clientes = clientes;
        this.frete = frete;
        this.transacao = transacao;
    }
    public List<PedidoDTO.Resposta> listar() {
        return transacao.executar(() -> pedidos.listar().stream().map(PedidoDTO.Resposta::de).toList());
    }
    public PedidoDTO.Resposta buscar(long id) {
        return transacao.executar(() -> PedidoDTO.Resposta.de(encontrar(id)));
    }
    public PedidoDTO.Resposta criar(PedidoDTO.Requisicao dto) {
        Validacao.exigir(dto.clienteId() != null && dto.clienteId() > 0, "clienteId deve ser positivo");
        Validacao.exigir(dto.enderecoId() != null && dto.enderecoId() > 0, "enderecoId deve ser positivo");
        Validacao.exigir(dto.itens() != null && !dto.itens().isEmpty() && dto.itens().size() <= 100,
                "itens deve conter entre 1 e 100 produtos distintos");
        return transacao.executar(() -> {
            Cliente cliente = clientes.buscar(dto.clienteId()).orElseThrow(() -> ApiException.naoEncontrado("Cliente"));
            Endereco endereco = cliente.enderecos().stream().filter(e -> e.id() == dto.enderecoId()).findFirst()
                    .orElseThrow(() -> ApiException.naoEncontrado("Endereço do cliente"));
            List<Pedido.Item> itens = new ArrayList<>();
            List<Produto> novosEstoques = new ArrayList<>();
            Set<Long> ids = new HashSet<>();
            BigDecimal subtotal = new BigDecimal("0.00");
            for (PedidoDTO.ItemRequisicao item : dto.itens()) {
                Validacao.exigir(item != null && item.produtoId() != null && item.produtoId() > 0
                        && item.quantidade() != null && item.quantidade() > 0, "Item requer produtoId e quantidade positivos");
                Validacao.exigir(ids.add(item.produtoId()), "Produto repetido: agrupe as quantidades em um único item");
                Produto produto = produtos.buscar(item.produtoId()).orElseThrow(() -> ApiException.naoEncontrado("Produto"));
                if (produto.estoque() < item.quantidade()) throw ApiException.conflito("Estoque insuficiente: " + produto.nome());
                BigDecimal total = produto.preco().multiply(BigDecimal.valueOf(item.quantidade()));
                itens.add(new Pedido.Item(produto.id(), produto.nome(), item.quantidade(), produto.preco(), total));
                novosEstoques.add(produto.comEstoque(produto.estoque() - item.quantidade()));
                subtotal = subtotal.add(total);
            }
            FreteDTO cotacao = frete.calcular(endereco.cep(), endereco.uf(), dto.modalidadeFrete());
            Pedido pedido = new Pedido(pedidos.proximoId(), cliente.id(), endereco, itens, subtotal, cotacao.valor(),
                    subtotal.add(cotacao.valor()), cotacao.modalidade(), cotacao.prazoDiasUteis(), Pedido.Status.CRIADO);
            // Estoque e pedido são confirmados juntos pela transação compartilhada.
            novosEstoques.forEach(produtos::salvar);
            return PedidoDTO.Resposta.de(pedidos.salvar(pedido));
        });
    }
    public PedidoDTO.Resposta cancelar(long id) {
        return transacao.executar(() -> {
            Pedido pedido = encontrar(id);
            if (pedido.status() == Pedido.Status.CANCELADO) return PedidoDTO.Resposta.de(pedido);
            List<Produto> restaurados = pedido.itens().stream().map(item -> {
                Produto produto = produtos.buscar(item.produtoId()).orElseThrow(() -> ApiException.naoEncontrado("Produto"));
                int estoque = Math.addExact(produto.estoque(), item.quantidade());
                if (estoque > 1_000_000) throw ApiException.conflito("Estoque excederia o limite ao cancelar o pedido");
                return produto.comEstoque(estoque);
            }).toList();
            restaurados.forEach(produtos::salvar);
            return PedidoDTO.Resposta.de(pedidos.salvar(pedido.cancelar()));
        });
    }
    private Pedido encontrar(long id) {
        return pedidos.buscar(id).orElseThrow(() -> ApiException.naoEncontrado("Pedido"));
    }
}
