package br.com.stuxnet.controller;

import br.com.stuxnet.infra.http.RespostaHttp;
import br.com.stuxnet.infra.http.Router;

import br.com.stuxnet.dto.ErroDTO;
import br.com.stuxnet.exception.ApiException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Único HttpHandler: pré-processamento, despacho e tratamento uniforme das respostas. */
public final class FrontController implements HttpHandler {
    private final Router router;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public FrontController(Router router) { this.router = router; }

    public void handle(HttpExchange exchange) throws IOException {
        String requestId = UUID.randomUUID().toString();
        exchange.getResponseHeaders().set("X-Request-Id", requestId);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        RespostaHttp resposta;
        try {
            resposta = router.despachar(exchange, gson);
        } catch (ApiException e) {
            resposta = erro(e.getStatus(), e.getCodigo(), e.getMessage(), exchange, requestId);
        } catch (IllegalArgumentException e) {
            resposta = erro(400, "REQUISICAO_INVALIDA", e.getMessage(), exchange, requestId);
        } catch (Exception e) {
            System.err.println("Falha interna na requisição " + requestId + ": " + e.getClass().getSimpleName());
            resposta = erro(500, "ERRO_INTERNO", "Erro interno do servidor", exchange, requestId);
        }
        try {
            if (resposta.localizacao() != null) exchange.getResponseHeaders().set("Location", resposta.localizacao());
            if (resposta.status() == 204) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                byte[] bytes = gson.toJson(resposta.corpo()).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                boolean head = exchange.getRequestMethod().equals("HEAD");
                exchange.sendResponseHeaders(resposta.status(), head ? -1 : bytes.length);
                if (!head) exchange.getResponseBody().write(bytes);
            }
        } finally {
            exchange.close();
        }
    }
    private RespostaHttp erro(int status, String codigo, String mensagem, HttpExchange exchange, String requestId) {
        return new RespostaHttp(status, new ErroDTO(status, codigo, mensagem,
                exchange.getRequestURI().getPath(), requestId), null);
    }
}
