package ml.justify.justify2.repo;

import ml.justify.justify2.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, String> {
}