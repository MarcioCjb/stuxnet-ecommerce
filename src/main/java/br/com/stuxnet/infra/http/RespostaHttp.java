package br.com.stuxnet.infra.http;

public record RespostaHttp(int status, Object corpo, String localizacao) {
    public static RespostaHttp ok(Object dto) { return new RespostaHttp(200, dto, null); }
    public static RespostaHttp criado(Object dto, String localizacao) { return new RespostaHttp(201, dto, localizacao); }
    public static RespostaHttp semConteudo() { return new RespostaHttp(204, null, null); }
}
