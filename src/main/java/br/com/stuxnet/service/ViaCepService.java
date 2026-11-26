package br.com.stuxnet.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.google.gson.Gson;
import br.com.stuxnet.dto.EnderecoDTO;

public class ViaCepService {
    public EnderecoDTO consultarEnderecoDTO(String cep){
        if(cep == null){
            throw new IllegalArgumentException("CEP não pode ficar vazio");
        }
        
        String cepApenasNumero = cep.replaceAll("\\D", "");

        if(cepApenasNumero.length() != 8){
            throw new IllegalArgumentException("CEP Inválido! Deve conter exatos 8 dígitos");
        }

        String json = buscarViaCep(cepApenasNumero);

        Gson gson = new Gson();
        EnderecoDTO endereco = gson.fromJson(json, EnderecoDTO.class);

        if(endereco.getErro() != null && endereco.getErro()){
            throw new IllegalArgumentException("O CEP " + cepApenasNumero + " não foi encontrado");
        }

        return endereco;
    }

    private String buscarViaCep(String cep){
        try{
            HttpClient cliente = HttpClient.newHttpClient();
            HttpRequest pedido = HttpRequest.newBuilder().uri(URI.create("https://viacep.com.br/ws" + cep + "/json/")).timeout(Duration.ofSeconds(3)).GET().build();

            HttpResponse<String> resposta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());

            if(resposta.statusCode() == 400){
                throw new IllegalArgumentException("O CEP " + cep + " é inválido");
            }
            if(resposta.statusCode() == 300){
                throw new IllegalArgumentException("A ViaCEP respondeu com o status " + resposta.statusCode());
            }

            return resposta.body();
        }catch (IOException | InterruptedException e){
            throw new IllegalArgumentException("Não foi possivel acessar o ViaCEP", e);
        }
    }
}