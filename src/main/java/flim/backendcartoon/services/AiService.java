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

        // ✅ Improved system prompt với context awareness
        String system = """
Trợ lý AI cho website phim CartoonToo. Trả về JSON theo schema:
{answer:string, showSuggestions:bool, suggestions:[], showPromos:bool, promos:[]}

QUY TẮC:
1) Chỉ gợi ý từ candidateSuggestions khi wantsRec=true (max 8).
2) Chỉ đưa promos từ activePromos khi wantsPromo=true (max 8). KHÔNG bịa mã.
3) Dùng currentMovie/mentionedMovies nếu user hỏi chi tiết phim.
4) Trả ngắn gọn, thân thiện, dùng "%s" khi xưng hô.
5) KHÔNG text ngoài JSON.

⚠️ QUAN TRỌNG - PHÂN BIỆT NGỮ CẢNH:
- Nếu candidateSuggestions có sẵn + user hỏi "nên xem bộ nào"/"phim nào hay"
  → User đang hỏi về PHIM trong danh sách, KHÔNG phải gói đăng ký
  → Trả lời về PHIM, so sánh thể loại/rating/nội dung
- Nếu user hỏi "gói đăng ký"/"giá tiền"/"mua gói"
  → Đó mới là hỏi về pricing/subscription
""".formatted(safeUser);



        // ✅ Chỉ đưa suggestions/promos vào context khi cần thiết
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("userName", safeUser);
        ctx.put("wantsRec", wantsRec);
        ctx.put("wantsPromo", wantsPromo);

        // ✅ Chỉ gắn candidateSuggestions khi wantsRec=true
        if (wantsRec && suggestions != null && !suggestions.isEmpty()) {
            ctx.put("candidateSuggestions", suggestions);
        }

        // ✅ Chỉ gắn activePromos khi wantsPromo=true
        if (wantsPromo && promos != null && !promos.isEmpty()) {
            ctx.put("activePromos", promos);
        }

        if (extras != null) ctx.putAll(extras);


        // Ép model theo JSON Schema để giảm sai key
        Map<String, Object> responseFormat = buildResponseFormat();

        var messages = new ArrayList<Map<String, Object>>();
        messages.add(Map.of("role", "system", "content", system));

        // ✅ Chỉ giữ 3-5 message cuối để giảm payload
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - 5);
            for (int i = startIdx; i < history.size(); i++) {
                var m = history.get(i);
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
            // ✅ Đo latency: t_send_openai
            long tSend = System.currentTimeMillis();

            // ✅ Log payload size
            String payloadJson = writeSafe(payload);
            int payloadSize = payloadJson.getBytes().length;
            log.info("⏱️ OpenAI request | payload_size={}bytes | messages_count={}",
                    payloadSize, messages.size());

            JsonNode root = openAI.post()
                    .uri("/chat/completions")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            // ✅ Đo latency: t_recv_openai
            long tRecv = System.currentTimeMillis();
            long openaiLatency = tRecv - tSend;

            String content = root == null ? "{}" : root.at("/choices/0/message/content").asText("{}");
            log.info("⏱️ OpenAI response | latency={}ms | response_size={}bytes",
                    openaiLatency, content.getBytes().length);

            ChatResponse cr = om.readValue(content, ChatResponse.class);

            // Chuẩn hoá flags + mảng + câu trả lời
            normalize(cr, safeUser, wantsRec, wantsPromo);

            // ✅ Đo latency end-to-end (từ service call)
            long tEnd = System.currentTimeMillis();
            log.info("⏱️ composeAnswer completed | total_latency={}ms | openai_latency={}ms",
                    (tEnd - tSend), openaiLatency);

            return cr;

        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // ✅ Handle timeout exceptions (ReadTimeoutException, ConnectTimeoutException)
            if (e.getCause() != null && e.getCause().toString().contains("ReadTimeoutException")) {
                log.warn("⏱️ OpenAI timeout - Query might be too complex or off-topic | timeout=12s");
                return fallbackOffTopic(safeUser, suggestions, wantsRec, promos, wantsPromo);
            }
            log.error("⚠️ OpenAI connection error: {}", e.getMessage());
            return fallback(safeUser, suggestions, wantsRec, promos, wantsPromo,
                    "Mình đang gặp sự cố kết nối. Đây là gợi ý dành cho bạn.");
        } catch (WebClientResponseException e) {
            log.error("⚠️ OpenAI API error {} - {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return fallback(safeUser, suggestions, wantsRec, promos, wantsPromo,
                    "Mình đang gặp sự cố kết nối OpenAI. Đây là gợi ý/k.mãi hiện có.");
        } catch (Exception e) {
            log.error("⚠️ composeAnswer error: {}", e.getMessage(), e);
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

    /**
     * ✅ Fallback for off-topic or timeout queries
     * Khi user hỏi câu không liên quan đến phim (vd: "trần trọng tín có đỉnh ko")
     */
    private ChatResponse fallbackOffTopic(String userName,
                                          List<MovieSuggestionDTO> suggestions, boolean wantsRec,
                                          List<PromoSuggestionDTO> promos, boolean wantsPromo) {
        String answer = String.format(
                "Xin lỗi %s, mình là trợ lý tìm phim nên chỉ có thể giúp bạn với các câu hỏi về phim, " +
                "thể loại, diễn viên, hoặc gợi ý xem gì. " +
                "Bạn có thể thử hỏi như:\n" +
                "• \"Gợi ý phim hành động Hàn Quốc\"\n" +
                "• \"Phim anime hay nhất\"\n" +
                "• \"Có khuyến mãi gì không?\"\n\n" +
                "Dưới đây là vài gợi ý phim hot hiện tại cho bạn:",
                userName
        );

        return ChatResponse.builder()
                .answer(answer)
                .suggestions(suggestions != null && !suggestions.isEmpty() ? suggestions : List.of())
                .showSuggestions(suggestions != null && !suggestions.isEmpty())
                .promos(List.of())
                .showPromos(false)
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
                "required", List.of("movieId", "title","thumbnailUrl")
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
