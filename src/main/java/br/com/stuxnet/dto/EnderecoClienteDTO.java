package br.com.stuxnet.dto;

import br.com.stuxnet.model.Endereco;

public final class EnderecoClienteDTO {
    private EnderecoClienteDTO() {}
    public record Requisicao(String cep, String numero, String complemento) {}
    public record Resposta(long id, String cep, String logradouro, String numero,
                           String complemento, String bairro, String cidade, String uf) {
        public static Resposta de(Endereco endereco) {
            return new Resposta(endereco.id(), endereco.cep(), endereco.logradouro(), endereco.numero(),
                    endereco.complemento(), endereco.bairro(), endereco.cidade(), endereco.uf());
        }
    }
}
