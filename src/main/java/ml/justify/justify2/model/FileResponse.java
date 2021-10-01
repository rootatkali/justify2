package ml.justify.justify2.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class FileResponse {
  private String id;
  private String name;
  private String type;
  private String downloadUrl;
  private long size;
}
