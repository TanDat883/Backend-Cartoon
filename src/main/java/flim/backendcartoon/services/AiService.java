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

        // ✅ PRE-VALIDATION: Detect off-topic TRƯỚC KHI gọi OpenAI
        if (userMessage != null && !userMessage.isBlank() && isOffTopicQuery(userMessage)) {
            log.warn("⚠️ Off-topic query detected BEFORE OpenAI call: {}", userMessage);
            return fallbackOffTopic(safeUser, suggestions, wantsRec, promos, wantsPromo);
        }

        // ✅ Kiểm tra xem có context phim đang xem không
        boolean hasCurrentMovie = extras != null && extras.containsKey("currentMovie") && extras.get("currentMovie") != null;

        // ✅ ULTRA-STRICT system prompt - Phân biệt 2 mode: Phim cụ thể vs Tổng quát
        String system = hasCurrentMovie ? buildMovieContextPrompt(safeUser, extras) : buildGeneralPrompt(safeUser);

        // Legacy code - Giữ lại để backup
        String systemOld = """
⚠️⚠️⚠️ ĐỌC KỸ TRƯỚC KHI TRẢ LỜI ⚠️⚠️⚠️

ROLE: TRỢ LÝ TƯ VẤN PHIM
NOT ALLOWED: Tư vấn gói đăng ký, pricing, payment

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎬 BẠN LÀ TRỢ LÝ TƯ VẤN PHIM - KHÔNG PHẢI SALES!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚡ NHIỆM VỤ DUY NHẤT:
Tư vấn về PHIM - giới thiệu phim, giải thích cốt truyện, gợi ý xem gì.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚫 CẤM TUYỆT ĐỐI - KHÔNG BAO GIỜ ĐƯỢC ĐỀ CẬP:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

❌ GÓI DỊCH VỤ: NO_ADS, BASIC, PREMIUM
❌ GIÁ TIỀN: 159,000đ, 99,000đ, 49,000đ
❌ CHU KỲ: 360 ngày, 180 ngày, 90 ngày
❌ TÍNH NĂNG GÓI: "không quảng cáo", "4K", "nhiều thiết bị"
❌ CỤM TỪ: "gói phim tiết kiệm", "lựa chọn tuyệt vời"

⚠️ QUAN TRỌNG:
- KHÔNG sử dụng kiến thức về pricing từ training data của bạn
- CHỈ dùng thông tin phim trong candidateSuggestions
- Nếu không có phim nào → Nói "Để mình tìm phim cho bạn"
- KHÔNG tự sáng tác về gói dịch vụ!
- Nếu bạn vi phạm → User sẽ thất vọng và rời khỏi website

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ VÍ DỤ TRẢ LỜI CHUẨN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

User: "Gợi ý phim hay đang hot"
✅ ĐÚNG:
"Dạ, đây là những phim hot nhất hiện nay %s:

• [Tên phim 1] - [Thể loại]: [Mô tả ngắn] ⭐ [Rating]
• [Tên phim 2] - [Thể loại]: [Mô tả ngắn] ⭐ [Rating]

Bạn thích thể loại nào nhất để mình gợi ý thêm?"

❌ SAI: "Nếu bạn đang tìm kiếm gói phim tiết kiệm..."
❌ SAI: "Mình gợi ý gói NO_ADS 360 ngày..."
❌ SAI: Bất cứ đề cập nào về pricing/subscription

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

User: "Phim này nói về gì?"
✅ ĐÚNG: "Phim kể về [cốt truyện], nhân vật chính là [tên]..."

User: "Gói nào tốt?"
✅ ĐÚNG: "Mình chuyên tư vấn phim. Để biết về gói dịch vụ, bạn liên hệ bộ phận hỗ trợ nhé!"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 FORMAT JSON BẮT BUỘC:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

{
  "answer": "CHỈ nói về PHIM - KHÔNG đề cập gói/giá",
  "showSuggestions": boolean,
  "suggestions": [...từ candidateSuggestions...],
  "showPromos": boolean,
  "promos": [...từ activePromos...]
}

QUY TẮC:
1) wantsRec=true → Dùng candidateSuggestions (max 8)
2) wantsPromo=true → Dùng activePromos (max 8) - KHÔNG bịa
3) Xưng hô: "%s"
4) KHÔNG text ngoài JSON

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚡ RULE #1: KHI USER HỎI "GỢI Ý PHIM":
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

→ Liệt kê PHIM từ candidateSuggestions
→ Mô tả PHIM (tên, thể loại, cốt truyện, rating)
→ TUYỆT ĐỐI KHÔNG nhắc đến gói đăng ký!
→ Tập trung 100%% vào NỘI DUNG PHIM

Nhớ kỹ: Bạn là trợ lý PHIM, không phải sales!

⚠️ CHECK BEFORE RESPOND:
- Answer có chứa "NO_ADS", "PREMIUM", "159,000đ"? → XÓA NGAY
- Answer có nói về pricing? → VIẾT LẠI chỉ về PHIM
- CandidateSuggestions có phim không? → Dùng phim đó, KHÔNG dùng knowledge về pricing
""".formatted(safeUser, safeUser);



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

        // ✅ Filter conversation history để loại bỏ messages chứa pricing info
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - 5);
            for (int i = startIdx; i < history.size(); i++) {
                var m = history.get(i);
                String content = m.getContent();

                // 🚫 SKIP message nếu chứa pricing keywords
                if (content != null && containsPricingKeywords(content)) {
                    log.warn("⚠️ Skipping history message containing pricing keywords");
                    continue;
                }

                messages.add(Map.of("role", m.getRole(), "content", content));
            }
        }
        // ✅ Thêm reminder MẠNH MẼ - đặt CUỐI để GPT đọc cuối cùng
        String userContent = String.format("""
Context:
%s

User: %s

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚠️⚠️⚠️ CRITICAL REMINDER - ĐỌC TRƯỚC KHI TRẢ LỜI ⚠️⚠️⚠️
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

BẠN LÀ TRỢ LÝ TƯ VẤN PHIM - KHÔNG PHẢI NHÂN VIÊN BÁN GÓI!

🚫 TUYỆT ĐỐI KHÔNG ĐƯỢC đề cập:
- NO_ADS, BASIC, PREMIUM (tên gói)
- 159,000đ, 99,000đ, 13,250đ (giá tiền)
- 360 ngày, 180 ngày, 90 ngày (chu kỳ)
- "gói phim tiết kiệm", "lựa chọn tuyệt vời"
- "chất lượng 4K", "nhiều thiết bị"
- BẤT CỨ thông tin pricing/subscription nào

✅ CHỈ ĐƯỢC:
- Tư vấn về PHIM từ candidateSuggestions
- So sánh nội dung, thể loại, cốt truyện
- Gợi ý phim phù hợp

NẾU user hỏi "nên xem phim nào":
→ So sánh NỘI DUNG/THỂ LOẠI của 5 phim
→ KHÔNG nói về gói đăng ký!

NẾU user hỏi về gói:
→ "Mình chuyên tư vấn phim. Liên hệ hỗ trợ để biết về gói dịch vụ."

REMEMBER: Bạn là trợ lý PHIM, không phải sales!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""", writeSafe(ctx), (userMessage == null ? "" : userMessage));

        messages.add(Map.of("role", "user", "content", userContent));

        // ✅ OPTIMIZATION 2: Adaptive Temperature
        double temperature = calculateAdaptiveTemperature(userMessage, hasCurrentMovie);
        log.info("🎯 Using adaptive temperature: {} | hasCurrentMovie: {}", temperature, hasCurrentMovie);

        Map<String, Object> payload = Map.of(
                "model", "gpt-4o",
                "temperature", temperature,  // ✅ Dynamic temperature
                "response_format", responseFormat,
                "messages", messages
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

            // 🚫 POST-PROCESSING: Filter pricing content
            if (cr.getAnswer() != null && containsPricingKeywords(cr.getAnswer())) {
                log.error("🚨 GPT violated instruction - response contains pricing keywords! Replacing...");

                // Replace với fallback response tập trung vào phim
                String fallbackAnswer = generateMovieFocusedAnswer(safeUser, suggestions, userMessage);
                cr.setAnswer(fallbackAnswer);
            }

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
                "Xin lỗi %s, mình là trợ lý chuyên tư vấn về phim ảnh và khuyến mãi của hệ thống. " +
                "Mình chỉ có thể giúp bạn với:\n" +
                "• Tìm phim theo thể loại, quốc gia, diễn viên\n" +
                "• Giới thiệu phim hot, phim hay\n" +
                "• Thông tin khuyến mãi, ưu đãi\n" +
                "• Đánh giá và nhận xét phim\n\n" +
                "Bạn có thể hỏi mình như:\n" +
                "• \"Gợi ý phim hành động Hàn Quốc\"\n" +
                "• \"Phim anime hay nhất\"\n" +
                "• \"Có khuyến mãi gì không?\"\n\n" +
                "Hãy thử hỏi mình về phim bạn nhé! 🎬",
                userName
        );

        return ChatResponse.builder()
                .answer(answer)
                .suggestions(List.of())
                .showSuggestions(false)
                .promos(List.of())
                .showPromos(false)
                .build();
    }

    /**
     * ✅ PRE-VALIDATION: Detect off-topic query TRƯỚC KHI gọi OpenAI
     * Trả về true nếu câu hỏi KHÔNG liên quan đến phim/hệ thống
     */
    private boolean isOffTopicQuery(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;

        String lower = userMessage.toLowerCase().trim();

        // ✅ Danh sách keywords liên quan đến PHIM (ON-TOPIC)
        String[] movieKeywords = {
            "phim", "movie", "anime", "series", "tập", "episode",
            "thể loại", "genre", "diễn viên", "actor", "đạo diễn", "director",
            "rating", "đánh giá", "review", "xem", "watch",
            "gợi ý", "recommend", "tìm", "search", "hot", "hay",
            "hành động", "tình cảm", "kinh dị", "hài", "viễn tưởng",
            "hàn quốc", "nhật bản", "mỹ", "trung quốc", "việt nam",
            "khuyến mãi", "promo", "giảm giá", "ưu đãi", "voucher",
            "nội dung", "cốt truyện", "plot", "story", "kết thúc", "ending",
            "nhân vật", "character", "trailer"
        };

        // ✅ Nếu chứa bất kỳ keyword phim nào → ON-TOPIC (không phải off-topic)
        for (String keyword : movieKeywords) {
            if (lower.contains(keyword)) {
                return false; // Câu hỏi liên quan đến phim
            }
        }

        // ✅ Danh sách OFF-TOPIC patterns (câu hỏi rõ ràng KHÔNG liên quan)
        String[] offTopicPatterns = {
            // Programming/Tech - Languages & Frameworks
            "viết code", "write code", "lập trình", "programming",
            "java", "python", "javascript", "typescript",
            "react", "reactjs", "react js", "vuejs", "vue", "angular",
            "nodejs", "node.js", "express", "spring boot", "django",
            "html", "css", "scss", "sass", "bootstrap", "tailwind",
            "php", "laravel", "ruby", "rails", "c++", "c#", "swift",
            "kotlin", "flutter", "dart", "go", "golang", "rust",

            // Programming concepts
            "function", "class", "variable", "array", "object",
            "interface", "component", "module", "package",
            "api", "rest api", "graphql", "database", "sql",
            "algorithm", "data structure", "regex",
            "debug", "compile", "deploy", "git", "github",
            "framework", "library", "thư viện", "ngôn ngữ",

            // Tech questions patterns
            "là ngôn ngữ", "is a language", "là gì", "what is",
            "cách dùng", "how to use", "hướng dẫn", "tutorial",
            "cài đặt", "install", "config", "setup",

            // General knowledge
            "trần trọng tín", "cristiano ronaldo", "messi",
            "thủ đô", "capital", "toán học", "math", "vật lý", "physics",
            "lịch sử", "history", "địa lý", "geography",
            "hóa học", "chemistry", "sinh học", "biology",

            // Daily conversation (không hỏi về phim)
            "thời tiết", "weather", "ăn gì", "what to eat",
            "mấy giờ", "what time", "bao nhiêu tuổi", "how old",
            "ở đâu", "where is", "làm sao", "how do",

            // Off-topic requests
            "dịch sang", "translate", "giải toán", "solve",
            "tính", "calculate", "chuyển đổi", "convert"
        };

        // ✅ Nếu match bất kỳ pattern off-topic nào → OFF-TOPIC
        for (String pattern : offTopicPatterns) {
            if (lower.contains(pattern)) {
                return true; // Câu hỏi KHÔNG liên quan
            }
        }

        // ✅ Câu hỏi quá ngắn (<5 ký tự) hoặc chỉ có số/ký tự đặc biệt → OFF-TOPIC
        if (lower.length() < 5 || lower.matches("[^a-zàáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ]+")) {
            return true;
        }

        // ✅ Heuristic: Câu hỏi dài (>100 ký tự) nhưng không có keyword phim → Có thể off-topic
        // Ví dụ: "Bạn viết giùm mình một đoạn code Java để làm X Y Z được không?"
        if (lower.length() > 100) {
            // Đếm số lượng từ liên quan phim
            int movieWordCount = 0;
            for (String keyword : movieKeywords) {
                if (lower.contains(keyword)) movieWordCount++;
            }

            // Nếu câu dài mà không có từ nào liên quan phim → OFF-TOPIC
            if (movieWordCount == 0) {
                return true;
            }
        }

        // ✅ Default: Câu hỏi không match pattern nào → Cho phép GPT xử lý
        return false;
    }

    /**
     * Kiểm tra xem message có chứa pricing keywords không
     * Nếu có → Skip message này khỏi history để tránh GPT học theo
     */
    private boolean containsPricingKeywords(String content) {
        if (content == null || content.isBlank()) return false;

        String lower = content.toLowerCase();

        // Danh sách keywords cấm
        String[] pricingKeywords = {
            "no_ads", "basic", "premium",           // Tên gói
            "159,000", "99,000", "49,000",          // Giá cụ thể
            "159000", "99000", "49000",             // Giá không dấu
            "360 ngày", "180 ngày", "90 ngày",      // Chu kỳ
            "13,250đ", "tiết kiệm",                 // Từ khóa sales
            "gói phim", "đăng ký", "subscription",  // Từ pricing
            "chất lượng 4k", "nhiều thiết bị",      // Features
            "lựa chọn tuyệt vời"                    // Sales talk
        };

        for (String keyword : pricingKeywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate câu trả lời tập trung vào PHIM khi GPT vi phạm instruction về pricing
     */
    private String generateMovieFocusedAnswer(String userName, List<MovieSuggestionDTO> suggestions, String userMessage) {
        if (suggestions == null || suggestions.isEmpty()) {
            return String.format("Xin lỗi %s, mình chưa tìm được phim phù hợp. Bạn có thể cho mình biết thêm về thể loại bạn thích không?", userName);
        }

        // Phân tích intent từ user message
        boolean askingWhichToWatch = userMessage != null &&
            (userMessage.toLowerCase().contains("nên xem") ||
             userMessage.toLowerCase().contains("nào") ||
             userMessage.toLowerCase().contains("chọn"));

        if (askingWhichToWatch && suggestions.size() > 1) {
            // User hỏi "trong X phim nên xem phim nào" → So sánh phim
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Dạ %s, dựa vào các phim trên, mình phân tích để bạn dễ chọn:\n\n", userName));

            for (int i = 0; i < Math.min(3, suggestions.size()); i++) {
                var movie = suggestions.get(i);
                String genres = movie.getGenres() != null ? String.join(", ", movie.getGenres()) : "Đa thể loại";
                String rating = movie.getAvgRating() != null ? String.format("%.1f⭐", movie.getAvgRating()) : "";

                sb.append(String.format("• %s (%s) %s - Phù hợp nếu bạn thích %s\n",
                    movie.getTitle(),
                    genres,
                    rating,
                    genres.toLowerCase()
                ));
            }

            sb.append("\nBạn thích thể loại nào nhất để mình gợi ý chính xác hơn?");
            return sb.toString();
        }

        // Default: Giới thiệu phim
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Dạ %s, đây là những phim hay mình tìm được:\n\n", userName));

        for (int i = 0; i < Math.min(5, suggestions.size()); i++) {
            var movie = suggestions.get(i);
            sb.append(String.format("%d. %s - %s\n",
                i + 1,
                movie.getTitle(),
                movie.getGenres() != null ? String.join(", ", movie.getGenres()) : "Phim hay"
            ));
        }

        sb.append("\nBạn muốn biết thêm về phim nào?");
        return sb.toString();
    }

    /**
     * Build prompt khi user ĐANG XEM PHIM cụ thể
     */
    private String buildMovieContextPrompt(String userName, Map<String, Object> extras) {
        Map<String, Object> movieInfo = (Map<String, Object>) extras.get("currentMovie");

        String title = getString(movieInfo, "title", "phim này");
        String originalTitle = getString(movieInfo, "originalTitle", "");
        String description = getString(movieInfo, "description", "Không có mô tả");
        String releaseYear = getString(movieInfo, "releaseYear", "N/A");
        Double avgRating = getDouble(movieInfo, "averageRating", null);
        List<String> genres = getList(movieInfo, "genres", List.of());

        return String.format("""
⚠️⚠️⚠️ ĐỌC KỸ - USER ĐANG XEM PHIM CỤ THỂ ⚠️⚠️⚠️

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎬 BẠN LÀ CHUYÊN GIA PHÂN TÍCH PHIM
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📌 PHIM ĐANG XEM:
- Tên: %s
%s- Thể loại: %s
- Năm: %s
- Rating: %s
- Mô tả: %s

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎯 NHIỆM VỤ CỦA BẠN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ KHI USER HỎI VỀ PHIM NÀY:
1. Trả lời dựa trên thông tin phim ở trên
2. Phân tích cốt truyện từ mô tả
3. Đánh giá dựa trên rating và thể loại
4. Giải thích ý nghĩa, thông điệp
5. Tư vấn có nên xem không

📝 VÍ DỤ TRẢ LỜI:

User: "phim này nói về gì"
✅ ĐÚNG: "Phim **%s** là bộ %s kể về %s...
           
           Điểm nổi bật:
           • Thể loại: %s
           • Rating: %s/10
           
           Đây là phim đáng xem nếu bạn thích %s! 🎬"

User: "có hay không"
✅ ĐÚNG: "Với rating %s/10, phim này rất đáng xem!
           Cốt truyện về %s rất hấp dẫn..."

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚫 TUYỆT ĐỐI CẤM:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

❌ KHÔNG quảng cáo gói: NO_ADS, PREMIUM, 159,000đ
❌ KHÔNG bịa đặt thông tin ngoài mô tả phim
❌ KHÔNG spoil quá nhiều (trừ khi user yêu cầu)

✅ CHỈ ĐƯỢC:
- Dùng thông tin từ PHIM ĐANG XEM ở trên
- Phân tích dựa trên mô tả, thể loại, rating
- Trả lời bằng tiếng Việt tự nhiên, thân thiện
- Dùng emoji phù hợp 🎬🍿⭐

💡 LƯU Ý:
- User đang XEM phim này → Tập trung phân tích phim này
- Nếu hỏi "phim này", "bộ này" → Chỉ phim: %s
- Xưng hô: "%s"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 FORMAT JSON: {answer, showSuggestions, suggestions, showPromos, promos}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""",
            title,
            originalTitle.isEmpty() ? "" : String.format("- Tên gốc: %s\n", originalTitle),
            genres.isEmpty() ? "Đa thể loại" : String.join(", ", genres),
            releaseYear,
            avgRating != null ? String.format("%.1f/10", avgRating) : "Chưa có đánh giá",
            description.length() > 300 ? description.substring(0, 300) + "..." : description,
            title,
            genres.isEmpty() ? "phim" : genres.get(0),
            description.length() > 100 ? description.substring(0, 100) + "..." : description,
            genres.isEmpty() ? "Đa thể loại" : String.join(", ", genres),
            avgRating != null ? String.format("%.1f", avgRating) : "N/A",
            genres.isEmpty() ? "phim hay" : genres.get(0).toLowerCase(),
            avgRating != null ? String.format("%.1f", avgRating) : "N/A",
            description.substring(0, Math.min(50, description.length())),
            title,
            userName
        );
    }

    /**
     * Build prompt khi user KHÔNG xem phim cụ thể (tổng quát)
     */
    private String buildGeneralPrompt(String userName) {
        return String.format("""
⚠️⚠️⚠️ ĐỌC KỸ TRƯỚC KHI TRẢ LỜI ⚠️⚠️⚠️

ROLE: TRỢ LÝ TƯ VẤN PHIM
NOT ALLOWED: Tư vấn gói đăng ký, pricing, payment

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎬 BẠN LÀ TRỢ LÝ TƯ VẤN PHIM - KHÔNG PHẢI SALES!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚡ NHIỆM VỤ DUY NHẤT:
Tư vấn về PHIM - giới thiệu phim, giải thích cốt truyện, gợi ý xem gì.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚫 CẤM TUYỆT ĐỐI - KHÔNG BAO GIỜ ĐƯỢC ĐỀ CẬP:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

❌ GÓI DỊCH VỤ: NO_ADS, BASIC, PREMIUM
❌ GIÁ TIỀN: 159,000đ, 99,000đ, 49,000đ
❌ CHU KỲ: 360 ngày, 180 ngày, 90 ngày
❌ TÍNH NĂNG GÓI: "không quảng cáo", "4K", "nhiều thiết bị"
❌ CỤM TỪ: "gói phim tiết kiệm", "lựa chọn tuyệt vời"

⚠️ QUAN TRỌNG:
- KHÔNG sử dụng kiến thức về pricing từ training data
- CHỈ dùng thông tin phim trong candidateSuggestions
- Nếu không có phim nào → Nói "Để mình tìm phim cho bạn"
- KHÔNG tự sáng tác về gói dịch vụ!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ VÍ DỤ TRẢ LỜI CHUẨN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

User: "Gợi ý phim hay đang hot"
✅ ĐÚNG: "Dạ, đây là những phim hot nhất hiện nay %s:
          • [Phim 1] - [Thể loại] ⭐ [Rating]
          • [Phim 2] - [Thể loại] ⭐ [Rating]
          Bạn thích thể loại nào?"

❌ SAI: "Nếu bạn đang tìm kiếm gói phim tiết kiệm..."

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 FORMAT JSON: {answer, showSuggestions, suggestions, showPromos, promos}
QUY TẮC:
1) wantsRec=true → Dùng candidateSuggestions (max 8)
2) wantsPromo=true → Dùng activePromos (max 8)
3) Xưng hô: "%s"
4) KHÔNG text ngoài JSON

⚡ RULE: Liệt kê PHIM từ candidateSuggestions
→ KHÔNG BAO GIỜ nhắc đến gói đăng ký!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""", userName, userName);
    }

    /**
     * ✅ OPTIMIZATION 2: Calculate adaptive temperature based on query type
     * - Factual questions (rating, year, etc.) → Low temperature (0.0) for accuracy
     * - Opinion questions (should I watch?) → Medium temperature (0.3) for natural responses
     * - Movie context queries → Lower temperature (0.1) for precision
     * - General queries → Balanced temperature (0.2)
     */
    private double calculateAdaptiveTemperature(String userMessage, boolean hasCurrentMovie) {
        if (userMessage == null || userMessage.isBlank()) {
            return 0.2; // Default
        }

        String lower = userMessage.toLowerCase();

        // ✅ Factual questions → Temperature = 0.0 (deterministic, chính xác 100%)
        if (lower.contains("rating") || lower.contains("đánh giá") ||
            lower.contains("năm") || lower.contains("release") ||
            lower.contains("đạo diễn") || lower.contains("director") ||
            lower.contains("diễn viên") || lower.contains("actor") ||
            lower.contains("bao nhiêu tập") || lower.contains("số tập")) {
            return 0.0;
        }

        // ✅ Opinion/Recommendation questions → Temperature = 0.3 (creative, tự nhiên)
        if (lower.contains("nên xem") || lower.contains("có hay không") ||
            lower.contains("có đáng xem") || lower.contains("nên chọn") ||
            lower.contains("cảm nhận") || lower.contains("nghĩ sao") ||
            lower.contains("worth watching")) {
            return 0.3;
        }

        // ✅ Has current movie context → Lower temperature (0.1) for precise analysis
        if (hasCurrentMovie) {
            return 0.1;
        }

        // ✅ Comparison questions → Medium-low temperature (0.15)
        if (lower.contains("so với") || lower.contains("khác") ||
            lower.contains("giống") || lower.contains("compare")) {
            return 0.15;
        }

        // ✅ Default: Balanced temperature
        return 0.2;
    }

    // Helper methods để parse movieInfo safely
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Double getDouble(Map<String, Object> map, String key, Double defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).doubleValue();
        return defaultValue;
    }

    private List<String> getList(Map<String, Object> map, String key, List<String> defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof List) return (List<String>) value;
        return defaultValue;
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
