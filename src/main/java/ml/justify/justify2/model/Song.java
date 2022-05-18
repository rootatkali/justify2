package ml.justify.justify2.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import org.jetbrains.annotations.NotNull;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;
import java.util.Objects;

@Entity
public class Song implements Comparable<Song> {
  @Id
  private int ro;
  
  private String artist;
  private String title;
  private String youtubeId;
  
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "song")
  private List<Vote> votes;
  
  public Song() {
  }
  
  public Song(int ro, String artist, String title, String youtubeId) {
    this.ro = ro;
    this.artist = artist;
    this.title = title;
    this.youtubeId = youtubeId;
  }
  
  public int getRo() {
    return ro;
  }
  
  public void setRo(int ro) {
    this.ro = ro;
  }
  
  public String getArtist() {
    return artist;
  }
  
  public void setArtist(String artist) {
    this.artist = artist;
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getYoutubeId() {
    return youtubeId;
  }
  
  public void setYoutubeId(String youtubeId) {
    this.youtubeId = youtubeId;
  }
  
  public int getVoteTotal() {
    return votes.stream().mapToInt(Vote::getPoints).sum();
  }
  
  public long getVoteCount() {
    return votes.stream().filter(v -> v.getPoints() > 0).count();
  }
  
  public long getPointCount(int points) {
    return votes.stream().filter(v -> v.getPoints() == points).count();
  }
  
  @Override
  public String toString() {
    return String.format("%s - %s", artist, title);
  }
  
  @Override
  public int compareTo(@NotNull Song o) {
    return ComparisonChain.start()
        .compare(getVoteTotal(), o.getVoteTotal())
        .compare(getVoteCount(), o.getVoteCount())
        .compare(getPointCount(12), o.getPointCount(12))
        .compare(getPointCount(10), o.getPointCount(10))
        .compare(getPointCount(8), o.getPointCount(8))
        .compare(getPointCount(7), o.getPointCount(7))
        .compare(getPointCount(6), o.getPointCount(6))
        .compare(getPointCount(5), o.getPointCount(5))
        .compare(getPointCount(4), o.getPointCount(4))
        .compare(getPointCount(3), o.getPointCount(3))
        .compare(getPointCount(2), o.getPointCount(2))
        .compare(getPointCount(1), o.getPointCount(1))
        .compare(o.ro, ro)
        .result();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Song song = (Song) o;
    return ro == song.ro && Objects.equals(artist, song.artist) && Objects.equals(title, song.title);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(ro, artist, title);
  }
}
