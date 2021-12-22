package ml.justify.justify2.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Date;

@Entity
public class Secret {
  @Id
  private int request;
  private Date startDate;
  private Date endDate;
  private Integer startPeriod;
  private Integer endPeriod;
  
  public Secret() {
  
  }
  
  public Secret(int request, Date startDate, Date endDate, Integer startPeriod, Integer endPeriod) {
    this.request = request;
    this.startDate = startDate;
    this.endDate = endDate;
    this.startPeriod = startPeriod;
    this.endPeriod = endPeriod;
  }
  
  public int getRequest() {
    return request;
  }
  
  public void setRequest(int request) {
    this.request = request;
  }
  
  public Date getStartDate() {
    return startDate;
  }
  
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }
  
  public Date getEndDate() {
    return endDate;
  }
  
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }
  
  public Integer getStartPeriod() {
    return startPeriod;
  }
  
  public void setStartPeriod(Integer startPeriod) {
    this.startPeriod = startPeriod;
  }
  
  public Integer getEndPeriod() {
    return endPeriod;
  }
  
  public void setEndPeriod(Integer endPeriod) {
    this.endPeriod = endPeriod;
  }
}
