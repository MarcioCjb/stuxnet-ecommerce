package br.com.stuxnet.support;

import br.com.stuxnet.infra.BancoDeDados;
import java.sql.*;
import java.util.UUID;

/** Cada teste usa um schema exclusivo, sem apagar tabelas de outros testes ou da aplicação. */
public final class PostgresTeste implements AutoCloseable {
    private final String url = System.getenv("TEST_DB_URL");
    private final String usuario = System.getenv().getOrDefault("TEST_DB_USER", "stuxnet");
    private final String senha = System.getenv("TEST_DB_PASSWORD");
    private final String schema = "teste_" + UUID.randomUUID().toString().replace("-", "");
    private final String urlIsolada;
    private final BancoDeDados banco;

    public static boolean habilitado() { return System.getenv("TEST_DB_URL") != null; }

    public PostgresTeste() throws SQLException {
        if (url == null || senha == null) throw new IllegalStateException("Configure TEST_DB_URL e TEST_DB_PASSWORD");
        try (var c = DriverManager.getConnection(url, usuario, senha); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        urlIsolada = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        banco = novaConexao();
        try { banco.inicializar(); }
        catch (RuntimeException | Error e) {
            try { close(); } catch (SQLException limpeza) { e.addSuppressed(limpeza); }
            throw e;
        }
    }
    public BancoDeDados banco() { return banco; }
    public BancoDeDados novaConexao() { return new BancoDeDados(urlIsolada, usuario, senha); }
    public void close() throws SQLException {
        try (var c = DriverManager.getConnection(url, usuario, senha); var s = c.createStatement()) {
            s.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
}
