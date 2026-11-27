package br.com.stuxnet.service;

import br.com.stuxnet.dto.EnderecoDTO;

@FunctionalInterface
public interface ConsultaCep {
    EnderecoDTO consultarCEP(String cep);
}
