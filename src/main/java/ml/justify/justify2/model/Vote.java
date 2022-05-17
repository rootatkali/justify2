package ml.justify.justify2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
public class Vote {
  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
  private String id;
  
  @ManyToOne
  @JoinColumn(name = "user")
  private User voter;
  
  @ManyToOne
  @JoinColumn(name = "song")
  @JsonIgnore
  private Song song;
  private int points;
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public User getVoter() {
    return voter;
  }
  
  public void setVoter(User voter) {
    this.voter = voter;
  }
  
  public Song getSong() {
    return song;
  }
  
  public void setSong(Song song) {
    this.song = song;
  }
  
  public int getPoints() {
    return points;
  }
  
  public void setPoints(int points) {
    this.points = points;
  }
}
