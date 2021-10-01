package ml.justify.justify2;

import ml.justify.justify2.config.MashovConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    MashovConfig.class
})
public class Justify2Application {

  public static void main(String[] args) {
    SpringApplication.run(Justify2Application.class, args);
  }

}
