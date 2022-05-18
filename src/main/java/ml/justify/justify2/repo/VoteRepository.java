package ml.justify.justify2.repo;

import ml.justify.justify2.model.User;
import ml.justify.justify2.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface VoteRepository extends JpaRepository<Vote, String> {
  
  @Transactional
  @Modifying
  @Query("delete from Vote v where v.voter = ?1")
  void deleteAllByVoter(User u);
}