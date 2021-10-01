package ml.justify.justify2.repo;

import ml.justify.justify2.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {
}
