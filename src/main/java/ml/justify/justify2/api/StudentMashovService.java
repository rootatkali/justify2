package ml.justify.justify2.api;

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import de.faceco.mashovapi.components.*;
import lombok.extern.slf4j.Slf4j;
import ml.justify.justify2.config.MashovConfig;
import ml.justify.justify2.model.Event;
import ml.justify.justify2.model.Justification;
import ml.justify.justify2.model.Role;
import ml.justify.justify2.model.User;
import ml.justify.justify2.repo.EventRepository;
import ml.justify.justify2.repo.JustificationRepository;
import ml.justify.justify2.repo.UserRepository;
import ml.justify.justify2.util.CookieUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class StudentMashovService {
  private final MashovConfig config;
  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final JustificationRepository justificationRepository;

  private School school;
  private final OkHttpClient http;
  private final Gson gson;

  @Autowired
  public StudentMashovService(MashovConfig config,
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

  public School getSchool() {
    return school;
  }

  public MashovResource<User> login(Login l, String uniquId) throws IOException {
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

    LoginInfo li = gson.fromJson(rb, LoginInfo.class);

    User u = new User();
    u.setId(String.valueOf(li.getCredential().getIdNumber()));
    u.setDisplayName(li.getAccessToken().getDisplayName());
    u.setMashovGuid(li.getCredential().getUserId());
    u.setRole(Role.STUDENT);

    if (!userRepository.existsById(u.getId())) userRepository.save(u);
    else u = userRepository.findById(u.getId()).orElseThrow();

    String uniquRet = cookies.getOrDefault("uniquId", null);

    MashovResource<User> ret = new MashovResource<>(u, CookieUtil.convert(cookies, "~"), csrfToken, uniquRet);


    // load event and justification from api
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

  // Returns only unjustified behaves
  public List<Behave> getBehaves(MashovResource<User> user) throws IOException {
    Response res = apiGet("/students/" + user.getValue().getMashovGuid() + "/behave", user);
    Behave[] raw = gson.fromJson(res.body().string(), Behave[].class);

    int[] whitelist = config.getEventWhitelist();
    boolean bypassWhitelist = whitelist == null || whitelist.length == 0;

    return Arrays.stream(raw)
        .filter(b -> b.getJustified() == -1) // not justified
        .filter(b -> bypassWhitelist || Ints.contains(whitelist, b.getEventCode())) // in whitelist
        .collect(Collectors.toList());
  }

  public MashovResource<Integer> logout(MashovResource<User> user) throws IOException {
    Response res = apiGet("/logout", user);
    return new MashovResource<>(res.code(), "", "", user.getUniquId());
  }
}
