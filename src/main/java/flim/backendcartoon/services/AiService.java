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

import java.util.ArrayList;
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
                                      List<ChatMemoryService.ChatMsg> history,
                                      boolean wantsRec,
                                      boolean wantsPromo,
                                      List<PromoSuggestionDTO> promos,
                                      Map<String,Object> extras) {

        final String safeUser = (userName == null || userName.isBlank()) ? "bạn" : userName;

        // Hướng dẫn model rất rõ + chỉ cho trả JSON
        String system = """
            Bạn là trợ lý thông minh cho website xem phim CartoonToo.
            
            BẠN ĐƯỢC CUNG CẤP TRONG 'Context':
            - currentMovie: object | null — phim của trang hiện tại (nếu người dùng đang ở trang chi tiết).
            - mentionedMovies: array — các phim trong DB khớp với tên mà người dùng vừa nhắc.
            - candidateSuggestions: array — các phim đề xuất để GỢI Ý khi (và chỉ khi) user thực sự muốn gợi ý.
            
            QUY TẮC:
            1) Nếu currentMovie != null và câu hỏi là về “phim này/đang xem” → trả lời dựa trên currentMovie.
            2) Nếu user nêu đích danh tên phim và nó có trong mentionedMovies → trả lời dựa trên phim đó.
            3) KHÔNG được nói "không có thông tin trong hệ thống" khi currentMovie hoặc mentionedMovies có dữ liệu.
            4) Chỉ gợi ý từ candidateSuggestions khi wantsRec=true.
            5) Trả về MỘT JSON OBJECT:
               - answer: string
               - showSuggestions: boolean
               - suggestions: array<MovieSuggestionDTO> (<=8)
               - showPromos: boolean
               - promos: array<PromoSuggestionDTO> (<=8)
            KHÔNG thêm text ngoài JSON.
            
                Trong 'Context' có thể có:
                - currentMovie { ..., directors[], performers[], authors[] }
                - mentionedMovies[] với cấu trúc tương tự
                
                QUY TẮC BỔ SUNG:
                - Khi người dùng hỏi đạo diễn/diễn viên, chỉ dùng currentMovie/mentionedMovies.
                - Nếu directors/performers rỗng hoặc thiếu, trả lời lịch sự kiểu:
                  "Hiện hệ thống chưa lưu diễn viên/đạo diễn cho phim này." và KHÔNG tự bịa.
                ...
            """;


        Map<String, Object> ctx = new HashMap<>();
        ctx.put("userName", safeUser);
        ctx.put("wantsRec", wantsRec);
        ctx.put("wantsPromo", wantsPromo);
        ctx.put("candidateSuggestions", suggestions == null ? List.of() : suggestions);
        ctx.put("activePromos", promos == null ? List.of() : promos);
        if (extras != null) ctx.putAll(extras);   // ✅ quan trọng


        // Ép model theo JSON Schema để giảm sai key
        Map<String, Object> responseFormat = buildResponseFormat();

        var messages = new ArrayList<Map<String, Object>>();
        messages.add(Map.of("role", "system", "content", system));

        if (history != null && !history.isEmpty()) {
            for (var m : history) {
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }
        messages.add(Map.of("role", "user",
                "content", "Context:\n" + writeSafe(ctx) + "\n\nUser: " + (userMessage == null ? "" : userMessage)));



        Map<String, Object> payload = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0.2,
                "response_format", responseFormat,
                "messages", messages  // ✅ Use the full messages array
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
        movieProps.put("description", Map.of("type", "string"));
        movieProps.put("genres", Map.of("type", "array", "items", Map.of("type", "string")));
        movieProps.put("viewCount", Map.of("type", "number"));
        movieProps.put("duration", Map.of("type", "string"));
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
