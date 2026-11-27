package br.com.stuxnet.dao.jdbc;

import br.com.stuxnet.dao.DAO;
import br.com.stuxnet.infra.BancoDeDados;
import br.com.stuxnet.model.Identificavel;
import java.sql.*;
import java.util.*;

/** O nome da tabela é definido apenas pelas subclasses, nunca por entradas HTTP. */
public abstract class AbstractJdbcDAO<T extends Identificavel> implements DAO<T> {
    protected final BancoDeDados banco;
    private final String tabela;

    protected AbstractJdbcDAO(BancoDeDados banco, String tabela) { this.banco = banco; this.tabela = tabela; }
    protected abstract T ler(ResultSet rs) throws SQLException;

    protected PreparedStatement preparar(String sql, Object... valores) throws SQLException {
        PreparedStatement stmt = banco.conexao().prepareStatement(sql);
        try {
            for (int i = 0; i < valores.length; i++) stmt.setObject(i + 1, valores[i]);
            return stmt;
        } catch (SQLException e) { stmt.close(); throw e; }
    }
    protected void executar(String sql, Object... valores) throws SQLException {
        try (var stmt = preparar(sql, valores)) { stmt.executeUpdate(); }
    }
    public long proximoId() {
        try (var stmt = preparar("SELECT nextval(pg_get_serial_sequence(?, 'id'))", tabela);
             var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        catch (SQLException e) { throw BancoDeDados.falha(e); }
    }
    public Optional<T> buscar(long id) {
        try (var stmt = preparar("SELECT * FROM " + tabela + " WHERE id = ?", id);
             var rs = stmt.executeQuery()) { return rs.next() ? Optional.of(ler(rs)) : Optional.empty(); }
        catch (SQLException e) { throw BancoDeDados.falha(e); }
    }
    public List<T> listar() {
        try (var stmt = preparar("SELECT * FROM " + tabela + " ORDER BY id"); var rs = stmt.executeQuery()) {
            List<T> lista = new ArrayList<>();
            while (rs.next()) lista.add(ler(rs));
            return List.copyOf(lista);
        } catch (SQLException e) { throw BancoDeDados.falha(e); }
    }
    public void excluir(long id) {
        try { executar("DELETE FROM " + tabela + " WHERE id = ?", id); }
        catch (SQLException e) { throw BancoDeDados.falha(e); }
    }
}
