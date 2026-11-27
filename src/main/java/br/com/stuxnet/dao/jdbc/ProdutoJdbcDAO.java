package br.com.stuxnet.dao.jdbc;

import br.com.stuxnet.dao.ProdutoDAO;
import br.com.stuxnet.infra.BancoDeDados;
import br.com.stuxnet.model.Produto;
import java.sql.*;

public final class ProdutoJdbcDAO extends AbstractJdbcDAO<Produto> implements ProdutoDAO {
    public ProdutoJdbcDAO(BancoDeDados banco) { super(banco, "produtos"); }
    protected Produto ler(ResultSet rs) throws SQLException {
        return new Produto(rs.getLong("id"), rs.getString("nome"), rs.getString("descricao"),
                Produto.Categoria.valueOf(rs.getString("categoria")), rs.getBigDecimal("preco"), rs.getInt("estoque"));
    }
    public Produto salvar(Produto p) {
        try {
            executar("""
                    INSERT INTO produtos (id, nome, descricao, categoria, preco, estoque) VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET nome=EXCLUDED.nome, descricao=EXCLUDED.descricao,
                    categoria=EXCLUDED.categoria, preco=EXCLUDED.preco, estoque=EXCLUDED.estoque
                    """, p.id(), p.nome(), p.descricao(), p.categoria().name(), p.preco(), p.estoque());
            return p;
        } catch (SQLException e) { throw BancoDeDados.falha(e); }
    }
}
