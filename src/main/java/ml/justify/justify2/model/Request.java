package ml.justify.justify2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import ml.justify.justify2.file.DbFile;
import ml.justify.justify2.util.Xss;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
public class Request implements Comparable<Request> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer requestId;
  private Timestamp requested;
  private Date dateStart;
  private Date dateEnd;
  private Integer periodStart;
  private Integer periodEnd;
  private Integer eventCode;
  private Integer justificationCode;
  @Lob
  private String note;
  private RequestStatus status;

  @ManyToOne
  @JoinColumn(name = "user")
  private User user;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "request")
  @JsonIgnore
  private Set<DbFile> files;

  public Request() {
    requested = Timestamp.valueOf(LocalDateTime.now());
  }

  public Integer getRequestId() {
    return requestId;
  }

  public void setRequestId(Integer requestId) {
    this.requestId = requestId;
  }

  public LocalDateTime getRequested() {
    return requested.toLocalDateTime();
  }

  public Date getDateStart() {
    return dateStart;
  }

  public void setDateStart(Date dateStart) {
    this.dateStart = dateStart;
  }

  public Date getDateEnd() {
    return dateEnd;
  }

  public void setDateEnd(Date dateEnd) {
    this.dateEnd = dateEnd;
  }

  public Integer getPeriodStart() {
    return periodStart;
  }

  public void setPeriodStart(Integer periodStart) {
    this.periodStart = periodStart;
  }

  public Integer getPeriodEnd() {
    return periodEnd;
  }

  public void setPeriodEnd(Integer periodEnd) {
    this.periodEnd = periodEnd;
  }

  public Integer getEventCode() {
    return eventCode;
  }

  public String getEventName(List<Event> events) {
    return events.stream().filter(e -> Objects.equals(e.getCode(), eventCode)).findFirst().orElseThrow().getName();
  }

  public void setEventCode(Integer eventCode) {
    this.eventCode = eventCode;
  }

  public Integer getJustificationCode() {
    return justificationCode;
  }

  public String getApprovalName(List<Justification> justifications) {
    return justifications.stream().filter(j -> Objects.equals(j.getCode(), justificationCode))
        .findFirst().orElseThrow().getName();
  }

  public void setJustificationCode(Integer justificationCode) {
    this.justificationCode = justificationCode;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = Xss.deXss(note);
  }

  public RequestStatus getStatus() {
    return status;
  }

  public String status() {
    return status.toString();
  }

  public String getStatusClass() {
    switch (status) {
      case APPROVED: return "table-success"; // green
      case REJECTED: return "table-danger"; // red
      case CANCELLED: return "table-secondary"; // gray
      default: return ""; // none
    }
  }

  public void setStatus(RequestStatus status) {
    this.status = status;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public List<DbFile> getFiles() {
    return new ArrayList<>(files);
  }

  public void setFiles(DbFile... files) {
    this.files = Set.of(files);
  }

  @Override
  public int compareTo(@NotNull Request o) {
    return requestId - o.requestId;
  }
}
