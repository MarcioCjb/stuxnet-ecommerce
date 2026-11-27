package br.com.stuxnet.dto;

import br.com.stuxnet.model.Cliente;

public final class ClienteDTO {
    private ClienteDTO() {}
    public record Cadastro(String nome, String email, String senha) {}
    public record Atualizacao(String nome, String email) {}
    public record Resposta(long id, String nome, String email) {
        public static Resposta de(Cliente cliente) {
            return new Resposta(cliente.id(), cliente.nome(), cliente.email());
        }
    }
}
