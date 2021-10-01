package ml.justify.justify2.api;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class MashovResource<T> {
  private T value;
  private String cookies;
  private String csrfToken;
  private String uniquId;
}
