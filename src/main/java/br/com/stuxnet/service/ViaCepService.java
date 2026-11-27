package br.com.stuxnet.service;

import br.com.stuxnet.dto.EnderecoDTO;
import br.com.stuxnet.exception.ApiException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class ViaCepService implements ConsultaCep {
    private final HttpClient cliente;
    private final URI baseUri;
    private final Gson gson = new Gson();

    public ViaCepService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
                URI.create("https://viacep.com.br/ws/"));
    }

    /** Permite testar o contrato HTTP com um servidor local, sem depender da internet. */
    public ViaCepService(HttpClient cliente, URI baseUri) {
        this.cliente = cliente;
        this.baseUri = baseUri;
    }

    public EnderecoDTO consultarCEP(String cep) {
        if (cep == null || !cep.matches("[0-9]{5}-?[0-9]{3}")) {
            throw new IllegalArgumentException("CEP inválido: informe oito dígitos, com hífen opcional");
        }
        String normalizado = cep.replace("-", "");
        HttpRequest pedido = HttpRequest.newBuilder(baseUri.resolve(normalizado + "/json/"))
                .timeout(Duration.ofSeconds(3)).GET().build();
        try {
            HttpResponse<String> resposta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) throw falhaExterna();
            EnderecoDTO endereco = gson.fromJson(resposta.body(), EnderecoDTO.class);
            if (endereco == null) throw falhaExterna();
            if (Boolean.TRUE.equals(endereco.getErro())) {
                throw ApiException.naoEncontrado("CEP " + normalizado);
            }
            if (endereco.getCep() == null || !endereco.getCep().replace("-", "").equals(normalizado)
                    || endereco.getUf() == null || !endereco.getUf().matches("[A-Z]{2}")
                    || endereco.getLocalidade() == null) throw falhaExterna();
            return endereco;
        } catch (HttpTimeoutException e) {
            throw new ApiException(504, "VIACEP_TIMEOUT", "Tempo limite ao consultar o ViaCEP");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw falhaExterna();
        } catch (IOException | JsonParseException e) {
            throw falhaExterna();
        }
    }

    private ApiException falhaExterna() {
        return new ApiException(502, "VIACEP_INDISPONIVEL", "Não foi possível obter uma resposta válida do ViaCEP");
    }
}
