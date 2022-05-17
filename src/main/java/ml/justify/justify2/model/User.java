package ml.justify.justify2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
public class User {
  @Id
  private String id;
  private String displayName;
  private String mashovGuid;
  private Role role;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
  @JsonIgnore
  private Set<Request> requests;
  
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "voter")
  @JsonIgnore
  private List<Vote> votes;

  public User() {
  }

  public User(String id, String displayName, Role role) {
    this.id = id;
    this.displayName = displayName;
    this.role = role;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getMashovGuid() {
    return mashovGuid;
  }

  public void setMashovGuid(String mashovGuid) {
    this.mashovGuid = mashovGuid;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public List<Request> getRequests() {
    return new ArrayList<>(requests);
  }
  
  public boolean hasVoted() {
    return votes != null && !votes.isEmpty();
  }
}
