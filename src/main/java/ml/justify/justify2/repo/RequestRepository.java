package ml.justify.justify2.repo;

import ml.justify.justify2.model.Request;
import ml.justify.justify2.model.RequestStatus;
import ml.justify.justify2.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RequestRepository extends CrudRepository<Request, Integer> {
  List<Request> getAllByUser(User user);

  List<Request> getAllByStatus(RequestStatus status);

  List<Request> getAllByUserAndStatus(User user, RequestStatus status);
}
