package ml.justify.justify2.file;

import ml.justify.justify2.model.Request;
import ml.justify.justify2.repo.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
public class FileStorageService {
  private final FileRepository fileRepository;

  @Autowired
  public FileStorageService(FileRepository fileRepository) {
    this.fileRepository = fileRepository;
  }

  public DbFile storeFile(String userId, MultipartFile file) throws IOException {
    String name = StringUtils.cleanPath(file.getOriginalFilename());
    if (name.contains("..")) throw new IOException();

    DbFile dbFile = new DbFile(name, file.getContentType(), file.getBytes(), userId);

    return fileRepository.save(dbFile);
  }

  public DbFile updateFileRequest(DbFile file, Request request) {
    file.setRequest(request);
    return fileRepository.save(file);
  }
  
  public DbFile renameFile(String id, String name) {
    name = StringUtils.cleanPath(name);
    if (name.contains("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    
    DbFile file = getFile(id);
    file.setFileName(name);
    return fileRepository.save(file);
  }

  public DbFile storeFileWithRequest(MultipartFile file, Request request) throws IOException {
    DbFile df = storeFile(request.getUser().getId(), file);
    return updateFileRequest(df, request);
  }

  public DbFile getFile(String id) {
    return fileRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
