package ml.justify.justify2.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class Admin {
  @Id
  private String token;

  public Admin() {
    token = UUID.randomUUID().toString();
  }

  public String getToken() {
    return token;
  }
}
