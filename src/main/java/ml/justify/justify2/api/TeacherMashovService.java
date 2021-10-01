package ml.justify.justify2.api;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import de.faceco.mashovapi.components.*;
import lombok.extern.slf4j.Slf4j;
import ml.justify.justify2.config.MashovConfig;
import ml.justify.justify2.model.*;
import ml.justify.justify2.repo.EventRepository;
import ml.justify.justify2.repo.JustificationRepository;
import ml.justify.justify2.repo.UserRepository;
import ml.justify.justify2.util.CookieUtil;
import okhttp3.*;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeacherMashovService {
  private final MashovConfig config;
  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final JustificationRepository justificationRepository;
  private School school;
  private final OkHttpClient http;
  private final Gson gson;

  @Autowired
  public TeacherMashovService(MashovConfig config,
                              UserRepository userRepository,
                              EventRepository eventRepository,
                              JustificationRepository justificationRepository) throws IOException {
    this.config = config;
    this.userRepository = userRepository;
    this.eventRepository = eventRepository;
    this.justificationRepository = justificationRepository;

    this.gson = new Gson();
    this.http = new OkHttpClient.Builder().build();
    this.school = fetchSchool();
  }

  private School fetchSchool() throws IOException {
    Request req = new Request.Builder()
        .url(config.getBaseUrl() + "/schools")
        .method("GET", null)
        .build();

    Response res = http.newCall(req).execute();
    School[] sls = gson.fromJson(res.body().string(), School[].class);
    school = Arrays.stream(sls).filter(s -> s.getId() == config.getSemel()).findAny().get();
    return school;
  }

  public MashovResource<User> login(Login l, String uniquId) throws IOException {
    // handle tester account
    if (l.getUsername().equals(config.getTestAccountUsername()) &&
        l.getPassword().equals(config.getTestAccountPassword())) {
      User tester = userRepository.findById(config.getTestAccountUsername()).orElseGet(() -> {
        User u = new User();
        u.setId(config.getTestAccountUsername());
        u.setDisplayName("Test User");
        u.setMashovGuid(config.getTestAccountUsername());
        u.setRole(Role.TEACHER);
        return userRepository.save(u);
      });

      return new MashovResource<>(tester, null, null, null);
    }

    MediaType json = MediaType.parse("application/json");
    RequestBody body = RequestBody.create(gson.toJson(l), json);

    Request.Builder builder = new Request.Builder()
        .url(config.getBaseUrl() + "/login")
        .method("POST", body)
        .addHeader("User-Agent", config.getUserAgent())
        .addHeader("Content-Type", "application/json");
    if (uniquId != null && !uniquId.equals("")) {
      builder.addHeader("Cookie", "uniquId=" + uniquId);
    }
    Request req = builder.build();

    Response res = http.newCall(req).execute();
    if (!res.isSuccessful()) {
      log.error(res.body().string());
      throw new ResponseStatusException(HttpStatus.valueOf(res.code()));
    }

    Map<String, String> cookies = new HashMap<>();

    Headers headers = res.headers();
    String csrfToken = Cookie.parse(req.url(), headers.get("Set-Cookie")).value();
    List<String> rawCookies = headers.values("Set-Cookie");
    for (String c : rawCookies) {
      String[] split = c.trim().split("=", 2);
      if (!cookies.containsKey(split[0])) {
        cookies.put(split[0], split[1].substring(0, split[1].indexOf(";")));
      }
    }

    String rb = res.body().string();

    ReadContext ctx = JsonPath.parse(rb);

    User u = new User();
    u.setId(String.valueOf((Integer) ctx.read("$.credential.idNumber")));
    u.setDisplayName(ctx.read("$.accessToken.displayName"));
    u.setMashovGuid(ctx.read("$.credential.userId"));
    u.setRole(Role.TEACHER);

    if (!userRepository.existsById(u.getId())) userRepository.save(u);
    else u = userRepository.findById(u.getId()).orElseThrow();

    String uniquRet = cookies.getOrDefault("uniquId", null);

    MashovResource<User> ret = new MashovResource<>(u, CookieUtil.convert(cookies, "~"), csrfToken, uniquRet);

    // load events and justifications from api
    for (Event e : fetchEvents(ret)) {
      if (!eventRepository.existsById(e.getCode())) eventRepository.save(e);
    }

    for (Justification j : fetchJustifications(ret)) {
      if (!justificationRepository.existsById(j.getCode())) justificationRepository.save(j);
    }

    return ret;
  }

  private Response apiGet(String path, MashovResource<User> user) throws IOException {
    Map<String, String> cookies = CookieUtil.convert(user.getCookies(), "~");
    Request request = new Request.Builder()
        .url(config.getBaseUrl() + path)
        .method("GET", null)
        .addHeader("User-Agent", config.getUserAgent())
        .addHeader("x-csrf-token", user.getCsrfToken())
        .addHeader("Cookie", CookieUtil.convert(cookies, ";"))
        .build();
    return http.newCall(request).execute();
  }

  private Event[] fetchEvents(MashovResource<User> user) throws IOException {
    Response res = apiGet("/achvas", user);
    return gson.fromJson(res.body().string(), Event[].class);
  }

  private List<Justification> fetchJustifications(MashovResource<User> user) throws IOException {
    Response res = apiGet("/justifications", user);
    MashovJustification[] mjs = gson.fromJson(res.body().string(), MashovJustification[].class);
    return Arrays.stream(mjs)
        .filter(mj -> !mj.isHidden())
        .filter(mj -> mj.getJustificationId() > 0)
        .map(mj -> new Justification(mj.getJustificationId(), mj.getJustification()))
        .collect(Collectors.toList());
  }

  public ApprovalResponse requestApproval(MashovResource<User> user, Approval approval) throws IOException {
    MediaType json = MediaType.parse("application/json");
    RequestBody body = RequestBody.create(gson.toJson(approval), json);
    Request req = new Request.Builder()
        .url(config.getBaseUrl() + "/futureJustifications")
        .method("POST", body)
        .addHeader("User-Agent", config.getUserAgent())
        .addHeader("x-csrf-token", user.getCsrfToken())
        .addHeader("Cookie", user.getCookies().replaceAll("~", ";"))
        .build();
    Response res = http.newCall(req).execute();

    String rb = res.body().string();
    if (!res.isSuccessful()) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    log.info(rb);
    return gson.fromJson(rb, ApprovalResponse.class);
  }

  public MashovResource<Integer> logout(MashovResource<User> user) throws IOException {
    // handle tester account
    if (user.getValue().getId().equals(config.getTestAccountUsername())) {
      return new MashovResource<>(HttpStatus.OK.value(), null, null, null);
    }

    Response res = apiGet("/logout", user);
    return new MashovResource<>(res.code(), "", "", user.getUniquId());
  }

  public School getSchool() {
    return school;
  }
}
