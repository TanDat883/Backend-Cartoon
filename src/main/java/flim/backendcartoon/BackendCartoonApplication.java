package flim.backendcartoon;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class BackendCartoonApplication {

    public static void main(String[] args) {
        Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.load();
        // Set to System env
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(BackendCartoonApplication.class, args);
    }

}
