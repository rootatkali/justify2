package ml.justify.justify2.content;

import de.faceco.mashovapi.components.Behave;
import lombok.extern.slf4j.Slf4j;
import ml.justify.justify2.config.MashovConfig;
import ml.justify.justify2.model.Role;
import ml.justify.justify2.model.User;
import ml.justify.justify2.repo.*;
import ml.justify.justify2.rest.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class ContentController {
  private final PermissionValidator validator;
  private final StudentApiController studentApi;
  private final GeneralApiController generalApi;
  private final UserRepository userRepository;
  private final MashovConfig config;

  private final Tab[] studentTabs = {
      new Tab("Dashboard", "/"),
      new Tab("Events", "/events"),
      new Tab("Requests", "/requests"),
      new Tab("Files", "/files")
  };

  @Autowired
  public ContentController(PermissionValidator validator,
                           StudentApiController studentApi,
                           GeneralApiController generalApi,
                           UserRepository userRepository,
                           MashovConfig config) {
    this.validator = validator;
    this.studentApi = studentApi;
    this.generalApi = generalApi;
    this.userRepository = userRepository;
    this.config = config;
  }

  private List<Behave> tryGetBehaves(String userId,
                                     String token,
                                     String mashovCookies,
                                     String csrfToken,
                                     String uniquId) {
    try {
      return studentApi.getBehaves(userId, token, mashovCookies, csrfToken, uniquId);
    } catch (IOException e) {
      return List.of();
    }
  }

  @GetMapping("/")
  public String getHomePage(@CookieValue(name = "user", required = false) String userId,
                            @CookieValue(name = "token", required = false) String token,
                            @CookieValue(name = "mashov", required = false) String mashovCookies,
                            @CookieValue(name = "csrf", required = false) String csrfToken,
                            @CookieValue(name = "uniquId", required = false) String uniquId,
                            Model model) {
    try {
      User u = validator.validateUser(userId, token);
      return u.getRole() == Role.STUDENT
          ? getStudentDashboardModel(u, model, token, mashovCookies, csrfToken, uniquId)
          : getAllRequests(userId, token, model);

    } catch (ResponseStatusException rse) {
      if (userId != null && !userId.isEmpty() && rse.getStatus() == HttpStatus.UNAUTHORIZED) {
        Role role = userRepository.findById(userId).orElseThrow().getRole();
        return role == Role.STUDENT ? getStudentLogin() : getTeacherLogin();
      }
      return getStudentLogin();
    }
  }

  private void makeModel(Model m, User u, Map<String, ?> attributes) {
    m.addAttribute("user", u);
    m.addAttribute("events", generalApi.getEvents());
    m.addAttribute("justifications", generalApi.getJustifications());
    m.addAllAttributes(attributes);
  }

  private String getStudentDashboardModel(User user,
                                          Model model,
                                          String token,
                                          String mashovCookies,
                                          String csrfToken,
                                          String uniquId) {
    makeModel(model, user, Map.of(
        "requests", studentApi.getMyPendingRequests(user.getId(), token),
        "behaves", tryGetBehaves(user.getId(), token, mashovCookies, csrfToken, uniquId),
        "tabs", studentTabs
    ));
    return "student_dashboard";
  }

  @GetMapping("/teacherLogin")
  public String getTeacherLogin() {
    return "teacher_login";
  }

  @GetMapping("/studentLogin")
  public String getStudentLogin() {
    return "student_login";
  }

  private String tryGetStudentPage(String userId, String token, String view, Model model, Map<String, ?> attributes) {
    try {
      User u = validator.validateUser(userId, token, Role.STUDENT);
      model.addAttribute("tabs", studentTabs);
      makeModel(model, u, attributes);
    } catch (ResponseStatusException e) {
      if (e.getStatus() == HttpStatus.FORBIDDEN) throw e;
      if (e.getStatus() == HttpStatus.UNAUTHORIZED) return getStudentLogin();
      throw e;
    }
    return view;
  }

  @GetMapping("/events")
  public String getStudentEvents(@CookieValue(name = "user", required = false) String userId,
                                 @CookieValue(name = "token", required = false) String token,
                                 @CookieValue(name = "mashov", required = false) String mashovCookies,
                                 @CookieValue(name = "csrf", required = false) String csrfToken,
                                 @CookieValue(name = "uniquId", required = false) String uniquId,
                                 Model model) {
    try {
      return tryGetStudentPage(userId, token, "student_events", model, Map.of(
          "behaves", tryGetBehaves(userId, token, mashovCookies, csrfToken, uniquId),
          "requests", studentApi.getMyPendingRequests(userId, token)
      ));
    } catch (ResponseStatusException e) {
      if (e.getStatus() == HttpStatus.UNAUTHORIZED) return getStudentLogin();
      throw e;
    }
  }

  @GetMapping("/requests")
  public String getStudentRequests(@CookieValue(name = "user", required = false) String userId,
                                   @CookieValue(name = "token", required = false) String token,
                                   Model model) {
    try {
      return tryGetStudentPage(userId, token, "student_requests", model, Map.of(
          "requests", studentApi.getMyRequestsWithFiles(userId, token)
      ));
    } catch (ResponseStatusException e) {
      if (e.getStatus() == HttpStatus.UNAUTHORIZED) return getStudentLogin();
      throw e;
    }
  }
  
  @GetMapping("/files")
  public String getStudentFiles(@CookieValue(name = "user", required = false) String userId,
                                @CookieValue(name = "token", required = false) String token,
                                Model model) {
    try {
      return tryGetStudentPage(userId, token, "student_files", model, Map.of(
          "files", generalApi.getUserFiles(userId, token, userId)
      ));
    } catch (ResponseStatusException e) {
      if (e.getStatus() == HttpStatus.UNAUTHORIZED) return getStudentLogin();
      throw e;
    }
  }
  
  @GetMapping("/vote")
  public String getVote(@CookieValue(name = "user", required = false) String userId,
                                @CookieValue(name = "token", required = false) String token,
                                Model model) {
    try {
      return tryGetStudentPage(userId, token, "vote", model, Map.of(
          "songs", generalApi.getSongs()
      ));
    } catch (ResponseStatusException e) {
      if (e.getStatus() == HttpStatus.UNAUTHORIZED) return getStudentLogin();
      throw e;
    }
  }

  private String tryGetTeacherPage(String userId,
                                   String token,
                                   String view,
                                   Model model,
                                   Map<String, ?> attributes) {
    try {
      User u = validator.validateUser(userId, token, Role.TEACHER);
      makeModel(model, u, attributes);
    } catch (ResponseStatusException e) {
      if (e.getStatus() == HttpStatus.FORBIDDEN) throw e;
      if (e.getStatus() == HttpStatus.UNAUTHORIZED) return getTeacherLogin();
      throw e;
    }

    return view;
  }

  @GetMapping("/all")
  private String getAllRequests(@CookieValue(name = "user", required = false) String userId,
                                @CookieValue(name = "token", required = false) String token,
                                Model model) {
    return tryGetTeacherPage(userId, token, "teacher_dashboard", model, Map.of(
        "requests", generalApi.getRequestsWithFiles(userId, token),
        "isTester", config.getTestAccountUsername().equals(userId)
    ));
  }

  @GetMapping("/active")
  private String getActiveRequests(@CookieValue(name = "user", required = false) String userId,
                                   @CookieValue(name = "token", required = false) String token,
                                   Model model) {
    return tryGetTeacherPage(userId, token, "teacher_dashboard", model, Map.of(
        "requests", generalApi.getPendingWithFiles(userId, token),
        "isTester", config.getTestAccountUsername().equals(userId)
    ));
  }
}
