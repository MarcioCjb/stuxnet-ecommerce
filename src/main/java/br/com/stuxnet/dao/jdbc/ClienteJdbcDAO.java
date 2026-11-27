package br.com.stuxnet.dao.jdbc;

import br.com.stuxnet.dao.ClienteDAO;
import br.com.stuxnet.infra.BancoDeDados;
import br.com.stuxnet.model.*;
import java.sql.*;
import java.util.ArrayList;

public final class ClienteJdbcDAO extends AbstractJdbcDAO<Cliente> implements ClienteDAO {
    public ClienteJdbcDAO(BancoDeDados banco) { super(banco, "clientes"); }
    protected Cliente ler(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        var enderecos = new ArrayList<Endereco>();
        try (var stmt = preparar("SELECT * FROM enderecos WHERE cliente_id = ? ORDER BY id", id);
             var endereco = stmt.executeQuery()) {
            while (endereco.next()) enderecos.add(new Endereco(endereco.getLong("id"), endereco.getString("cep"),
                    endereco.getString("logradouro"), endereco.getString("numero"), endereco.getString("complemento"),
                    endereco.getString("bairro"), endereco.getString("cidade"), endereco.getString("uf")));
        }
        return new Cliente(id, rs.getString("nome"), rs.getString("email"), rs.getString("senha_hash"), enderecos);
    }
    public Cliente salvar(Cliente c) {
        try {
            executar("""
                    INSERT INTO clientes (id, nome, email, senha_hash) VALUES (?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET nome=EXCLUDED.nome, email=EXCLUDED.email, senha_hash=EXCLUDED.senha_hash
                    """, c.id(), c.nome(), c.email(), c.senhaHash());
            executar("DELETE FROM enderecos WHERE cliente_id = ?", c.id());
            for (Endereco e : c.enderecos()) executar("""
                    INSERT INTO enderecos (cliente_id, id, cep, logradouro, numero, complemento, bairro, cidade, uf)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, c.id(), e.id(), e.cep(), e.logradouro(), e.numero(), e.complemento(), e.bairro(), e.cidade(), e.uf());
            return c;
        } catch (SQLException e) { throw BancoDeDados.falha(e); }
    }
}
