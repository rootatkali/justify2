package ml.justify.justify2.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class Auth {
  @Id
  private String userId;
  private String token;
  private Timestamp expires;

  public Auth() {
  }

  public Auth(String userId) {
    this.userId = userId;
  }

  public void generateToken() {
    this.token = UUID.randomUUID().toString();
    this.expires = Timestamp.valueOf(LocalDateTime.now().plusMinutes(60));
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public LocalDateTime getExpires() {
    return expires.toLocalDateTime();
  }

  @Override
  public String toString() {
    return "Auth{" + "userId='" + userId + '\'' +
        ", token='" + token + '\'' +
        ", expires=" + expires +
        '}';
  }
}
