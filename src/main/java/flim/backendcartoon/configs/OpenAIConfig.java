package flim.backendcartoon.configs;

import io.github.cdimascio.dotenv.Dotenv;
import io.netty.channel.ChannelOption;
import org.apache.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    private final Dotenv dotenv= Dotenv.load();
    private final String apiKey = dotenv.get("REACT_APP_OPENAI_API_KEY");

    @Bean
    public WebClient openAI(WebClient.Builder builder) {
        // Tối ưu HTTP client: gzip, keep-alive, HTTP/2, timeout hợp lý
        HttpClient httpClient = HttpClient.create()
                .compress(true)  // ✅ Enable gzip compression
                .keepAlive(true) // ✅ Enable connection reuse
                .responseTimeout(Duration.ofSeconds(20)) // ✅ Response timeout (tăng lên 20s cho complex queries)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000); // ✅ Connect timeout 5s

        return builder
                .baseUrl("https://api.openai.com/v1")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}