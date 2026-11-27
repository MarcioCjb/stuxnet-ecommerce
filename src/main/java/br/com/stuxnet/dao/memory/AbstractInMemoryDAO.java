package br.com.stuxnet.dao.memory;

import br.com.stuxnet.model.Identificavel;
import br.com.stuxnet.dao.DAO;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractInMemoryDAO<T extends Identificavel> implements DAO<T> {
    private final ConcurrentHashMap<Long, T> dados = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    public long proximoId() { return sequencia.incrementAndGet(); }
    public T salvar(T entidade) { dados.put(entidade.id(), entidade); return entidade; }
    public Optional<T> buscar(long id) { return Optional.ofNullable(dados.get(id)); }
    public List<T> listar() {
        return dados.values().stream().sorted(Comparator.comparingLong(Identificavel::id)).toList();
    }
    public void excluir(long id) { dados.remove(id); }
}
