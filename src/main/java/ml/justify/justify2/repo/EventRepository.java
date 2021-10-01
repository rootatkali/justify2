package ml.justify.justify2.repo;

import ml.justify.justify2.model.Event;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, Integer> {
}
