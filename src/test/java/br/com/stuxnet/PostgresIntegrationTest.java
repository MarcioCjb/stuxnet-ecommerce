package br.com.stuxnet;

import br.com.stuxnet.dao.jdbc.*;
import br.com.stuxnet.infra.*;
import br.com.stuxnet.model.*;
import br.com.stuxnet.support.PostgresTeste;
import br.com.stuxnet.support.ViaCepStub;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PostgresIntegrationTest {
    private PostgresTeste postgres;
    @BeforeEach void iniciar() throws Exception {
        Assumptions.assumeTrue(PostgresTeste.habilitado(), "Configure TEST_DB_URL para testar PostgreSQL real");
        postgres = new PostgresTeste();
    }
    @AfterEach void parar() throws Exception { if (postgres != null) postgres.close(); }

    private Produto produto(long id, int estoque) {
        return new Produto(id, "SSD", "Persistente", Produto.Categoria.HARDWARE, new BigDecimal("199.90"), estoque);
    }

    @Test void dadosPersistemAposReiniciarServidorHttp() throws Exception {
        try (var cep = new ViaCepStub()) {
            BancoDeDados banco = postgres.banco();
            var servidor = StuxNetApplication.criarServidor(0, ConfiguracaoAplicacao.criarRouter(cep.service(),
                    new ProdutoJdbcDAO(banco), new ClienteJdbcDAO(banco), new PedidoJdbcDAO(banco), new Transacao(banco)));
            servidor.start();
            long id;
            var http = HttpClient.newHttpClient();
            try {
                var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/api/produtos"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"nome\":\"SSD\",\"categoria\":\"HARDWARE\",\"preco\":199.90,\"estoque\":4}" )).build();
                var resposta = http.send(req, HttpResponse.BodyHandlers.ofString());
                assertEquals(201, resposta.statusCode(), resposta.body());
                id = JsonParser.parseString(resposta.body()).getAsJsonObject().get("id").getAsLong();
            } finally { servidor.stop(0); }
            BancoDeDados novoBanco = postgres.novaConexao();
            novoBanco.inicializar(); // Inicialização repetida não remove dados.
            var reiniciado = StuxNetApplication.criarServidor(0, ConfiguracaoAplicacao.criarRouter(cep.service(),
                    new ProdutoJdbcDAO(novoBanco), new ClienteJdbcDAO(novoBanco), new PedidoJdbcDAO(novoBanco), new Transacao(novoBanco)));
            reiniciado.start();
            try {
                var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + reiniciado.getAddress().getPort() + "/api/produtos/" + id)).GET().build();
                var resposta = http.send(req, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, resposta.statusCode());
                assertEquals(4, JsonParser.parseString(resposta.body()).getAsJsonObject().get("estoque").getAsInt());
            } finally { reiniciado.stop(0); }
        }
    }

    @Test void falhaSqlAoGravarPedidoDesfazBaixaDeEstoque() {
        var banco = postgres.banco();
        var produtos = new ProdutoJdbcDAO(banco);
        long id = banco.transacionar(() -> { long novo = produtos.proximoId(); produtos.salvar(produto(novo, 5)); return novo; });
        assertThrows(RuntimeException.class, () -> banco.transacionar(() -> {
            produtos.salvar(produto(id, 3));
            // Cliente inexistente causa falha de FK depois da baixa de estoque.
            var pedidos = new PedidoJdbcDAO(banco);
            pedidos.salvar(new Pedido(pedidos.proximoId(), 99999,
                    new Endereco(1, "01001000", "Praça", "1", "", "Centro", "São Paulo", "SP"), List.of(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "NORMAL", 1, Pedido.Status.CRIADO));
            return null;
        }));
        assertEquals(5, banco.transacionar(() -> produtos.buscar(id).orElseThrow().estoque()));
        assertTrue(banco.transacionar(() -> new PedidoJdbcDAO(banco).listar().isEmpty()));
    }

    @Test void erroAoSalvarEnderecoDesfazAlteracaoDoCliente() {
        var banco = postgres.banco();
        var clientes = new ClienteJdbcDAO(banco);
        long id = banco.transacionar(() -> { long novo = clientes.proximoId(); clientes.salvar(new Cliente(novo, "Original", "cliente@example.com", "hash", List.of())); return novo; });
        assertThrows(RuntimeException.class, () -> banco.transacionar(() -> {
            clientes.salvar(new Cliente(id, "Alterado", "cliente@example.com", "hash", List.of(
                    new Endereco(1, "01001000", "Praça", "1", "", "Centro", "São Paulo", "UF_INVALIDA"))));
            return null;
        }));
        assertEquals("Original", banco.transacionar(() -> clientes.buscar(id).orElseThrow().nome()));
    }
}
