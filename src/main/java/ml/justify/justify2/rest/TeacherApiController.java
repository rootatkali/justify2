package ml.justify.justify2.rest;

import de.faceco.mashovapi.components.Login;
import ml.justify.justify2.api.MashovResource;
import ml.justify.justify2.api.TeacherMashovService;
import ml.justify.justify2.config.MashovConfig;
import ml.justify.justify2.model.Auth;
import ml.justify.justify2.model.Role;
import ml.justify.justify2.model.User;
import ml.justify.justify2.repo.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@RestController
@RequestMapping("/api/teachers")
public class TeacherApiController {
  private final AuthRepository authRepository;
  private final PermissionValidator validator;
  private final TeacherMashovService mashovService;
  private final MashovConfig mashovConfig;

  private static final String user = "user";
  private static final String token = "token";
  private static final String mashov = "mashov";
  private static final String csrf = "csrf";
  private static final String uniquId = "uniquId";

  @Autowired
  public TeacherApiController(AuthRepository authRepository,
                              PermissionValidator validator,
                              TeacherMashovService mashovService,
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

    // handle tester account
    if (resource.getValue().getId().equals(mashovConfig.getTestAccountUsername())) {
      Auth auth = new Auth(resource.getValue().getId());
      auth.generateToken();
      authRepository.save(auth);

      // Store token and id cookies for tester account
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

      return resource.getValue();
    }

    if (resource.getValue().getRole() != Role.TEACHER) { // check for role
      mashovService.logout(resource);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Students cannot log in as teachers.");
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

    User u = validator.validateUser(userId, authToken, Role.TEACHER);
    MashovResource<User> req = new MashovResource<>(u, mashovCookies, csrfToken, uniquId);

    // logout mashov
    int logoutCode = mashovService.logout(req).getValue();
    if (logoutCode != HttpStatus.OK.value()) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

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
}
