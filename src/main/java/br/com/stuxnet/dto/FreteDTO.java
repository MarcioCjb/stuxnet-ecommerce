package br.com.stuxnet.dto;

import java.math.BigDecimal;

public record FreteDTO(String cep, String uf, String modalidade, BigDecimal valor, int prazoDiasUteis) {}
