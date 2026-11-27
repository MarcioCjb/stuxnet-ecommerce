package br.com.stuxnet.infra.http;

import br.com.stuxnet.exception.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Requisicao {
    private static final int MAX_BODY = 100 * 1024;
    private final HttpExchange exchange;
    private final Map<String, String> parametros;
    private final Gson gson;

    Requisicao(HttpExchange exchange, Map<String, String> parametros, Gson gson) {
        this.exchange = exchange;
        this.parametros = parametros;
        this.gson = gson;
    }
    public String parametro(String nome) { return parametros.get(nome); }
    public long id(String nome) {
        try {
            long id = Long.parseLong(parametro(nome));
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) { throw new IllegalArgumentException(nome + " deve ser um inteiro positivo"); }
    }
    public String query(String nome) {
        String query = exchange.getRequestURI().getRawQuery();
        Map<String, String> valores = new LinkedHashMap<>();
        if (query != null && !query.isEmpty()) {
            for (String par : query.split("&")) {
                String[] partes = par.split("=", 2);
                String chave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                String valor = partes.length == 2 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
                if (valores.putIfAbsent(chave, valor) != null) {
                    throw new IllegalArgumentException("Parâmetro duplicado: " + chave);
                }
            }
        }
        return valores.get(nome);
    }
    public <T> T corpo(Class<T> tipo) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].strip().equalsIgnoreCase("application/json")) {
            throw new ApiException(415, "TIPO_NAO_SUPORTADO", "Use Content-Type: application/json");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
        if (bytes.length > MAX_BODY) throw new ApiException(413, "CORPO_MUITO_GRANDE", "Limite do corpo: 100 KiB");
        String json = new String(bytes, StandardCharsets.UTF_8);
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) throw new IllegalArgumentException("Envie um objeto JSON");
            reader.skipValue();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Envie apenas um objeto JSON");
            T dto = gson.fromJson(json, tipo);
            if (dto == null) throw new IllegalArgumentException("Corpo JSON obrigatório");
            return dto;
        } catch (JsonParseException | IOException | IllegalStateException e) {
            throw new IllegalArgumentException("JSON inválido ou campos com tipos incompatíveis");
        }
    }
}
