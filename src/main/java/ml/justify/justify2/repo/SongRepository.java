package ml.justify.justify2.repo;

import ml.justify.justify2.model.Song;
import org.springframework.data.repository.CrudRepository;

public interface SongRepository extends CrudRepository<Song, Integer> {
}