package br.com.stuxnet.infra;

import br.com.stuxnet.exception.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;
import java.util.function.Supplier;

/** JDBC simples: uma conexão e uma transação por operação de negócio. */
public final class BancoDeDados {
    private final String url;
    private final String usuario;
    private final String senha;
    private final ThreadLocal<Connection> atual = new ThreadLocal<>();

    public BancoDeDados(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    public static BancoDeDados configurado() {
        Properties props = new Properties();
        Path arquivo = Path.of("database.properties");
        if (Files.exists(arquivo)) {
            try (var entrada = Files.newInputStream(arquivo)) { props.load(entrada); }
            catch (IOException e) { throw new IllegalStateException("Não foi possível ler database.properties", e); }
        }
        String senha = System.getenv().getOrDefault("DB_PASSWORD", props.getProperty("db.password", ""));
        if (senha.isBlank()) throw new IllegalStateException("Configure DB_PASSWORD ou db.password em database.properties");
        return new BancoDeDados(
                System.getenv().getOrDefault("DB_URL", props.getProperty("db.url", "jdbc:postgresql://127.0.0.1:5432/stuxnet")),
                System.getenv().getOrDefault("DB_USER", props.getProperty("db.user", "stuxnet")), senha);
    }

    public Connection conexao() {
        Connection conexao = atual.get();
        if (conexao == null) throw new IllegalStateException("DAO deve ser utilizado dentro de uma transação");
        return conexao;
    }

    public void inicializar() {
        try (var entrada = BancoDeDados.class.getResourceAsStream("/db/schema.sql")) {
            if (entrada == null) throw new IllegalStateException("Schema SQL não encontrado");
            String sql = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            transacionar(() -> {
                try (Statement stmt = conexao().createStatement()) { stmt.execute(sql); return null; }
                catch (SQLException e) { throw falha(e); }
            });
        } catch (IOException e) { throw new IllegalStateException("Não foi possível ler o schema SQL", e); }
    }

    public <T> T transacionar(Supplier<T> operacao) {
        if (atual.get() != null) return operacao.get();
        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            conexao.setAutoCommit(false);
            atual.set(conexao);
            try {
                // Serializa as operações inclusive entre duas instâncias da API no mesmo banco.
                try (Statement stmt = conexao.createStatement()) { stmt.execute("SELECT pg_advisory_xact_lock(73921401)"); }
                T resultado = operacao.get();
                conexao.commit();
                return resultado;
            } catch (RuntimeException | Error | SQLException e) {
                try { conexao.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql) throw falha(sql);
                throw e;
            } finally { atual.remove(); }
        } catch (SQLException e) { throw falha(e); }
    }

    public static RuntimeException falha(SQLException e) {
        if ("23505".equals(e.getSQLState())) return ApiException.conflito("Registro já cadastrado");
        if ("23503".equals(e.getSQLState())) return ApiException.conflito("Registro possui referências ou referência inexistente");
        return new IllegalStateException("Falha ao acessar PostgreSQL (SQLState " + e.getSQLState() + ")", e);
    }
}
