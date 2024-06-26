package holikov.scriptrunner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class Config {

    @Bean
    public ExecutorService executorServiceForScripts(){
        return Executors.newFixedThreadPool(100);
    }
    @Bean
    public ExecutorService executorServiceForNonBlocking(){
        return Executors.newCachedThreadPool();
    }
    @Bean
    public ScheduledExecutorService scheduler(){
        return Executors.newScheduledThreadPool(1);
    }
}
