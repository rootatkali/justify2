package ml.justify.justify2.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class RequestAndFiles {
  private Request request;
  private List<FileResponse> files;
}
