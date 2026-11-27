package br.com.stuxnet.dto;
import java.math.BigDecimal;

public class FreteDTO {
    private String uf;
    private BigDecimal valorFrete;
    private int prazoDiasUteis;

    public String getUf() {
        return uf;
    }
    public void setUf(String uf) {
        this.uf = uf;
    }
    public BigDecimal getValorFrete() {
        return valorFrete;
    }
    public void setValorFrete(BigDecimal valor) {
        this.valorFrete = valor;
    }
    public int getPrazoDiasUteis() {
        return prazoDiasUteis;
    }
    public void setPrazoDiasUteis(int prazoDiasUteis) {
        this.prazoDiasUteis = prazoDiasUteis;
    }
}
