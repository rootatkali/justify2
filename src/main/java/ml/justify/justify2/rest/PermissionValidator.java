package ml.justify.justify2.rest;

import ml.justify.justify2.config.MashovConfig;
import ml.justify.justify2.model.Auth;
import ml.justify.justify2.model.Role;
import ml.justify.justify2.model.User;
import ml.justify.justify2.repo.AdminRepository;
import ml.justify.justify2.repo.AuthRepository;
import ml.justify.justify2.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class PermissionValidator {
  private final UserRepository userRepository;
  private final AuthRepository authRepository;
  private final AdminRepository adminRepository;
  private final MashovConfig config;

  @Autowired
  public PermissionValidator(UserRepository userRepository,
                             AuthRepository authRepository,
                             AdminRepository adminRepository,
                             MashovConfig config) {
    this.userRepository = userRepository;
    this.authRepository = authRepository;
    this.adminRepository = adminRepository;
    this.config = config;
  }

  private void validateRole(User user, Role... allowed) {
    if (allowed == null || allowed.length == 0) return;
    if (!Arrays.asList(allowed).contains(user.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  private void validateToken(User user, String token) {
    // this check should always be true
    if (!authRepository.existsById(user.getId())) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

    if (token == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

    Auth auth = authRepository.findById(user.getId()).orElseThrow();
    if (!token.equals(auth.getToken())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (auth.getExpires().isBefore(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired.");
    }
  }

  private User validateUserExists(String userId) {
    if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return userRepository.findById(userId).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.")
    );
  }

  public User validateUser(String userId, String token, Role... expected) {
    User u = validateUserExists(userId);
    validateToken(u, token);
    validateRole(u, expected);
    return u;
  }

  public boolean isTester(User user) {
    return user.getId().equals(config.getTestAccountUsername());
  }

  public void denyTester(User user) {
    if (isTester(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tester account denied");
  }

  public void validateAdmin(String token) {
    if (token == null || !adminRepository.existsById(token)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }
}
