package ml.justify.justify2.model;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {
  private int futureId;
  private int studentid;
  private int achvaCode;
  private int justificationCode;
  private String startDate;
  private String endDate;
  private int startlesson;
  private int endlesson;
  private int startDayOfWeek;
  private int endDayOfWeek;
  private int startDayLesson;
  private int endDayLesson;
  private int justifiedById;
  private String timestamp;
  private String studentGuid;
}
