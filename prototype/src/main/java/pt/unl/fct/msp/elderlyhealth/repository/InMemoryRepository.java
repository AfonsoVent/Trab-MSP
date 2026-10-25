package pt.unl.fct.msp.elderlyhealth.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Repositório em memória (protótipo). Para passar a base de dados basta trocar
 * estas classes por interfaces Spring Data JPA com os mesmos métodos.
 */
public abstract class InMemoryRepository<T extends Entidade> {

    private final Map<Long, T> dados = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    public T guardar(T entidade) {
        if (entidade.getId() == null) {
            entidade.setId(sequencia.incrementAndGet());
        }
        dados.put(entidade.getId(), entidade);
        return entidade;
    }

    public Optional<T> procurar(Long id) {
        return Optional.ofNullable(dados.get(id));
    }

    public List<T> todos() {
        return dados.values().stream().sorted(Comparator.comparing(Entidade::getId)).toList();
    }

    public List<T> filtrar(Predicate<T> condicao) {
        return todos().stream().filter(condicao).toList();
    }
}
