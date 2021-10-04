package ml.justify.justify2.rest;

import ml.justify.justify2.file.DbFile;
import ml.justify.justify2.file.FileStorageService;
import ml.justify.justify2.model.*;
import ml.justify.justify2.repo.FileRepository;
import ml.justify.justify2.repo.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileApiController {
  private final FileStorageService fileService;
  private final PermissionValidator validator;
  private final FileRepository fileRepository;
  private final RequestRepository requestRepository;
  
  @Autowired
  public FileApiController(FileStorageService fileService,
                           PermissionValidator validator,
                           FileRepository fileRepository,
                           RequestRepository requestRepository) {
    this.fileService = fileService;
    this.validator = validator;
    this.fileRepository = fileRepository;
    this.requestRepository = requestRepository;
  }
  
  private FileResponse storeAfterVerify(Request r, MultipartFile file) throws IOException {
    DbFile dbFile = fileService.storeFileWithRequest(file, r);
    
    String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/files/download/")
        .path(dbFile.getId())
        .toUriString();
    
    return new FileResponse(dbFile.getId(), dbFile.getFileName(), downloadUrl, file.getContentType(), file.getSize());
  }
  
  @PostMapping("/request/{id}")
  public FileResponse uploadFile(@CookieValue(name = "user") String userId,
                                 @CookieValue(name = "token") String token,
                                 @PathVariable Integer id,
                                 @RequestParam(name = "file") MultipartFile file) throws IOException {
    User u = validator.validateUser(userId, token, Role.STUDENT);
    Request r = requestRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (r.getStatus() != RequestStatus.PENDING) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (!userId.equals(r.getUser().getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    
    return storeAfterVerify(r, file);
  }
  
  @PostMapping("/request/{id}/multiple")
  public List<FileResponse> uploadFiles(@CookieValue(name = "user") String userId,
                                        @CookieValue(name = "token") String token,
                                        @PathVariable Integer id,
                                        @RequestParam(name = "files") MultipartFile[] files) throws IOException {
    User u = validator.validateUser(userId, token, Role.STUDENT);
    Request r = requestRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!userId.equals(r.getUser().getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (r.getStatus() != RequestStatus.PENDING) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    
    List<FileResponse> responses = new ArrayList<>();
    for (MultipartFile mf : files) {
      responses.add(storeAfterVerify(r, mf));
    }
    return responses;
  }
  
  @GetMapping("/download/{id}")
  public ResponseEntity<Resource> downloadFile(@CookieValue(name = "user") String userId,
                                               @CookieValue(name = "token") String token,
                                               @PathVariable String id) {
    User u = validator.validateUser(userId, token);
    validator.denyTester(u);
    
    DbFile file = fileService.getFile(id);
    
    if (u.getRole() == Role.STUDENT && !userId.equals(file.getUploader())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.getFileType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + '"')
        .body(new ByteArrayResource(file.getData()));
  }
  
  @PostMapping("/{id}/rename")
  public FileResponse renameFile(@CookieValue(name = "user") String userId,
                                 @CookieValue(name = "token") String token,
                                 @PathVariable String id,
                                 @RequestParam String name) {
    User u = validator.validateUser(userId, token);
    DbFile dbFile = fileService.getFile(id);
    
    if (u.getRole() == Role.STUDENT && !userId.equals(dbFile.getUploader())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    
    dbFile = fileService.renameFile(id, name);
    
    String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/files/download/")
        .path(dbFile.getId())
        .toUriString();
    
    return new FileResponse(dbFile.getId(), dbFile.getFileName(), downloadUrl, dbFile.getFileName(), dbFile.getData().length);
  }
  
  @DeleteMapping("/{id}")
  public void deleteFile(@CookieValue(name = "user") String userId,
                         @CookieValue(name = "token") String token,
                         @PathVariable String id) {
    User u = validator.validateUser(userId, token);
    DbFile file = fileService.getFile(id);
    
    if (u.getRole() == Role.STUDENT && !userId.equals(file.getUploader())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    
    fileRepository.delete(file);
  }
}
