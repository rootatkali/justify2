package ml.justify.justify2.rest;

import de.faceco.mashovapi.components.Behave;
import de.faceco.mashovapi.components.Login;
import lombok.extern.slf4j.Slf4j;
import ml.justify.justify2.api.MashovResource;
import ml.justify.justify2.api.StudentMashovService;
import ml.justify.justify2.config.MashovConfig;
import ml.justify.justify2.model.*;
import ml.justify.justify2.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController()
@RequestMapping("/api/students")
public class StudentApiController {
  private final AuthRepository authRepository;
  private final PermissionValidator validator;
  private final StudentMashovService mashovService;
  private final MashovConfig mashovConfig;

  private static final String user = "user";
  private static final String token = "token";
  private static final String mashov = "mashov";
  private static final String csrf = "csrf";
  private static final String uniquId = "uniquId";

  @Autowired
  public StudentApiController(AuthRepository authRepository,
                              PermissionValidator validator,
                              StudentMashovService mashovService,
                              MashovConfig mashovConfig) {
    this.authRepository = authRepository;
    this.validator = validator;
    this.mashovService = mashovService;
    this.mashovConfig = mashovConfig;
  }

  @PostMapping("/login")
  public User login(@RequestParam String username, @RequestParam String password,
                    @CookieValue(name = "uniquId", required = false) String uniqu,
                    HttpServletResponse res) throws IOException {
    // Login mashov
    Login l = new Login(mashovService.getSchool(), mashovConfig.getYear(), username, password);
    MashovResource<User> resource = mashovService.login(l, uniqu);

    if (resource.getValue().getRole() != Role.STUDENT) { // check for role
      mashovService.logout(resource);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teachers cannot log in as students.");
    }

    // Generate token
    Auth auth = new Auth(resource.getValue().getId());
    auth.generateToken();
    authRepository.save(auth);

    // Store token and id cookies
    Cookie userId = new Cookie(user, resource.getValue().getId());
    userId.setMaxAge(Integer.MAX_VALUE);
    Cookie tokenCookie = new Cookie(token, auth.getToken());
    tokenCookie.setMaxAge(3600);
    tokenCookie.setHttpOnly(true);
    Arrays.asList(userId, tokenCookie).forEach(c -> {
      c.setPath("/");
      c.setSecure(true);
      res.addCookie(c);
    });

    // store mashov resource cookies
    Cookie mashovCookie = new Cookie(mashov, resource.getCookies());
    mashovCookie.setMaxAge(3600);
    Cookie csrfCookie = new Cookie(csrf, resource.getCsrfToken());
    csrfCookie.setMaxAge(3600);
    Cookie uniquCookie = new Cookie(uniquId, resource.getUniquId());
    uniquCookie.setMaxAge(Integer.MAX_VALUE);
    Arrays.asList(mashovCookie, csrfCookie, uniquCookie).forEach(c -> {
      c.setHttpOnly(true);
      c.setSecure(true);
      c.setPath("/");
      res.addCookie(c);
    });

    return resource.getValue();
  }

  @PostMapping("/logout")
  public void logout(@CookieValue(name = "user", required = false) String userId,
                     @CookieValue(name = "token", required = false) String authToken,
                     @CookieValue(name = "mashov", required = false) String mashovCookies,
                     @CookieValue(name = "csrf", required = false) String csrfToken,
                     @CookieValue(name = "uniquId", required = false) String uniquId,
                     HttpServletResponse res) throws IOException {

    User u = validator.validateUser(userId, authToken, Role.STUDENT);
    MashovResource<User> req = new MashovResource<>(u, mashovCookies, csrfToken, uniquId);

    // logout mashov
    int logoutCode = mashovService.logout(req).getValue();
    if (logoutCode != 200) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

    // clear token
    authRepository.deleteById(u.getId());

    // delete cookies (except for uniquId)
    Cookie userCookie = new Cookie(user, "");
    Cookie authCookie = new Cookie(token, "");
    Cookie mashovCookie = new Cookie(mashov, "");
    Cookie csrfCookie = new Cookie(csrf, "");
    Arrays.asList(userCookie, authCookie, mashovCookie, csrfCookie).forEach(c -> {
      c.setMaxAge(0);
      res.addCookie(c);
    });
  }

  @GetMapping("/behaves")
  public List<Behave> getBehaves(@CookieValue(name = "user", required = false) String userId,
                                 @CookieValue(name = "token", required = false) String authToken,
                                 @CookieValue(name = "mashov", required = false) String mashovCookies,
                                 @CookieValue(name = "csrf", required = false) String csrfToken,
                                 @CookieValue(name = "uniquId", required = false) String uniquId) throws IOException {

    User u = validator.validateUser(userId, authToken, Role.STUDENT);
    MashovResource<User> req = new MashovResource<>(u, mashovCookies, csrfToken, uniquId);

    return mashovService.getBehaves(req);
  }

  @GetMapping("/me")
  public User getMe(@CookieValue(name = "user", required = false) String userId,
                    @CookieValue(name = "token", required = false) String token) {
    return validator.validateUser(userId, token, Role.STUDENT);
  }

  @GetMapping("/myRequests")
  public List<Request> getMyRequests(@CookieValue(name = "user", required = false) String userId,
                                     @CookieValue(name = "token", required = false) String token) {
    User me = validator.validateUser(userId, token, Role.STUDENT);
    List<Request> requests = me.getRequests();
    if (requests == null) return List.of(); // Avoid jackson null serialization error
    Collections.sort(requests);
    Collections.reverse(requests);
    return requests;
  }

  @GetMapping("/myRequests/pending")
  public List<Request> getMyPendingRequests(@CookieValue(name = "user", required = false) String userId,
                                            @CookieValue(name = "token", required = false) String token) {
    List<Request> requests = getMyRequests(userId, token);
    return requests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).collect(Collectors.toList());
  }

  @GetMapping("/myRequests/withFiles")
  public List<RequestAndFiles> getMyRequestsWithFiles(@CookieValue(name = "user", required = false) String userId,
                                                      @CookieValue(name = "token", required = false) String token) {
    List<Request> requests = getMyRequests(userId, token);
    List<RequestAndFiles> ret = new ArrayList<>();

    requests.forEach(r -> ret.add(new RequestAndFiles(r, r.getFiles().stream().map(f -> {
      String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
          .path("/api/files/download/")
          .path(f.getId())
          .toUriString();
      return new FileResponse(f.getId(), f.getFileName(), f.getFileType(), downloadUrl, f.getData().length);
    }).collect(Collectors.toList()))));

    return ret;
  }
}
