package ml.justify.justify2.model;

import lombok.*;

import java.sql.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class RequestTemplate {
  private String userId;
  private Date dateStart;
  private Date dateEnd;
  private Integer periodStart;
  private Integer periodEnd;
  private Integer eventCode;
  private Integer justificationCode;
  private String note;
}
