package br.com.stuxnet.dao.jdbc;

import br.com.stuxnet.dao.PedidoDAO;
import br.com.stuxnet.infra.BancoDeDados;
import br.com.stuxnet.model.*;
import com.google.gson.Gson;
import java.sql.*;
import java.util.ArrayList;

public final class PedidoJdbcDAO extends AbstractJdbcDAO<Pedido> implements PedidoDAO {
    private static final Gson GSON = new Gson();
    public PedidoJdbcDAO(BancoDeDados banco) { super(banco, "pedidos"); }
    protected Pedido ler(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        var itens = new ArrayList<Pedido.Item>();
        try (var stmt = preparar("SELECT * FROM pedido_itens WHERE pedido_id = ? ORDER BY posicao", id);
             var item = stmt.executeQuery()) {
            while (item.next()) itens.add(new Pedido.Item(item.getLong("produto_id"), item.getString("nome"),
                    item.getInt("quantidade"), item.getBigDecimal("preco_unitario"), item.getBigDecimal("total")));
        }
        return new Pedido(id, rs.getLong("cliente_id"), GSON.fromJson(rs.getString("endereco"), Endereco.class),
                itens, rs.getBigDecimal("subtotal"), rs.getBigDecimal("frete"), rs.getBigDecimal("total"),
                rs.getString("modalidade_frete"), rs.getInt("prazo_dias_uteis"), Pedido.Status.valueOf(rs.getString("status")));
    }
    public Pedido salvar(Pedido p) {
        try {
            executar("""
                    INSERT INTO pedidos (id, cliente_id, endereco, subtotal, frete, total, modalidade_frete, prazo_dias_uteis, status)
                    VALUES (?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET status=EXCLUDED.status
                    """, p.id(), p.clienteId(), GSON.toJson(p.endereco()), p.subtotal(), p.frete(), p.total(),
                    p.modalidadeFrete(), p.prazoDiasUteis(), p.status().name());
            executar("DELETE FROM pedido_itens WHERE pedido_id = ?", p.id());
            int posicao = 0;
            for (Pedido.Item i : p.itens()) executar("""
                    INSERT INTO pedido_itens (pedido_id, produto_id, nome, quantidade, preco_unitario, total, posicao)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, p.id(), i.produtoId(), i.nome(), i.quantidade(), i.precoUnitario(), i.total(), posicao++);
            return p;
        } catch (SQLException e) { throw BancoDeDados.falha(e); }
    }
}
