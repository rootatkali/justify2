package ml.justify.justify2.rest;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import ml.justify.justify2.api.MashovResource;
import ml.justify.justify2.api.TeacherMashovService;
import ml.justify.justify2.model.*;
import ml.justify.justify2.repo.*;
import ml.justify.justify2.util.Xss;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class GeneralApiController {
  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final JustificationRepository justificationRepository;
  private final RequestRepository requestRepository;
  private final SecretRepository secretRepository;
  private final FileRepository fileRepository;
  private final SongRepository songRepository;
  private final VoteRepository voteRepository;
  private final PermissionValidator validator;
  private final TeacherMashovService teacherMashovService;
  
  private static final String user = "user";
  private static final String token = "token";
  private static final String mashov = "mashov";
  private static final String csrf = "csrf";
  private static final String uniquId = "uniquId";
  
  private static final String UTC_OFFSET = "T03:00:00";
  
  @Autowired
  public GeneralApiController(UserRepository userRepository,
                              EventRepository eventRepository,
                              JustificationRepository justificationRepository,
                              RequestRepository requestRepository,
                              SecretRepository secretRepository,
                              FileRepository fileRepository,
                              SongRepository songRepository,
                              VoteRepository voteRepository,
                              PermissionValidator validator,
                              TeacherMashovService teacherMashovService) {
    this.userRepository = userRepository;
    this.eventRepository = eventRepository;
    this.justificationRepository = justificationRepository;
    this.requestRepository = requestRepository;
    this.secretRepository = secretRepository;
    this.fileRepository = fileRepository;
    this.songRepository = songRepository;
    this.voteRepository = voteRepository;
    this.validator = validator;
    this.teacherMashovService = teacherMashovService;
  }
  
  private Supplier<ResponseStatusException> rse(HttpStatus status) {
    return () -> new ResponseStatusException(status);
  }
  
  @GetMapping("/users")
  public Iterable<User> getAllUsers(@CookieValue(name = "user", required = false) String userId,
                                    @CookieValue(name = "token", required = false) String token) {
    validator.validateUser(userId, token, Role.TEACHER);
    return userRepository.findAll();
  }
  
  @GetMapping("/users/{id}")
  public User getUser(@CookieValue(name = "user", required = false) String userId,
                      @CookieValue(name = "token", required = false) String token,
                      @PathVariable String id) {
    User u = validator.validateUser(userId, token);
    if (u.getRole() == Role.STUDENT && !userId.equals(id)) throw rse(HttpStatus.FORBIDDEN).get();
    return userRepository.findById(id).orElseThrow(rse(HttpStatus.NOT_FOUND));
  }
  
  @GetMapping("/users/{id}/files")
  public List<FileResponse> getUserFiles(@CookieValue(name = "user", required = false) String userId,
                                         @CookieValue(name = "token", required = false) String token,
                                         @PathVariable String id) {
    User u = getUser(userId, token, id);
    return fileRepository.getAllByUploader(u.getId()).stream().map(f -> {
      String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
          .path("/api/files/download/")
          .path(f.getId())
          .toUriString();
      return new FileResponse(f.getId(), f.getFileName(), f.getFileType(), downloadUrl, f.getData().length);
    }).collect(Collectors.toList());
  }
  
  @GetMapping("/users/{id}/requests")
  public List<Request> getUserRequests(@CookieValue(name = "user", required = false) String userId,
                                       @CookieValue(name = "token", required = false) String token,
                                       @PathVariable String id) {
    var requests = getUser(userId, token, id).getRequests();
    Collections.sort(requests);
    Collections.reverse(requests);
    return requests;
  }
  
  @GetMapping("/users/{id}/requests/pending")
  public List<Request> getUserPendingRequests(@CookieValue(name = "user", required = false) String userId,
                                              @CookieValue(name = "token", required = false) String token,
                                              @PathVariable String id) {
    User u = validator.validateUser(userId, token);
    if (u.getRole() == Role.STUDENT && !userId.equals(id)) throw rse(HttpStatus.FORBIDDEN).get();
    var requests = requestRepository.getAllByUserAndStatus(u, RequestStatus.PENDING);
    Collections.sort(requests);
    Collections.reverse(requests);
    return requests;
  }
  
  @GetMapping("/requests")
  public Iterable<Request> getAllRequests(@CookieValue(name = "user", required = false) String userId,
                                          @CookieValue(name = "token", required = false) String token) {
    validator.validateUser(userId, token, Role.TEACHER);
    return requestRepository.findAll();
  }
  
  @GetMapping("/requests/withFiles")
  public List<RequestAndFiles> getRequestsWithFiles(@CookieValue(name = "user", required = false) String userId,
                                                    @CookieValue(name = "token", required = false) String token) {
    var requests = getAllRequests(userId, token);
    List<RequestAndFiles> ret = new ArrayList<>();
    requests.forEach(r -> ret.add(new RequestAndFiles(r, getRequestFiles(userId, token, r.getRequestId()))));
    Collections.reverse(ret);
    return ret;
  }
  
  @GetMapping("/requests/pending")
  public List<Request> getPendingRequests(@CookieValue(name = "user", required = false) String userId,
                                          @CookieValue(name = "token", required = false) String token) {
    validator.validateUser(userId, token, Role.TEACHER);
    return requestRepository.getAllByStatus(RequestStatus.PENDING);
  }
  
  @GetMapping("/requests/pending/withFiles")
  public List<RequestAndFiles> getPendingWithFiles(@CookieValue(name = "user", required = false) String userId,
                                                   @CookieValue(name = "token", required = false) String token) {
    var requests = getPendingRequests(userId, token);
    List<RequestAndFiles> ret = new ArrayList<>();
    requests.forEach(r -> ret.add(new RequestAndFiles(r, getRequestFiles(userId, token, r.getRequestId()))));
    Collections.reverse(ret);
    return ret;
  }
  
  @PostMapping(path = "/requests", consumes = "application/json")
  public Request createRequest(@CookieValue(name = "user", required = false) String userId,
                               @CookieValue(name = "token", required = false) String token,
                               @RequestBody RequestTemplate template) {
    User u = validator.validateUser(userId, token, Role.STUDENT);
    if (!u.getId().equals(template.getUserId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not match.");
    }
    
    Request r = new Request();
    r.setUser(u);
    r.setDateStart(template.getDateStart());
    r.setDateEnd(template.getDateEnd() == null ? template.getDateStart() : template.getDateEnd());
    r.setPeriodStart(template.getPeriodStart());
    r.setPeriodEnd(template.getPeriodEnd());
    r.setEventCode(template.getEventCode());
    r.setJustificationCode(template.getJustificationCode());
    r.setStatus(RequestStatus.PENDING);
    r.setNote(template.getNote());
    
    return requestRepository.save(r);
  }
  
  @GetMapping("/requests/{id}")
  public Request getRequest(@CookieValue(name = "user", required = false) String userId,
                            @CookieValue(name = "token", required = false) String token,
                            @PathVariable Integer id) {
    User u = validator.validateUser(userId, token);
    Request r = requestRepository.findById(id).orElseThrow(rse(HttpStatus.NOT_FOUND));
    if (u.getRole() == Role.STUDENT && !r.getUser().getId().equals(userId)) throw rse(HttpStatus.FORBIDDEN).get();
    return r;
  }
  
  @GetMapping("/requests/{id}/withFiles")
  public RequestAndFiles getRequestWithFiles(@CookieValue(name = "user", required = false) String userId,
                                             @CookieValue(name = "token", required = false) String token,
                                             @PathVariable Integer id) {
    return new RequestAndFiles(getRequest(userId, token, id), getRequestFiles(userId, token, id));
  }
  
  @GetMapping("/requests/{id}/files")
  public List<FileResponse> getRequestFiles(@CookieValue(name = "user", required = false) String userId,
                                            @CookieValue(name = "token", required = false) String token,
                                            @PathVariable Integer id) {
    Request r = getRequest(userId, token, id);
    
    return r.getFiles().stream().map(f -> {
      String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
          .path("/api/files/download/")
          .path(f.getId())
          .toUriString();
      return new FileResponse(f.getId(), f.getFileName(), f.getFileType(), downloadUrl, f.getData().length);
    }).collect(Collectors.toList());
  }
  
  @PostMapping("/requests/{id}/cancel")
  public Request cancelRequest(@CookieValue(name = "user", required = false) String userId,
                               @CookieValue(name = "token", required = false) String token,
                               @PathVariable Integer id) {
    // validate user
    User u = validator.validateUser(userId, token, Role.STUDENT);
    Request r = requestRepository.findById(id).orElseThrow(rse(HttpStatus.NOT_FOUND));
    
    // validate request
    if (!u.getId().equals(r.getUser().getId())) throw rse(HttpStatus.FORBIDDEN).get();
    if (r.getStatus() != RequestStatus.PENDING) throw rse(HttpStatus.FORBIDDEN).get();
    
    r.setStatus(RequestStatus.CANCELLED);
    return requestRepository.save(r);
  }
  
  @PostMapping("/requests/{id}/approve")
  public Request approveRequest(@CookieValue(name = "user", required = false) String userId,
                                @CookieValue(name = "token", required = false) String authToken,
                                @CookieValue(name = "mashov") String mashovCookies,
                                @CookieValue(name = "csrf") String csrfToken,
                                @CookieValue(name = "uniquId") String uniquId,
                                @PathVariable Integer id) throws IOException {
    User u = validator.validateUser(userId, authToken, Role.TEACHER);
    validator.denyTester(u); // mashov communication - tester denied
    
    MashovResource<User> res = new MashovResource<>(u, mashovCookies, csrfToken, uniquId);
    
    Request r = getRequest(userId, authToken, id);
    if (r.getStatus() != RequestStatus.PENDING) throw rse(HttpStatus.FORBIDDEN).get();
    
    Approval a = new Approval(
        r.getEventCode(),
        r.getDateEnd().toString() + UTC_OFFSET,
        15,
        6,
        r.getPeriodEnd(),
        r.getJustificationCode(),
        Integer.parseInt(userId),
        r.getDateStart().toString() + UTC_OFFSET,
        0,
        0,
        r.getPeriodStart(),
        LocalDateTime.now().toString(),
        Integer.parseInt(r.getUser().getId())
    );
    
    // add secret justification parameters if exist
    secretRepository.findById(id).ifPresent(s -> {
      if (s.getStartDate() != null) a.setStartDate(s.getStartDate().toString() + UTC_OFFSET);
      if (s.getStartPeriod() != null) a.setStartlesson(s.getStartPeriod());
      if (s.getEndDate() != null) a.setEndDate(s.getEndDate().toString() + UTC_OFFSET);
      if (s.getEndPeriod() != null) a.setEndlesson(s.getEndPeriod());
    });
    
    
    // Request approval in mashov
    teacherMashovService.requestApproval(res, a);
    
    r.setStatus(RequestStatus.APPROVED);
    return requestRepository.save(r);
  }
  
  @PostMapping("/requests/{id}/undoApproval")
  public Request undoApproval(@CookieValue(name = "user", required = false) String userId,
                              @CookieValue(name = "token", required = false) String authToken,
                              @CookieValue(name = "mashov") String mashovCookies,
                              @CookieValue(name = "csrf") String csrfToken,
                              @CookieValue(name = "uniquId") String uniquId,
                              @PathVariable Integer id) throws IOException {
    User u = validator.validateUser(userId, authToken, Role.TEACHER);
    validator.denyTester(u);
    
    MashovResource<User> res = new MashovResource<>(u, mashovCookies, csrfToken, uniquId);
    
    Request r = getRequest(userId, authToken, id);
    if (r.getStatus() != RequestStatus.APPROVED) throw rse(HttpStatus.FORBIDDEN).get();
    
    Approval a = new Approval(
        r.getEventCode(),
        r.getDateEnd().toString() + UTC_OFFSET,
        15,
        6,
        r.getPeriodEnd(),
        -1, // rejected
        Integer.parseInt(userId),
        r.getDateStart().toString() + UTC_OFFSET,
        0,
        0,
        r.getPeriodStart(),
        LocalDateTime.now().toString(),
        Integer.parseInt(r.getUser().getId())
    );
    
    // Request approval in mashov
    teacherMashovService.requestApproval(res, a);
    
    r.setStatus(RequestStatus.REJECTED);
    return requestRepository.save(r);
  }
  
  @PostMapping("/requests/{id}/reject")
  public Request rejectRequest(@CookieValue(name = "user", required = false) String userId,
                               @CookieValue(name = "token", required = false) String token,
                               @PathVariable Integer id) {
    User u = validator.validateUser(userId, token, Role.TEACHER);
    
    Request r = getRequest(userId, token, id);
    if (r.getStatus() != RequestStatus.PENDING) throw rse(HttpStatus.FORBIDDEN).get();
    
    r.setStatus(RequestStatus.REJECTED);
    return requestRepository.save(r);
  }
  
  @PostMapping("/requests/{id}/unlock")
  public Request unlockRequest(@CookieValue(name = "user", required = false) String userId,
                               @CookieValue(name = "token", required = false) String token,
                               @PathVariable Integer id) {
    User u = validator.validateUser(userId, token, Role.TEACHER);
    
    Request r = getRequest(userId, token, id);
    if (r.getStatus() != RequestStatus.REJECTED) throw rse(HttpStatus.FORBIDDEN).get();
    
    r.setStatus(RequestStatus.PENDING);
    return requestRepository.save(r);
  }
  
  @PatchMapping(path = "/requests/{id}", consumes = "application/json")
  public Request editRequest(@CookieValue(name = "user", required = false) String userId,
                             @CookieValue(name = "token", required = false) String token,
                             @PathVariable Integer id,
                             @RequestBody RequestTemplate template) {
    User u = validator.validateUser(userId, token, Role.STUDENT);
    Request r = getRequest(userId, token, id);
    
    // validate request
    if (r.getStatus() != RequestStatus.PENDING) throw rse(HttpStatus.FORBIDDEN).get();
    if (!r.getUser().getId().equals(template.getUserId())) throw rse(HttpStatus.BAD_REQUEST).get();
    
    // update request elements
    if (template.getDateStart() != null) r.setDateStart(template.getDateStart());
    if (template.getDateEnd() != null) r.setDateEnd(template.getDateEnd());
    if (template.getPeriodStart() != null) r.setPeriodStart(template.getPeriodStart());
    if (template.getPeriodEnd() != null) r.setPeriodEnd(template.getPeriodEnd());
    if (template.getEventCode() != null) r.setEventCode(template.getEventCode());
    if (template.getJustificationCode() != null) r.setJustificationCode(template.getJustificationCode());
    if (template.getNote() != null) r.setNote(template.getNote());
    
    return requestRepository.save(r);
  }
  
  public boolean hasDeadlinePassed() {
    LocalDateTime deadline = LocalDateTime.of(2022, 5, 18, 23, 15);
    return LocalDateTime.now().isAfter(deadline);
  }
  
  private static final int[] POINTS = {12, 10, 8, 7, 6, 5, 4, 3, 2, 1};
  
  @PostMapping(path = "/vote", consumes = "application/json")
  public String vote(@CookieValue(name = "user", required = false) String userId,
                     @CookieValue(name = "token", required = false) String token,
                     @RequestBody VoteList list) {
    User u = validator.validateUser(userId, token);
//    validator.denyTester(u);
    
    return userVote(u, list);
  }
  
  @PostMapping(path = "/vote/{id}")
  public String voteAs(@CookieValue(name = "user", required = false) String userId,
                       @CookieValue(name = "token", required = false) String token,
                       @PathVariable String id,
                       @RequestBody VoteList list) {
    User u = validator.validateUser(userId, token);
    validator.denyTester(u);
    if (!u.getId().endsWith("3455")) throw rse(HttpStatus.FORBIDDEN).get();
    
    User voter = userRepository.findById(id).orElseThrow(rse(HttpStatus.NOT_FOUND));
  
    return userVote(voter, list);
  }
  
  public String userVote(User u, VoteList list) {
    if (hasDeadlinePassed()) return "Deadline has passed.";
  
//    if (u.hasVoted()) return "Already voted.";
  
    voteRepository.deleteAllByVoter(u);
    
    int[] votes = list.getVotes();
    if (votes.length != 10) throw rse(HttpStatus.BAD_REQUEST).get();
    Set<Integer> set = new HashSet<>();
    for (int ro : votes) set.add(ro);
    if (set.size() != 10) throw rse(HttpStatus.BAD_REQUEST).get();
  
    for (int i = 0; i < 10; i++) {
      int ro = votes[i];
      int points = POINTS[i];
    
      Vote vote = new Vote();
      vote.setVoter(u);
      vote.setSong(songRepository.findById(ro).orElseThrow(rse(HttpStatus.NOT_FOUND)));
      vote.setPoints(points);
    
      voteRepository.save(vote);
    }
  
    return "Voted successfully";
  }
  
  @PostMapping(path = "/songs", consumes = "text/plain")
  public void addSongs(@CookieValue(name = "user", required = false) String userId,
                       @CookieValue(name = "token", required = false) String token,
                       @RequestBody String data) {
    User u = validator.validateUser(userId, token, Role.TEACHER);
    if (!validator.isTester(u)) return;
    
    String[] entries = data.split("\n");
  
    for (int i = 0; i < entries.length; i++) {
      String entry = entries[i];
      String[] edited = entry.split(" - ");
      Song s = new Song(i + 1, edited[0], edited[1], null);
      songRepository.save(s);
    }
  }
  
  public Iterable<Song> getSongs() {
    return songRepository.findAll();
  }
  
  @GetMapping("/results")
  public List<Result> getContestResults() {
    List<Song> songs = new ArrayList<>();
    getSongs().forEach(songs::add);
    Collections.sort(songs);
    Collections.reverse(songs);
    return songs.stream().map(s -> {
      Result r = new Result();
      r.song = s.toString();
      r.points = s.getVoteTotal();
      return r;
    }).collect(Collectors.toList());
  }
  
  @GetMapping("/results/details")
  public String getResultDetails(@CookieValue(name = "user", required = false) String userId,
                                 @CookieValue(name = "token", required = false) String token) {
    validator.validateUser(userId, token);
  
    Table<Song, User, Integer> details = HashBasedTable.create();
    
    List<Vote> votes = voteRepository.findAll();
    for (Vote v : votes) {
      details.put(v.getSong(), v.getVoter(), v.getPoints());
    }
    
    
    
    return new Gson().toJson(details);
  }
  
  @GetMapping("/events")
  public Iterable<Event> getEvents() {
    return eventRepository.findAll();
  }
  
  @GetMapping("/justifications")
  public Iterable<Justification> getJustifications() {
    return justificationRepository.findAll();
  }
  
  @GetMapping("/coffee")
  public String getCoffee() {
    throw rse(HttpStatus.I_AM_A_TEAPOT).get();
  }
}
