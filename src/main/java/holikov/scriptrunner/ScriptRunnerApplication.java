package holikov.scriptrunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ScriptRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScriptRunnerApplication.class, args);
    }

}
