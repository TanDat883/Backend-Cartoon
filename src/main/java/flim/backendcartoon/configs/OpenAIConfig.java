package flim.backendcartoon.configs;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Value;
import org.apache.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAIConfig {

    private final Dotenv dotenv= Dotenv.load();
    private final String apiKey = dotenv.get("REACT_APP_OPENAI_API_KEY");

    @Bean
    public WebClient openAI(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}