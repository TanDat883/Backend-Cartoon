package flim.backendcartoon.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import flim.backendcartoon.entities.DTO.response.ChatResponse;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import flim.backendcartoon.entities.DTO.response.PromoSuggestionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    // Bean WebClient cấu hình ở OpenAIConfig (tên bean là "openAI")
    @Qualifier("openAI")
    private final WebClient openAI;

    private final ObjectMapper om = new ObjectMapper();


    public ChatResponse composeAnswer(String userName,
                                      List<MovieSuggestionDTO> suggestions,
                                      String userMessage,
                                      boolean wantsRec,
                                      boolean wantsPromo,
                                      List<PromoSuggestionDTO> promos) {

        String system = """
      Bạn là trợ lý cho website xem phim. Chỉ trả JSON hợp lệ.
      - Nếu wantsRec=true: set "showSuggestions": true và có mảng "suggestions" (<=8).
      - Nếu wantsPromo=true: set "showPromos": true và có mảng "promos" (<=8).
      - Có thể bật cả hai nếu câu hỏi vừa muốn phim vừa muốn khuyến mãi.
      - Nếu không muốn phần nào thì đặt cờ phần đó = false và mảng rỗng.
    """;

        Map<String, Object> ctx = Map.of(
                "userName", userName == null ? "bạn" : userName,
                "wantsRec", wantsRec,
                "wantsPromo", wantsPromo,
                "candidateSuggestions", suggestions,
                "activePromos", promos
        );

        Map<String, Object> payload = Map.of(
                "model", "gpt-4o-mini",
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role","system","content", system),
                        Map.of("role","user","content",
                                "Context:\n" + writeSafe(ctx) + "\n\nUser: " + (userMessage == null ? "" : userMessage))
                )
        );

        JsonNode root = openAI.post().uri("/chat/completions")
                .bodyValue(payload).retrieve().bodyToMono(JsonNode.class).block();

        String content = root.path("choices").get(0).path("message").path("content").asText("{}");

        try {
            ChatResponse cr = om.readValue(content, ChatResponse.class);

            // Chuẩn hoá flags + mảng
            if (!wantsRec) { cr.setShowSuggestions(false); cr.setSuggestions(List.of()); }
            if (!wantsPromo){ cr.setShowPromos(false);      cr.setPromos(List.of()); }
            if (cr.getShowSuggestions() == null) cr.setShowSuggestions(wantsRec);
            if (cr.getShowPromos() == null)      cr.setShowPromos(wantsPromo);
            if (cr.getSuggestions() == null)     cr.setSuggestions(List.of());
            if (cr.getPromos() == null)          cr.setPromos(List.of());
            if (cr.getAnswer() == null)          cr.setAnswer("Mình đã nhận câu hỏi của bạn nhé!");

            return cr;
        } catch (Exception e) {
            return new ChatResponse(
                    "Mình gặp chút trục trặc định dạng, nhưng vẫn hiểu câu hỏi của bạn.",
                    List.of(), false, List.of(), false
            );
        }
    }


    private String writeSafe(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }
}
