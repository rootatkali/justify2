package ml.justify.justify2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mashov")
public class MashovConfig {
  private int semel;
  private int year;
  private String baseUrl;
  private String userAgent;
  private String testAccountUsername;
  private String testAccountPassword;
  private int[] eventWhitelist;


  public int getSemel() {
    return semel;
  }

  public void setSemel(int semel) {
    this.semel = semel;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getTestAccountUsername() {
    return testAccountUsername;
  }

  public void setTestAccountUsername(String testAccountUsername) {
    this.testAccountUsername = testAccountUsername;
  }

  public String getTestAccountPassword() {
    return testAccountPassword;
  }

  public void setTestAccountPassword(String testAccountPassword) {
    this.testAccountPassword = testAccountPassword;
  }

  public int[] getEventWhitelist() {
    return eventWhitelist;
  }

  public void setEventWhitelist(int[] eventWhitelist) {
    this.eventWhitelist = eventWhitelist;
  }
}
