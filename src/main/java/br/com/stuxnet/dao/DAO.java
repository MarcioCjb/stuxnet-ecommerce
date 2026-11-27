package br.com.stuxnet.dao;

import br.com.stuxnet.model.Identificavel;
import java.util.List;
import java.util.Optional;

/** Contrato de persistência: os serviços desconhecem a implementação do armazenamento. */
public interface DAO<T extends Identificavel> {
    long proximoId();
    T salvar(T entidade);
    Optional<T> buscar(long id);
    List<T> listar();
    void excluir(long id);
}
