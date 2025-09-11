package flim.backendcartoon.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import flim.backendcartoon.entities.DTO.response.ChatResponse;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import flim.backendcartoon.entities.DTO.response.PromoSuggestionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    // Bean WebClient cấu hình ở OpenAIConfig (tên bean là "openAI")
    @Qualifier("openAI")
    private final WebClient openAI;

    private final ObjectMapper om = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ChatResponse composeAnswer(String userName,
                                      List<MovieSuggestionDTO> suggestions,
                                      String userMessage,
                                      boolean wantsRec,
                                      boolean wantsPromo,
                                      List<PromoSuggestionDTO> promos) {

        final String safeUser = (userName == null || userName.isBlank()) ? "bạn" : userName;

        // Hướng dẫn model rất rõ + chỉ cho trả JSON
        String system = """
Bạn là trợ lý cho website xem phim. Chỉ trả về MỘT JSON OBJECT hợp lệ với các khóa CHÍNH XÁC:
- answer: string (câu trả lời ngắn gọn, thân thiện và vui vẻ)
- showSuggestions: boolean
- suggestions: array<MovieSuggestionDTO> (<=8) - dùng đúng các phần tử có sẵn trong "candidateSuggestions" nếu wantsRec=true, ngược lại []
- showPromos: boolean
- promos: array<PromoSuggestionDTO> (<=8) - dùng đúng các phần tử có sẵn trong "activePromos" nếu wantsPromo=true, ngược lại []
Không thêm bất kỳ text/thuyết minh nào ngoài JSON.
""";

        Map<String, Object> ctx = Map.of(
                "userName", safeUser,
                "wantsRec", wantsRec,
                "wantsPromo", wantsPromo,
                "candidateSuggestions", suggestions == null ? List.of() : suggestions,
                "activePromos", promos == null ? List.of() : promos
        );

        // Ép model theo JSON Schema để giảm sai key
        Map<String, Object> responseFormat = buildResponseFormat();

        Map<String, Object> payload = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0.2,
                "response_format", responseFormat,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content",
                                "Context:\n" + writeSafe(ctx) + "\n\nUser: " + (userMessage == null ? "" : userMessage))
                )
        );

        try {
            JsonNode root = openAI.post()
                    .uri("/chat/completions")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            String content = root == null ? "{}" : root.at("/choices/0/message/content").asText("{}");
            log.debug("OpenAI content: {}", content);

            ChatResponse cr = om.readValue(content, ChatResponse.class);

            // Chuẩn hoá flags + mảng + câu trả lời
            normalize(cr, safeUser, wantsRec, wantsPromo);

            return cr;

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return fallback(safeUser, suggestions, wantsRec, promos, wantsPromo,
                    "Mình đang gặp sự cố kết nối OpenAI. Đây là gợi ý/k.mãi hiện có.");
        } catch (Exception e) {
            log.error("composeAnswer error", e);
            return fallback(safeUser, suggestions, wantsRec, promos, wantsPromo,
                    "Mình gặp chút trục trặc định dạng, nhưng vẫn hiểu câu hỏi của bạn.");
        }
    }

    /* ---------------- helpers ---------------- */

    private void normalize(ChatResponse cr, String userName, boolean wantsRec, boolean wantsPromo) {
        if (cr == null) cr = new ChatResponse();

        // Flags
        if (!wantsRec) { cr.setShowSuggestions(false); cr.setSuggestions(List.of()); }
        if (!wantsPromo){ cr.setShowPromos(false);      cr.setPromos(List.of()); }

        if (cr.getShowSuggestions() == null) cr.setShowSuggestions(wantsRec);
        if (cr.getShowPromos() == null)      cr.setShowPromos(wantsPromo);

        // Lists
        if (cr.getSuggestions() == null) cr.setSuggestions(List.of());
        if (cr.getPromos() == null)      cr.setPromos(List.of());

        // Answer
        if (cr.getAnswer() == null || cr.getAnswer().isBlank()) {
            String msg;
            if (wantsPromo)      msg = "Mình tổng hợp khuyến mãi dành cho " + userName + " dưới đây.";
            else if (wantsRec)   msg = "Mình có vài gợi ý phim phù hợp cho " + userName + ".";
            else                 msg = "Mình có thể giúp bạn tìm phim theo thể loại, quốc gia hoặc chủ đề.";
            cr.setAnswer(msg);
        }
    }

    private ChatResponse fallback(String userName,
                                  List<MovieSuggestionDTO> suggestions, boolean wantsRec,
                                  List<PromoSuggestionDTO> promos, boolean wantsPromo,
                                  String answer) {
        return ChatResponse.builder()
                .answer(answer)
                .suggestions(wantsRec && suggestions != null ? suggestions : List.of())
                .showSuggestions(wantsRec && suggestions != null && !suggestions.isEmpty())
                .promos(wantsPromo && promos != null ? promos : List.of())
                .showPromos(wantsPromo && promos != null && !promos.isEmpty())
                .build();
    }

    private String writeSafe(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }

    /** Tạo response_format kiểu json_schema cho Chat Completions */
    private Map<String, Object> buildResponseFormat() {
        // MovieSuggestionDTO schema
        Map<String, Object> movieProps = new HashMap<>();
        movieProps.put("movieId", Map.of("type", "string"));
        movieProps.put("title", Map.of("type", "string"));
        movieProps.put("thumbnailUrl", Map.of("type", "string"));
        movieProps.put("genres", Map.of("type", "array", "items", Map.of("type", "string")));
        movieProps.put("viewCount", Map.of("type", "number"));
        movieProps.put("avgRating", Map.of("type", "number"));

        Map<String, Object> movieItem = Map.of(
                "type", "object",
                "properties", movieProps,
                "required", List.of("movieId", "title")
        );

        // PromoSuggestionDTO schema
        Map<String, Object> promoProps = new HashMap<>();
        promoProps.put("promotionId", Map.of("type", "string"));
        promoProps.put("title", Map.of("type", "string"));
        promoProps.put("type", Map.of("type", "string"));
        promoProps.put("discountPercent", Map.of("type", "integer"));
        promoProps.put("voucherCode", Map.of("type", "string"));
        promoProps.put("maxDiscountAmount", Map.of("type", "integer"));
        promoProps.put("startDate", Map.of("type", "string"));
        promoProps.put("endDate", Map.of("type", "string"));
        promoProps.put("status", Map.of("type", "string"));
        promoProps.put("note", Map.of("type", "string"));

        Map<String, Object> promoItem = Map.of(
                "type", "object",
                "properties", promoProps
        );

        // ChatResponse schema
        Map<String, Object> rootProps = new HashMap<>();
        rootProps.put("answer", Map.of("type", "string"));
        rootProps.put("showSuggestions", Map.of("type", "boolean"));
        rootProps.put("suggestions", Map.of("type", "array", "items", movieItem));
        rootProps.put("showPromos", Map.of("type", "boolean"));
        rootProps.put("promos", Map.of("type", "array", "items", promoItem));

        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", rootProps,
                "required", List.of("answer", "showSuggestions", "suggestions", "showPromos", "promos")
        );

        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "ChatResponse",
                        "schema", schema
                )
        );
    }
}
