package ml.justify.justify2.repo;

import ml.justify.justify2.file.DbFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<DbFile, String> {
  @Query("select f from files f where f.uploader = ?1")
  List<DbFile> getAllByUploader(String uploader);
}
