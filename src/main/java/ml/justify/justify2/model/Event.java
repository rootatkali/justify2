package ml.justify.justify2.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Event {
  @Id
  private Integer code;
  private String name;

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}