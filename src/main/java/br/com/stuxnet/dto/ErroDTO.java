package br.com.stuxnet.dto;

public record ErroDTO(int status, String codigo, String mensagem, String caminho, String requestId) {}
