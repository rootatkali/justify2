package ml.justify.justify2.rest;

import ml.justify.justify2.file.DbFile;
import ml.justify.justify2.file.FileStorageService;
import ml.justify.justify2.model.*;
import ml.justify.justify2.repo.*;
import ml.justify.justify2.util.Xss;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
  private final UserRepository userRepository;
  private final RequestRepository requestRepository;
  private final FileRepository fileRepository;
  private final FileStorageService fileService;
  private final PermissionValidator validator;

  private final Supplier<ResponseStatusException> notFound = () -> new ResponseStatusException(HttpStatus.NOT_FOUND);

  @Autowired
  public AdminApiController(UserRepository userRepository,
                            RequestRepository requestRepository,
                            FileRepository fileRepository,
                            FileStorageService fileService,
                            PermissionValidator validator,
                            AdminRepository adminRepository) {
    this.userRepository = userRepository;
    this.requestRepository = requestRepository;
    this.fileRepository = fileRepository;
    this.fileService = fileService;
    this.validator = validator;

    initSql(adminRepository);
  }

  private void initSql(AdminRepository repo) {
    if (repo.count() < 1) {
      Admin admin = new Admin();
      repo.save(admin);
    }
  }

  @GetMapping("/users")
  public Iterable<User> getUsers(@CookieValue(name = "admin", required = false) String token) {
    validator.validateAdmin(token);

    return userRepository.findAll();
  }

  @GetMapping("/users/{id}")
  public User getUser(@CookieValue(name = "admin", required = false) String token, @PathVariable String id) {
    validator.validateAdmin(token);

    return userRepository.findById(id).orElseThrow(notFound);
  }

  @GetMapping("/requests")
  public Iterable<Request> getRequests(@CookieValue(name = "admin", required = false) String token) {
    validator.validateAdmin(token);

    return requestRepository.findAll();
  }

  @GetMapping("/requests/{id}")
  public Request getRequest(@CookieValue(name = "admin", required = false) String token, @PathVariable Integer id) {
    validator.validateAdmin(token);

    return requestRepository.findById(id).orElseThrow(notFound);
  }

  private Request setStatus(Integer id, RequestStatus status) {
    Request r = requestRepository.findById(id).orElseThrow(notFound);
    r.setStatus(status);
    return requestRepository.save(r);
  }

  @PostMapping("/requests/{id}/approve")
  public Request setApproved(@CookieValue(name = "admin", required = false) String token, @PathVariable Integer id) {
    validator.validateAdmin(token);

    return setStatus(id, RequestStatus.APPROVED);
  }


  @PostMapping("/requests/{id}/reject")
  public Request setRejected(@CookieValue(name = "admin", required = false) String token, @PathVariable Integer id) {
    validator.validateAdmin(token);

    return setStatus(id, RequestStatus.REJECTED);
  }

  @PostMapping("/requests/{id}/cancel")
  public Request setCancelled(@CookieValue(name = "admin", required = false) String token, @PathVariable Integer id) {
    validator.validateAdmin(token);

    return setStatus(id, RequestStatus.CANCELLED);
  }

  @PostMapping("/requests/{id}/reset")
  public Request setPending(@CookieValue(name = "admin", required = false) String token, @PathVariable Integer id) {
    validator.validateAdmin(token);

    return setStatus(id, RequestStatus.PENDING);
  }

  @DeleteMapping("/requests/{id}")
  public void deleteRequest(@CookieValue(name = "admin", required = false) String token, @PathVariable Integer id) {
    validator.validateAdmin(token);

    requestRepository.deleteById(id);
  }

  @PatchMapping(path = "/requests/{id}", consumes = "application/json")
  public Request editRequest(@CookieValue(name = "admin", required = false) String token,
                             @PathVariable Integer id, @RequestBody RequestTemplate template) {
    Request r = getRequest(token, id);

    if (template.getDateStart() != null) r.setDateStart(template.getDateStart());
    if (template.getPeriodStart() != null) r.setPeriodStart(template.getPeriodStart());
    if (template.getDateEnd() != null) r.setDateEnd(template.getDateEnd());
    if (template.getPeriodEnd() != null) r.setPeriodEnd(template.getPeriodEnd());
    if (template.getJustificationCode() != null) r.setJustificationCode(template.getJustificationCode());
    if (template.getEventCode() != null) r.setEventCode(template.getEventCode());
    if (template.getNote() != null) r.setNote(template.getNote());

    return requestRepository.save(r);
  }

  @PatchMapping("/users/{id}/name")
  public User setUserName(@CookieValue(name = "admin", required = false) String token,
                          @PathVariable String id, @RequestParam String name) {
    User u = getUser(token, id);
    u.setDisplayName(Xss.deXss(name));
    return userRepository.save(u);
  }

  // files
  @GetMapping("/files")
  public List<FileResponse> getFiles(@CookieValue(name = "admin", required = false) String token) {
    validator.validateAdmin(token);

    return fileRepository.findAll().stream().map(f -> {
      String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
          .path("/api/files/download/")
          .path(f.getId())
          .toUriString();
      return new FileResponse(f.getId(), f.getFileName(), f.getFileType(), downloadUrl, f.getData().length);
    }).collect(Collectors.toList());
  }

  @GetMapping("/files/download/{id}")
  public ResponseEntity<Resource> downloadFile(@CookieValue(name = "admin", required = false) String token,
                                               @PathVariable String id) {
    validator.validateAdmin(token);
    DbFile file = fileService.getFile(id);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.getFileType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + '"')
        .body(new ByteArrayResource(file.getData()));
  }
}
