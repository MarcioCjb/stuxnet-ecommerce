package br.com.stuxnet.support;

import br.com.stuxnet.service.ViaCepService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Dublê HTTP apenas de testes. A aplicação publicada sempre consulta o ViaCEP real. */
public final class ViaCepStub implements AutoCloseable {
    private final HttpServer servidor;
    public final AtomicInteger chamadas = new AtomicInteger();
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    public ViaCepStub() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/ws/", exchange -> {
            chamadas.incrementAndGet();
            String cep = exchange.getRequestURI().getPath().split("/")[2];
            String corpo = switch (cep) {
                case "01001000" -> "{\"cep\":\"01001-000\",\"logradouro\":\"Praça da Sé\",\"bairro\":\"Sé\",\"localidade\":\"São Paulo\",\"uf\":\"SP\"}";
                case "40010000" -> "{\"cep\":\"40010-000\",\"logradouro\":\"Avenida da França\",\"bairro\":\"Comércio\",\"localidade\":\"Salvador\",\"uf\":\"BA\"}";
                case "80010000" -> "{\"cep\":\"80010-000\",\"localidade\":\"Curitiba\",\"uf\":\"PR\"}";
                case "11111111" -> "<html>indisponível</html>";
                case "22222222" -> "null";
                case "33333333" -> "{}";
                case "55555555" -> "{\"erro\":\"true\"}";
                default -> "{\"erro\":true}";
            };
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(cep.equals("44444444") ? 503 : 200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        servidor.start();
    }
    public ViaCepService service() {
        return new ViaCepService(CLIENTE, URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/ws/"));
    }
    public void close() { servidor.stop(0); }
}
