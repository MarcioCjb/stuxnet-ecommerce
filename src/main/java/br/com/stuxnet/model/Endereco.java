package br.com.stuxnet.model;

public record Endereco(long id, String cep, String logradouro, String numero,
                       String complemento, String bairro, String cidade, String uf) {}
