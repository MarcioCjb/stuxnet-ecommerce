package br.com.stuxnet.model;

import java.util.List;

public record Cliente(long id, String nome, String email, String senhaHash,
                      List<Endereco> enderecos) implements Identificavel {
    public Cliente { enderecos = List.copyOf(enderecos); }
}
