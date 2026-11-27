package br.com.stuxnet.service;

import br.com.stuxnet.dto.EnderecoDTO;
import br.com.stuxnet.dto.FreteDTO;
import br.com.stuxnet.frete.*;
import java.util.Set;

public final class FreteService {
    private final ConsultaCep consultaCep;

    public FreteService(ConsultaCep consultaCep) { this.consultaCep = consultaCep; }
    public FreteDTO consultar(String cep, String modalidade) {
        EnderecoDTO endereco = consultaCep.consultarCEP(cep);
        return calcular(endereco.getCep(), endereco.getUf(), modalidade);
    }
    public FreteDTO calcular(String cep, String uf, String modalidade) {
        String tipo = modalidade == null ? "NORMAL" : modalidade;
        Validacao.exigir(tipo.equals("NORMAL") || tipo.equals("EXPRESSO"), "modalidade deve ser NORMAL ou EXPRESSO");
        FreteStrategy estrategia = uf.equals("BA") ? new FreteBahia()
                : Set.of("SP", "RJ", "MG", "ES").contains(uf) ? new FreteSudeste() : new FreteOutrasRegioes();
        if (tipo.equals("EXPRESSO")) estrategia = new FreteExpressoDecorator(estrategia);
        FreteStrategy.Cotacao cotacao = estrategia.calcular();
        return new FreteDTO(cep, uf, tipo, cotacao.valor(), cotacao.prazoDiasUteis());
    }
}
