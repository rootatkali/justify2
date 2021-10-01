package ml.justify.justify2.model;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Approval {
  private int achvaCode;
  private String endDate;
  private int endDayLesson;
  private int endDayOfWeek;
  private int endlesson;
  private int justificationCode;
  private int justifiedById;
  private String startDate;
  private int startDayLesson;
  private int startDayOfWeek;
  private int startlesson;
  private String timestamp;
  private int studentid;
}
