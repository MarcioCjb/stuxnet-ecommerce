package br.com.stuxnet.service;

import java.math.BigDecimal;
import java.util.List;
import br.com.stuxnet.dto.EnderecoDTO;
import br.com.stuxnet.dto.FreteDTO;

public class FreteService {
    private static final List<String> OUTROS_NORDESTE = List.of("SE", "PE", "AL", "PB", "RN", "CE", "PI", "MA");
    private static final List<String> SUDESTE = List.of("SP", "RJ", "MG", "ES");
    private ViaCepService viaCepService = new ViaCepService();

    public FreteDTO calcularFrete(String cep){
        EnderecoDTO endereco = viaCepService.consultarCEP(cep);
        String uf = endereco.getUf();
        FreteDTO frete = new FreteDTO();
        frete.setUf(uf);
        if (uf.equals("BA")) {
            frete.setValorFrete(new BigDecimal("22.90"));
            frete.setPrazoDiasUteis(2);
        } else if (OUTROS_NORDESTE.contains(uf)) {
            frete.setValorFrete(new BigDecimal("22.90"));
            frete.setPrazoDiasUteis(4);
        } else if (SUDESTE.contains(uf)) {
            frete.setValorFrete(new BigDecimal("34.90"));
            frete.setPrazoDiasUteis(6);
        } else {
            frete.setValorFrete(new BigDecimal("49.90"));
            frete.setPrazoDiasUteis(9);
        }
        return frete;
    }
}
