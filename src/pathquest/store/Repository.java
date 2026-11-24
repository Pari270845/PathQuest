package pathquest.store;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface Repository<T, ID> {
    ID save(T item) throws IOException;

    Optional<T> findById(ID id) throws IOException;

    List<T> findAll() throws IOException;

    void update(ID id, T item) throws IOException;

    void delete(ID id) throws IOException;
}
