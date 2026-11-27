package br.com.stuxnet.exception;

public class ApiException extends IllegalArgumentException {
    private final int status;
    private final String codigo;

    public ApiException(int status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }
    public int getStatus() { return status; }
    public String getCodigo() { return codigo; }
    public static ApiException naoEncontrado(String recurso) {
        return new ApiException(404, "NAO_ENCONTRADO", recurso + " não encontrado");
    }
    public static ApiException conflito(String mensagem) { return new ApiException(409, "CONFLITO", mensagem); }
}
