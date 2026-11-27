package br.com.stuxnet.infra.http;

import br.com.stuxnet.command.Command;
import br.com.stuxnet.exception.ApiException;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Router {
    private record Rota(String metodo, Pattern caminho, List<String> parametros, Command command) {}
    private final List<Rota> rotas = new ArrayList<>();

    public void registrar(String metodo, String caminho, Command command) {
        Matcher matcher = Pattern.compile("\\{(\\w+)\\}").matcher(caminho);
        StringBuilder regex = new StringBuilder("^");
        List<String> nomes = new ArrayList<>();
        int inicio = 0;
        while (matcher.find()) {
            regex.append(Pattern.quote(caminho.substring(inicio, matcher.start()))).append("([^/]+)");
            nomes.add(matcher.group(1));
            inicio = matcher.end();
        }
        regex.append(Pattern.quote(caminho.substring(inicio))).append("/?$");
        rotas.add(new Rota(metodo, Pattern.compile(regex.toString()), List.copyOf(nomes), command));
    }

    public RespostaHttp despachar(HttpExchange exchange, Gson gson) throws IOException {
        Set<String> permitidos = new LinkedHashSet<>();
        for (Rota rota : rotas) {
            Matcher matcher = rota.caminho().matcher(exchange.getRequestURI().getPath());
            if (!matcher.matches()) continue;
            permitidos.add(rota.metodo());
            if (!rota.metodo().equals(exchange.getRequestMethod())) continue;
            Map<String, String> parametros = new LinkedHashMap<>();
            for (int i = 0; i < rota.parametros().size(); i++) parametros.put(rota.parametros().get(i), matcher.group(i + 1));
            return rota.command().executar(new Requisicao(exchange, parametros, gson));
        }
        if (!permitidos.isEmpty()) {
            exchange.getResponseHeaders().set("Allow", String.join(", ", permitidos));
            throw new ApiException(405, "METODO_NAO_PERMITIDO", "Método não permitido para esta rota");
        }
        throw ApiException.naoEncontrado("Rota");
    }
}
