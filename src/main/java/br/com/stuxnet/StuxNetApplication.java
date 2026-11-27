package br.com.stuxnet;

import br.com.stuxnet.controller.FrontController;
import br.com.stuxnet.infra.ConfiguracaoAplicacao;
import br.com.stuxnet.infra.http.Router;
import br.com.stuxnet.service.ConsultaCep;
import br.com.stuxnet.service.ViaCepService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public final class StuxNetApplication {
    private StuxNetApplication() {}

    public static void main(String[] args) throws IOException {
        String configurada = System.getenv().getOrDefault("PORT", "8080");
        int porta = Integer.parseInt(configurada);
        if (porta < 1 || porta > 65535) throw new IllegalArgumentException("PORT deve estar entre 1 e 65535");
        HttpServer servidor = criarServidor(porta, new ViaCepService());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> servidor.stop(1)));
        servidor.start();
        System.out.println("StuxNet disponível em http://localhost:" + porta + "/api");
    }

    /** Inicializa o servidor e registra o Front Controller como único ponto de entrada. */
    public static HttpServer criarServidor(int porta, ConsultaCep cep) throws IOException {
        Router router = ConfiguracaoAplicacao.criarRouter(cep);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        servidor.createContext("/", new FrontController(router));
        servidor.setExecutor(null);
        return servidor;
    }
}
