package br.com.stuxnet;

import br.com.stuxnet.support.ViaCepStub;
import br.com.stuxnet.support.PostgresTeste;
import br.com.stuxnet.infra.*;
import br.com.stuxnet.dao.jdbc.*;
import br.com.stuxnet.dao.memory.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ApiIntegrationTest {
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();
    private ViaCepStub cep;
    private HttpServer servidor;
    private String base;
    private PostgresTeste postgres;

    @BeforeEach void iniciar() throws Exception {
        cep = new ViaCepStub();
        if (PostgresTeste.habilitado()) {
            postgres = new PostgresTeste();
            BancoDeDados banco = postgres.banco();
            servidor = StuxNetApplication.criarServidor(0, ConfiguracaoAplicacao.criarRouter(cep.service(),
                    new ProdutoJdbcDAO(banco), new ClienteJdbcDAO(banco), new PedidoJdbcDAO(banco), new Transacao(banco)));
        } else {
            servidor = StuxNetApplication.criarServidor(0, ConfiguracaoAplicacao.criarRouter(cep.service(),
                    new ProdutoEmMemoriaDAO(), new ClienteEmMemoriaDAO(), new PedidoEmMemoriaDAO(), new Transacao()));
        }
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }
    @AfterEach void parar() throws Exception {
        if (servidor != null) servidor.stop(0);
        if (cep != null) cep.close();
        if (postgres != null) postgres.close();
    }

    private HttpRequest requisicao(String metodo, String rota, String corpo, String contentType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + rota));
        if (contentType != null) builder.header("Content-Type", contentType);
        return builder.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo)).build();
    }
    private HttpResponse<String> enviar(String metodo, String rota, String corpo, int status) throws Exception {
        HttpResponse<String> resposta = CLIENTE.send(requisicao(metodo, rota, corpo, "application/json"), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, resposta.statusCode(), resposta.body());
        assertTrue(resposta.headers().firstValue("X-Request-Id").isPresent());
        if (status != 204) assertTrue(resposta.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        if (status >= 400) {
            JsonObject erro = objeto(resposta);
            assertEquals(status, erro.get("status").getAsInt());
            assertEquals(resposta.headers().firstValue("X-Request-Id").orElseThrow(), erro.get("requestId").getAsString());
            assertFalse(erro.has("stackTrace"));
        }
        return resposta;
    }
    private JsonObject objeto(HttpResponse<String> resposta) { return JsonParser.parseString(resposta.body()).getAsJsonObject(); }
    private long id(HttpResponse<String> resposta) { return objeto(resposta).get("id").getAsLong(); }
    private long produto(String nome, int estoque) throws Exception {
        return id(enviar("POST", "/api/produtos", "{\"nome\":\"" + nome + "\",\"categoria\":\"HARDWARE\",\"preco\":499.90,\"estoque\":" + estoque + "}", 201));
    }
    private long cliente() throws Exception {
        return id(enviar("POST", "/api/clientes", "{\"nome\":\"Andrei\",\"email\":\"andrei@example.com\",\"senha\":\"senha12345\"}", 201));
    }
    private long endereco(long cliente) throws Exception {
        return id(enviar("POST", "/api/clientes/" + cliente + "/enderecos", "{\"cep\":\"01001-000\",\"numero\":\"100\"}", 201));
    }
    private String pedido(long cliente, long endereco, String itens) {
        return "{\"clienteId\":" + cliente + ",\"enderecoId\":" + endereco + ",\"itens\":" + itens + "}";
    }
    private String item(long produto, int quantidade) { return "{\"produtoId\":" + produto + ",\"quantidade\":" + quantidade + "}"; }

    @Test void crudProdutosPreservaContratoHttp() throws Exception {
        enviar("GET", "/api/health", null, 200);
        long id = produto("Memória DDR5", 10);
        assertEquals(1, JsonParser.parseString(enviar("GET", "/api/produtos", null, 200).body()).getAsJsonArray().size());
        JsonObject atualizado = objeto(enviar("PUT", "/api/produtos/" + id,
                "{\"nome\":\"Teclado\",\"categoria\":\"PERIFERICO\",\"preco\":99.99,\"estoque\":5}", 200));
        assertEquals("PERIFERICO", atualizado.get("categoria").getAsString());
        enviar("GET", "/api/produtos/" + id, null, 200);
        assertEquals("", enviar("DELETE", "/api/produtos/" + id, null, 204).body());
        enviar("GET", "/api/produtos/" + id, null, 404);
    }
    @Test void clienteUsaDtosSemSenhaEEmailUnico() throws Exception {
        long id = cliente();
        String rota = "/api/clientes/" + id;
        for (String caminho : List.of(rota, "/api/clientes")) {
            String json = enviar("GET", caminho, null, 200).body();
            assertFalse(json.toLowerCase().contains("senha"));
            assertFalse(json.contains("pbkdf2"));
        }
        enviar("POST", "/api/clientes", "{\"nome\":\"Outro\",\"email\":\"ANDREI@example.com\",\"senha\":\"senha12345\"}", 409);
        enviar("PUT", rota, "{\"nome\":\"Andrei Moreira\",\"email\":\"novo@example.com\"}", 200);
        enviar("GET", rota + "/enderecos", null, 200);
        enviar("DELETE", rota, null, 204);
        enviar("GET", rota, null, 404);
    }
    @Test void calculaFreteRegionalEExpressaPorComposicao() throws Exception {
        String[][] casos = {{"40010000", "NORMAL", "22.90", "2"}, {"01001000", "NORMAL", "34.90", "6"},
                {"80010000", "NORMAL", "49.90", "9"}, {"51020001", "NORMAL", "22.90", "4"},
                {"40010000", "EXPRESSO", "37.90", "1"}, {"51020001", "EXPRESSO", "37.90", "2"},
                {"01001000", "EXPRESSO", "49.90", "4"}, {"80010000", "EXPRESSO", "64.90", "7"}};
        for (String[] caso : casos) {
            JsonObject cotacao = objeto(enviar("GET", "/api/frete?cep=" + caso[0] + "&modalidade=" + caso[1], null, 200));
            assertEquals(new java.math.BigDecimal(caso[2]), cotacao.get("valor").getAsBigDecimal());
            assertEquals(Integer.parseInt(caso[3]), cotacao.get("prazoDiasUteis").getAsInt());
        }
        enviar("GET", "/api/frete?cep=01001000&modalidade=INVALIDA", null, 400);
    }
    @Test void pedidoCalculaTotalBaixaEstoqueEPreservaHistorico() throws Exception {
        long produto = produto("SSD", 10);
        long cliente = cliente();
        long endereco = endereco(cliente);
        String rotaCliente = "/api/clientes/" + cliente;
        JsonObject enderecos = JsonParser.parseString(enviar("GET", rotaCliente + "/enderecos", null, 200).body())
                .getAsJsonArray().get(0).getAsJsonObject();
        assertEquals("São Paulo", enderecos.get("cidade").getAsString());
        HttpResponse<String> resposta = enviar("POST", "/api/pedidos", pedido(cliente, endereco, "[" + item(produto, 2) + "]"), 201);
        JsonObject pedido = objeto(resposta);
        long pedidoId = id(resposta);
        assertEquals(new java.math.BigDecimal("1034.70"), pedido.get("total").getAsBigDecimal());
        assertEquals(8, objeto(enviar("GET", "/api/produtos/" + produto, null, 200)).get("estoque").getAsInt());
        enviar("GET", "/api/pedidos", null, 200);
        enviar("GET", "/api/pedidos/" + pedidoId, null, 200);
        enviar("DELETE", "/api/produtos/" + produto, null, 409);
        enviar("DELETE", rotaCliente, null, 409);
        for (int i = 0; i < 2; i++) {
            assertEquals("CANCELADO", objeto(enviar("PUT", "/api/pedidos/" + pedidoId + "/cancelamento", null, 200)).get("status").getAsString());
        }
        assertEquals(10, objeto(enviar("GET", "/api/produtos/" + produto, null, 200)).get("estoque").getAsInt());
        enviar("DELETE", rotaCliente, null, 409);
    }
    @Test void pedidoInvalidoNaoModificaEstoqueNemGravaPedido() throws Exception {
        long produto = produto("GPU", 2);
        long outro = produto("CPU", 0);
        long cliente = cliente();
        long endereco = endereco(cliente);
        enviar("POST", "/api/pedidos", pedido(cliente, endereco, "[" + item(produto, 1) + "," + item(outro, 1) + "]"), 409);
        enviar("POST", "/api/pedidos", pedido(cliente, endereco, "[" + item(produto, 1) + "," + item(produto, 1) + "]"), 400);
        enviar("POST", "/api/pedidos", pedido(cliente, endereco, "[null]"), 400);
        enviar("POST", "/api/pedidos", pedido(cliente, 999, "[" + item(produto, 1) + "]"), 404);
        enviar("POST", "/api/pedidos", pedido(cliente, endereco, "[" + item(produto, 1) + "," + item(999, 1) + "]"), 404);
        assertEquals(2, objeto(enviar("GET", "/api/produtos/" + produto, null, 200)).get("estoque").getAsInt());
        assertEquals("[]", enviar("GET", "/api/pedidos", null, 200).body());
    }
    @Test void ultimaUnidadeSoPodeSerVendidaUmaVez() throws Exception {
        long produto = produto("Última GPU", 1);
        long cliente = cliente();
        long endereco = endereco(cliente);
        HttpRequest request = requisicao("POST", "/api/pedidos", pedido(cliente, endereco, "[" + item(produto, 1) + "]"), "application/json");
        CompletableFuture<HttpResponse<String>> primeira = CLIENTE.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        CompletableFuture<HttpResponse<String>> segunda = CLIENTE.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(List.of(201, 409), List.of(primeira.join().statusCode(), segunda.join().statusCode()).stream().sorted().toList());
        assertEquals(0, objeto(enviar("GET", "/api/produtos/" + produto, null, 200)).get("estoque").getAsInt());
    }
    @Test void pedidoIgnoraTotaisDoConsumidorEPreservaPrecoHistorico() throws Exception {
        long produto = produto("SSD original", 10);
        long cliente = cliente();
        long endereco = endereco(cliente);
        String corpo = pedido(cliente, endereco, "[" + item(produto, 1) + "]");
        corpo = corpo.substring(0, corpo.length() - 1) + ",\"total\":0.01,\"frete\":0,\"subtotal\":0.01}";
        long pedidoId = id(enviar("POST", "/api/pedidos", corpo, 201));
        enviar("PUT", "/api/produtos/" + produto,
                "{\"nome\":\"SSD atualizado\",\"categoria\":\"HARDWARE\",\"preco\":999.90,\"estoque\":9}", 200);
        JsonObject historico = objeto(enviar("GET", "/api/pedidos/" + pedidoId, null, 200));
        assertEquals(new java.math.BigDecimal("534.80"), historico.get("total").getAsBigDecimal());
        JsonObject item = historico.getAsJsonArray("itens").get(0).getAsJsonObject();
        assertEquals("SSD original", item.get("nome").getAsString());
        assertEquals(new java.math.BigDecimal("499.90"), item.get("precoUnitario").getAsBigDecimal());
    }
    @Test void errosDeRoteamentoEEntradaSaoEstruturados() throws Exception {
        enviar("GET", "/api/inexistente", null, 404);
        assertTrue(enviar("PATCH", "/api/produtos", null, 405).headers().firstValue("Allow").orElseThrow().contains("POST"));
        enviar("GET", "/api/produtos/0", null, 400);
        enviar("GET", "/api/produtos/9999999999999999999999", null, 400);
        enviar("GET", "/api/cep/123", null, 400);
        enviar("GET", "/api/cep/99999999", null, 404);
        enviar("GET", "/api/cep/11111111", null, 502);
        enviar("GET", "/api/frete", null, 400);
        enviar("GET", "/api/frete?cep=01001000&cep=40010000", null, 400);
        for (String invalido : List.of("null", "[]", "", "{", "{} {}", "{nome:'teste'}", "{}")) {
            enviar("POST", "/api/produtos", invalido, 400);
        }
        enviar("POST", "/api/produtos", "{\"nome\":\"SSD\",\"categoria\":\"HARDWARE\",\"preco\":1.999,\"estoque\":10}", 400);
        enviar("POST", "/api/produtos", "{\"nome\":\"SSD\",\"categoria\":\"HARDWARE\",\"preco\":1.99,\"estoque\":1.5}", 400);
        enviar("POST", "/api/produtos", "{\"nome\":\"" + "a".repeat(103000) + "\"}", 413);
        HttpResponse<String> resposta = CLIENTE.send(requisicao("POST", "/api/produtos", "{}", "text/plain"), HttpResponse.BodyHandlers.ofString());
        assertEquals(415, resposta.statusCode());
        assertEquals(415, CLIENTE.send(requisicao("POST", "/api/produtos", "{}", null), HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
