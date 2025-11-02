package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.*;
import flim.backendcartoon.entities.DTO.request.ChatRequest;
import flim.backendcartoon.entities.DTO.response.ChatResponse;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import flim.backendcartoon.entities.DTO.response.PromoSuggestionDTO;
import flim.backendcartoon.exception.AuthorException;
import flim.backendcartoon.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private static final int HISTORY_LIMIT = 12;

    private final UserService userService;
    private final PromotionService promotionService;
    private final PromotionLineService promotionLineService;
    private final PromotionDetailService promotionDetailService;
    private final RecommendationService recService;
    private final SeasonService seasonService;
    private final EpisodeService episodeService;
    private final AuthorService authorService;
    private final MovieService movieService;
    private final AiService aiService;
    private final ChatMemoryService memory;
    private final IntentParser intentParser;
    private final MovieFilterService movieFilterService;

    /* ============================ PUBLIC APIs ============================ */

    @PostMapping(value = "/chat", produces = "application/json;charset=UTF-8")
    public ResponseEntity<ChatResponse> chat(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody ChatRequest req) throws AuthorException {
        // ✅ Start timing for end-to-end latency measurement
        long tStart = System.currentTimeMillis();

        var user = resolveUser(jwt);
        final String convId = nullSafe(req.getConversationId());

        final String rawQ = nullSafe(req.getMessage());
        final String q = vnNorm(rawQ); // chuẩn hoá để match không dấu

        // ✅ FAST-PATH: Parse intent trước khi xử lý logic phức tạp
        IntentParser.Intent intent = intentParser.parse(rawQ);
        log.info("⏱️ Intent parsed | isPureFilter={} | genres={} | countries={} | wantsPromo={} | wantsRec={}",
                intent.isPureFilter(), intent.getGenres(), intent.getCountries(),
                intent.isWantsPromo(), intent.isWantsRec());

        // ✅ OFF-TOPIC DETECTION: Detect obviously off-topic queries to avoid timeout
        if (isObviouslyOffTopic(rawQ, intent)) {
            log.warn("⚠️ Off-topic query detected: {}", rawQ);
            ChatResponse offTopicResponse = handleOffTopicQuery(user.userName, convId);
            long tEnd = System.currentTimeMillis();
            log.info("⏱️ Off-topic handled | latency={}ms | no_llm_call=true", (tEnd - tStart));
            return ResponseEntity.ok(offTopicResponse);
        }

        // ✅ FAST-PATH: Xử lý query lọc thuần KHÔNG gọi LLM
        if (intent.isPureFilter()) {
            ChatResponse fastResponse = handlePureFilterQuery(intent, user.userName, convId, rawQ);
            long tEnd = System.currentTimeMillis();
            log.info("⏱️ Fast-path completed | latency={}ms | no_llm_call=true", (tEnd - tStart));
            return ResponseEntity.ok(fastResponse);
        }

        // Ý định người dùng (fallback từ rule cũ)
        final boolean wantsPromo = intent.isWantsPromo() || containsAny(q, "khuyen mai","uu dai","voucher","ma giam","promo","giam gia");

        // ✅ NEW: Detect pricing queries
        final boolean wantsPricing = containsAny(q,
            "goi dang ky","goi nao","goi gi","goi thanh vien",
            "gia tien","gia ca","bao nhieu tien","phi","cost","price",
            "premium","basic","vip","membership","subscription",
            "dang ky","mua goi","thanh toan");

        // Phim ngữ cảnh
        Movie current = isBlank(req.getCurrentMovieId()) ? null : movieService.findMovieById(req.getCurrentMovieId());
        List<Movie> mentioned = findMentionedMovies(q);

        //nhận diện gợi ý
        final boolean explicitRec = intent.isWantsRec() || containsAny(q,
                "goi y","de xuat","xem gi","nen xem","top","trending",
                "hay nhat","phu hop",
                "phim nao hay","co phim nao hay","co gi xem", "hay khong", "hay ko",
                "recommend","suggest"
        );
        // Nếu hỏi thông tin phim → tắt gợi ý
        boolean asksInfo = intent.isAsksInfo() || current != null || !mentioned.isEmpty()
                || containsAny(q, "thong tin","noi dung","tom tat","bao nhieu tap","may tap",
                "trailer","danh gia","rating","nam phat hanh","quoc gia","luot xem",
                "dien vien","dao dien","season","phan","tap");

        // ✅ FIX: Pricing queries should NOT show movie recommendations
        boolean wantsRec = explicitRec || (!asksInfo && !wantsPromo && !wantsPricing);
        if (asksInfo || wantsPricing) wantsRec = false;

        // Candidate suggestions: ưu tiên những gì đã hiển thị ở phiên trước
        List<MovieSuggestionDTO> prior = isBlank(convId) ? List.of() : memory.getSuggestions(convId);
        List<MovieSuggestionDTO> candidates = !prior.isEmpty()
                ? prior
                : recService.recommendForUser(user.userId, req.getCurrentMovieId(), 8);

        // --- Nếu user hỏi theo thể loại (vd: anime/hoạt hình) → ghi đè candidates bằng danh sách đã lọc ---
        Set<String> wantedGenres = detectWantedGenres(q);
        if (!wantedGenres.isEmpty()) {
            var filtered = movieService.findAllMovies().stream()
                    .filter(m -> movieHasAnyGenreNormalized(m, wantedGenres))
                    .sorted((a,b) -> Long.compare(
                            (b.getViewCount()==null?0:b.getViewCount()),
                            (a.getViewCount()==null?0:a.getViewCount())))
                    .limit(8)
                    .map(m -> new MovieSuggestionDTO(
                            m.getMovieId(), m.getTitle(), m.getThumbnailUrl(),
                            m.getGenres(), m.getViewCount(), m.getAvgRating()))
                    .toList();

            if (!filtered.isEmpty()) {
                candidates = filtered;
            }
        }

        
        // Lịch sử hội thoại
        List<ChatMemoryService.ChatMsg> prev = isBlank(convId)
                ? List.of()
                : memory.history(convId, HISTORY_LIMIT);

        // ✅ NEW: Nếu hỏi về pricing/gói đăng ký → trả thông tin trực tiếp với AI consultation
        if (wantsPricing) {
            log.info("⏱️ Pricing query detected | building INTELLIGENT pricing response...");
            ChatResponse pricingResp = buildPricingResponse(user.userName, rawQ);  // Pass user query for AI
            log.info("✅ Pricing response built with AI consultation | NO movie suggestions");
            persistMemory(convId, rawQ, pricingResp.getAnswer(), pricingResp.getSuggestions(), false);
            long tEnd = System.currentTimeMillis();
            log.info("⏱️ Pricing query completed | latency={}ms", (tEnd - tStart));
            return ResponseEntity.ok(pricingResp);
        }

        // Nếu hỏi khuyến mãi → trả thẳng dữ liệu, không gọi AI
        if (wantsPromo) {
            log.info("⏱️ Promo query detected | building promo response...");
            ChatResponse promoResp = buildPromoResponse(wantsRec, candidates);
            log.info("✅ Promo response built | promos_count={} | has_promos={}",
                    promoResp.getPromos() != null ? promoResp.getPromos().size() : 0,
                    promoResp.getShowPromos());
            persistMemory(convId, rawQ, promoResp.getAnswer(), promoResp.getSuggestions(), wantsRec);
            long tEnd = System.currentTimeMillis();
            log.info("⏱️ Promo query completed | latency={}ms", (tEnd - tStart));
            return ResponseEntity.ok(promoResp);
        }

        // Gọi AI với đầy đủ context (phim hiện tại + phim được nhắc)
        List<Map<String, ?>> mentionedInfos = mentioned.stream()
                .map(m -> {
                    try { return toMovieInfo(m); }
                    catch (AuthorException e) {
                        log.warn("toMovieInfo failed: {}", e.getMessage());
                        return Map.of("movieId", m.getMovieId(), "title", m.getTitle()); // fallback tối thiểu
                    }
                })
                .collect(Collectors.toList());

        Map<String,Object> extras = new HashMap<>();
        extras.put("currentMovie", toMovieInfo(current)); // chỗ này cũng phải try/catch nếu vẫn throws
        extras.put("mentionedMovies", mentionedInfos);

        // === NEW: gom khuyến mãi nếu user hỏi ===
        List<PromoSuggestionDTO> promos = wantsPromo ? collectActivePromos() : List.of();

        ChatResponse resp = aiService.composeAnswer(
                user.userName, candidates, rawQ, prev, wantsRec, wantsPromo, promos, extras
        );

        // Lưu lịch sử + danh sách đề xuất đã hiển thị (để hiểu "hai phim đó" ở lượt sau)
        persistMemory(convId, rawQ, resp.getAnswer(),
                (resp.getShowSuggestions()!=null && resp.getShowSuggestions())
                        ? nullSafe(resp.getSuggestions()) : candidates,
                wantsRec);

        // ✅ Log end-to-end latency
        long tEnd = System.currentTimeMillis();
        log.info("⏱️ Chat completed | end_to_end_latency={}ms | llm_called=true", (tEnd - tStart));

        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/welcome", produces = "application/json;charset=UTF-8")
    public ResponseEntity<ChatResponse> welcome(@AuthenticationPrincipal Jwt jwt,
                                                @RequestParam(value="conversationId", required=false) String conversationId) {
        var user = resolveUser(jwt);
        var suggestions = recService.recommendForUser(user.userId, null, 6);

        String answer = "Chào " + user.userName + "! Mình có thể tìm phim theo thể loại, quốc gia, chủ đề, "
                + "hoặc gợi ý dựa trên sở thích của bạn.\nBạn thử các câu như:\n"
                + "- \"Gợi ý phim hành động Hàn\"\n- \"Top phim gia đình hot\"\n- \"Phim chiếu rạp mới\"\n"
                + "Dưới đây là vài đề xuất dành cho bạn:";

        ChatResponse resp = ChatResponse.builder()
                .answer(answer)
                .suggestions(suggestions)
                .showSuggestions(true)
                .promos(List.of())
                .showPromos(false)
                .build();

        if (!isBlank(conversationId)) {
            memory.reset(conversationId);
            memory.append(conversationId, "assistant", answer);
            memory.setSuggestions(conversationId, suggestions);
        }
        return ResponseEntity.ok(resp);
    }

    /* ============================ HELPERS ============================ */

    /**
     * ✅ OFF-TOPIC DETECTION: Phát hiện câu hỏi rõ ràng không liên quan đến phim
     * Tránh gọi OpenAI cho queries như: "trần trọng tín có đỉnh ko", "2+2=?", etc.
     */
    private boolean isObviouslyOffTopic(String query, IntentParser.Intent intent) {
        if (query == null || query.length() < 3) return false;

        String q = vnNorm(query.toLowerCase());

        // Has any movie-related intent? → NOT off-topic
        if (!intent.getGenres().isEmpty() || !intent.getCountries().isEmpty() ||
            intent.isWantsPromo() || intent.isWantsRec() || intent.isAsksInfo()) {
            return false;
        }

        // Check for movie-related keywords
        if (containsAny(q, "phim", "movie", "film", "tap", "episode", "season", "phan",
                "xem", "watch", "trailer", "rating", "danh gia", "dien vien", "actor",
                "dao dien", "director", "the loai", "genre", "quoc gia", "country")) {
            return false;
        }

        // ✅ Obviously off-topic patterns
        // Personal questions about people (not actors/directors)
        if (containsAny(q, "co dinh ko", "co dep ko", "co gioi ko", "co hay ko") &&
            !containsAny(q, "phim", "movie", "tap", "season")) {
            return true;
        }

        // Math questions
        if (q.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*")) {
            return true;
        }

        // General knowledge not related to movies
        if (containsAny(q, "thu do", "capital", "tong thong", "president", "toan hoc", "math") &&
            !containsAny(q, "phim", "movie")) {
            return true;
        }

        // Very short queries without movie keywords (likely random)
        if (q.length() < 15 && !containsAny(q, "phim", "movie", "xem", "goi y", "top", "hay")) {
            return true;
        }

        return false;
    }

    /**
     * ✅ Handle off-topic queries gracefully without calling OpenAI
     */
    private ChatResponse handleOffTopicQuery(String userName, String convId) {
        String answer = String.format(
                "Xin lỗi %s, mình là trợ lý tìm phim nên chỉ có thể giúp bạn với các câu hỏi về phim, " +
                        "thể loại, diễn viên, hoặc gợi ý xem gì. " +
                        "Bạn có thể thử hỏi như:\n" +
                        "• \"Gợi ý phim hành động Hàn Quốc\"\n" +
                        "• \"Phim anime hay nhất\"\n" +
                        "• \"Có khuyến mãi gì không?\"\n\n" +
                        "Dưới đây là vài gợi ý phim hot hiện tại:",
                userName
        );

        // Get top movies as suggestions
        var topMovies = movieFilterService.getTopMovies(8);

        ChatResponse resp = ChatResponse.builder()
                .answer(answer)
                .suggestions(topMovies)
                .showSuggestions(!topMovies.isEmpty())
                .promos(List.of())
                .showPromos(false)
                .build();

        // Persist memory
        persistMemory(convId, "", answer, topMovies, false);

        return resp;
    }

    /**
     * ✅ FAST-PATH: Xử lý query lọc thuần (country/genre/year) KHÔNG gọi LLM
     * Target: ≤300ms server time
     */
    private ChatResponse handlePureFilterQuery(IntentParser.Intent intent, String userName,
                                               String convId, String userMessage) {
        // ✅ SEMANTIC SEARCH: Sử dụng semantic understanding
        // "hoạt hình" → also search "anime", "thiếu nhi", etc.
        var filtered = movieFilterService.filterMoviesWithSemanticFallback(
                intent.getGenres(),
                intent.getCountries(),
                intent.getYearMin(),
                intent.getYearMax(),
                8
        );

        // Build response template
        // Convert country names to Vietnamese for friendly response
        String countriesText = intent.getCountries().isEmpty() ? "" :
                String.join(", ", intent.getCountries().stream()
                        .map(this::toVietnameseCountryName)
                        .toList()) + " ";

        String genresText = intent.getGenres().isEmpty() ? "" :
                "thể loại " + String.join(", ", intent.getGenres().stream()
                        .map(this::toVietnameseGenreName)
                        .toList());

        String answer;
        if (filtered.isEmpty()) {
            // Không tìm thấy → gợi ý thay thế
            answer = String.format("Mình chưa tìm thấy phim %s%s phù hợp. Thử thay đổi bộ lọc hoặc xem gợi ý khác nhé!",
                    countriesText, genresText);

            // ✅ Gợi ý thay thế: lấy phim hot hiện tại
            filtered = movieFilterService.getTopMovies(8);
        } else {
            // ✅ SMART MESSAGE: Giải thích nếu dùng semantic fallback
            // Check if we used semantic expansion (found movies but different genre names)
            boolean usedSemanticFallback = filtered.stream()
                    .anyMatch(m -> m.getGenres() != null &&
                            m.getGenres().stream().noneMatch(g ->
                                    intent.getGenres().stream().anyMatch(wanted ->
                                            vnNorm(g).equals(vnNorm(wanted)))));

            if (usedSemanticFallback && !intent.getGenres().isEmpty()) {
                // Explain semantic match
                answer = String.format("Mình tìm thấy %d phim %sliên quan đến %s cho %s:",
                        filtered.size(),
                        countriesText,
                        genresText,
                        userName);
            } else {
                // Normal match
                answer = String.format("Mình tìm thấy %d phim %s%s cho %s:",
                        filtered.size(),
                        countriesText,
                        genresText,
                        userName);
            }
        }

        ChatResponse resp = ChatResponse.builder()
                .answer(answer)
                .suggestions(filtered)
                .showSuggestions(!filtered.isEmpty())
                .promos(List.of())
                .showPromos(false)
                .build();

        // Persist memory
        persistMemory(convId, userMessage, answer, filtered, true);

        return resp;
    }

    private record UserCtx(String userId, String userName) {}

    private UserCtx resolveUser(Jwt jwt) {
        String name = "bạn"; String uid = null;
        if (jwt != null) {
            String phone = safeClaim(jwt, "phone_number");
            String username = phone != null ? phone : safeClaim(jwt, "username");
            if (username == null) username = safeClaim(jwt, "cognito:username");
            if (username != null) {
                var u = userService.findUserByPhoneNumber(username);
                if (u != null) {
                    uid = u.getUserId();
                    if (u.getUserName() != null) name = u.getUserName();
                }
            }
        }
        return new UserCtx(uid, name);
    }

    // Chuẩn hoá trạng thái để không lệ thuộc đúng chữ "ACTIVE"
    private static String normStatus(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();
        return switch (s) {
            case "hoạt động", "active" -> "ACTIVE";
            case "tạm dừng", "paused" -> "PAUSED";
            case "hết hạn", "expired" -> "EXPIRED";
            case "sắp diễn ra", "upcoming" -> "UPCOMING";
            case "nháp", "draft" -> "DRAFT";
            default -> s.toUpperCase();
        };
    }

    /** Phiên bản mới: build promo từ Promotion + PromotionLine + PromotionDetail */
    private ChatResponse buildPromoResponse(boolean wantsRec, List<MovieSuggestionDTO> candidates) {
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

        // 1) Lọc Promotion đang hoạt động trong khung ngày
        var allPromos = promotionService.listAll();
        log.debug("🎁 Total promotions in DB: {}", allPromos.size());

        var activePromos = allPromos.stream()
                .filter(p -> {
                    String st = normStatus(p.getStatus());
                    boolean okStatus = "ACTIVE".equals(st) || st.isBlank();
                    boolean okStart  = (p.getStartDate() == null) || !today.isBefore(p.getStartDate());
                    boolean okEnd    = (p.getEndDate()   == null) || !today.isAfter(p.getEndDate());
                    boolean result = okStatus && okStart && okEnd;
                    if (!result) {
                        log.debug("❌ Filtered out promo: {} | status={} | startDate={} | endDate={} | today={}",
                                p.getPromotionName(), p.getStatus(), p.getStartDate(), p.getEndDate(), today);
                    }
                    return result;
                })
                .toList();

        log.info("🎁 Active promotions after filter: {} (out of {})", activePromos.size(), allPromos.size());

        var promoCards = new java.util.ArrayList<PromoSuggestionDTO>();

        for (var p : activePromos) {
            // 2) Lấy các Line của promotion và lọc theo trạng thái + ngày
            var lines = promotionLineService.getPromotionLinesByPromotion(p.getPromotionId());
            for (var line : lines) {
                String lst = normStatus(line.getStatus());
                boolean inWindow =
                        (line.getStartDate() == null || !today.isBefore(line.getStartDate())) &&
                                (line.getEndDate()   == null || !today.isAfter(line.getEndDate())) &&
                                ("ACTIVE".equals(lst) || lst.isBlank());
                if (!inWindow) continue;

                switch (line.getPromotionLineType()) {
                    case VOUCHER -> {
                        // 3a) Lấy các voucher thuộc line
                        var vouchers = promotionDetailService.getAllPromotionVoucher(line.getPromotionLineId());
                        for (var v : vouchers) {
                            Integer percent = null;
                            String note = "Ưu đãi voucher";
                            if (v.getDiscountType() != null && v.getDiscountValue() != null) {
                                switch (v.getDiscountType()) {
                                    case PERCENTAGE -> {
                                        percent = v.getDiscountValue();
                                        Long cap = v.getMaxDiscountAmount();
                                        note = (cap != null && cap > 0)
                                                ? ("Giảm " + percent + "%, tối đa " + cap)
                                                : ("Giảm " + percent + "%");
                                    }
                                    case FIXED_AMOUNT -> {
                                        note = "Giảm " + v.getDiscountValue() +
                                                ((v.getMaxDiscountAmount()!=null && v.getMaxDiscountAmount()>0)
                                                        ? (" (tối đa " + v.getMaxDiscountAmount() + ")") : "");
                                    }
                                    default -> {}
                                }
                            }
                            promoCards.add(new PromoSuggestionDTO(
                                    p.getPromotionId(),
                                    p.getPromotionName(),              // -> map vào field 'title' của DTO
                                    "VOUCHER",
                                    percent,
                                    v.getVoucherCode(),
                                    v.getMaxDiscountAmount() == null ? null : v.getMaxDiscountAmount().intValue(),
                                    p.getStartDate(),
                                    p.getEndDate(),
                                    p.getStatus(),
                                    note
                            ));
                        }
                    }
                    case PACKAGE -> {
                        // 3b) Lấy các ưu đãi theo gói
                        var packs = promotionDetailService.getAllPromotionPackages(line.getPromotionLineId());
                        for (var g : packs) {
                            Integer percent = g.getDiscountPercent();
                            String note = (g.getPackageId()!=null && !g.getPackageId().isEmpty())
                                    ? ("Giảm " + percent + "% cho gói: " + String.join(", ", g.getPackageId()))
                                    : ("Giảm " + percent + "% cho các gói áp dụng");
                            promoCards.add(new PromoSuggestionDTO(
                                    p.getPromotionId(),
                                    p.getPromotionName(),
                                    "PACKAGE",
                                    percent,
                                    null,
                                    g.getMaxDiscountAmount() == null ? null : g.getMaxDiscountAmount().intValue(),
                                    p.getStartDate(),
                                    p.getEndDate(),
                                    p.getStatus(),
                                    note
                            ));
                        }
                    }
                }
            }
        }

        String answer = promoCards.isEmpty()
                ? "Hiện chưa có khuyến mãi/voucher đang hoạt động."
                : "Đây là các khuyến mãi/voucher đang hoạt động. Bấm vào để sao chép mã và dùng khi thanh toán:";

        log.info("🎁 Final promo cards: {} | answer: {}", promoCards.size(),
                promoCards.isEmpty() ? "No promos" : "Has promos");

        if (promoCards.isEmpty()) {
            log.warn("⚠️ No active promos found! Check database:");
            log.warn("   - Are there promotions with status='ACTIVE'?");
            log.warn("   - Are start/end dates valid for today ({})?", today);
            log.warn("   - Do promotion lines have status='ACTIVE'?");
            log.warn("   - Do promotion details (vouchers/packages) exist?");
        }

        return ChatResponse.builder()
                .answer(answer)
                .suggestions(wantsRec ? (candidates == null ? java.util.List.of() : candidates) : java.util.List.of())
                .showSuggestions(wantsRec && candidates != null && !candidates.isEmpty())
                .promos(promoCards)
                .showPromos(!promoCards.isEmpty())
                .build();
    }

    private final AssistantPricingService assistantPricingService;

    /**
     * ✅ NEW: Build INTELLIGENT pricing response with AI consultation
     * Analyzes user query and provides smart recommendations
     * ✅ IMPORTANT: This method NEVER returns movie suggestions
     */
    private ChatResponse buildPricingResponse(String userName, String userQuery) {
        log.info("💰 Building INTELLIGENT pricing response for user: {} | query: {}", userName, userQuery);

        try {
            // ✅ Fetch real pricing data from database
            var pricingData = assistantPricingService.getActivePricing(null); // null = today

            if (pricingData.getPackages().isEmpty()) {
                log.warn("⚠️ No active packages found in database");
                return buildPricingErrorResponse();
            }

            // 🎯 Use AI to provide intelligent consultation
            String aiConsultation = buildAIPricingConsultation(userQuery, pricingData);

            log.info("✅ AI pricing consultation generated successfully");

            // ✅ IMPORTANT: Pricing queries should NEVER show movie suggestions!
            return ChatResponse.builder()
                    .answer(aiConsultation)
                    .suggestions(java.util.List.of())  // Always empty for pricing queries
                    .showSuggestions(false)            // Always false for pricing queries
                    .promos(java.util.List.of())
                    .showPromos(false)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error building pricing response: {}", e.getMessage(), e);
            return buildPricingErrorResponse();
        }
    }

    /**
     * 🎯 Build AI-powered pricing consultation
     * Uses OpenAI to analyze user needs and recommend best packages
     */
    private String buildAIPricingConsultation(String userQuery, flim.backendcartoon.entities.DTO.response.AssistantPricingResponse pricingData) {
        try {
            // Build context with pricing data
            StringBuilder pricingContext = new StringBuilder("BẢNG GIÁ:\n\n");

            // Group by type for easier AI understanding
            var byType = pricingData.getPackages().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        flim.backendcartoon.entities.DTO.response.AssistantPackageDTO::getType
                    ));

            byType.forEach((type, packages) -> {
                pricingContext.append(type).append(":\n");
                packages.forEach(pkg -> {
                    pricingContext.append(String.format("  - %d ngày: %,dđ (~%,dđ/tháng)\n",
                        pkg.getDurationDays(), pkg.getPrice(), pkg.getPriceMonthly()));
                });
                if (!packages.isEmpty() && packages.get(0).getFeatures() != null) {
                    pricingContext.append("  Features: ").append(String.join(", ", packages.get(0).getFeatures())).append("\n");
                }
                pricingContext.append("\n");
            });

            // Build prompt for AI
            String fullPrompt = String.format("""
                Bạn là chuyên gia tư vấn gói phim CartoonToo.
                
                %s
                
                Khách hỏi: "%s"
                
                TƯ VẤN THÔNG MINH:
                - Nếu hỏi "rẻ/tiết kiệm" → gợi ý NO_ADS 360 ngày (rẻ nhất: 13,250đ/tháng)
                - Nếu hỏi "4K + nhiều thiết bị" → gợi ý PREMIUM  
                - Nếu so sánh 2 gói → giải thích rõ khác biệt
                - Ngắn gọn 3-5 dòng, thân thiện, dùng emoji
                - ĐỪNG liệt kê hết tất cả gói!
                
                Trả lời:
                """, pricingContext.toString(), userQuery);

            // Use existing AiService with simplified call
            var response = aiService.composeAnswer(
                null,  // userName
                List.of(),  // no movie suggestions
                fullPrompt,  // user message with context
                List.of(),  // no history
                false,  // don't want recommendations
                false,  // don't want promos
                List.of(),  // no promos
                Map.of()  // no extras
            );

            return response.getAnswer();

        } catch (Exception e) {
            log.error("❌ AI consultation failed, falling back to simple response: {}", e.getMessage());
            // Fallback: return simple formatted list
            return buildSimplePricingList(pricingData);
        }
    }

    /**
     * Fallback: Build simple pricing list when AI fails
     */
    private String buildSimplePricingList(flim.backendcartoon.entities.DTO.response.AssistantPricingResponse pricingData) {
        StringBuilder answer = new StringBuilder();
        answer.append("Hôm nay có các gói đăng ký sau (giá từ hệ thống):\n\n");

        // Group packages by type
        var packagesByType = pricingData.getPackages().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    flim.backendcartoon.entities.DTO.response.AssistantPackageDTO::getType
                ));

        // Format NO_ADS packages
        if (packagesByType.containsKey("NO_ADS")) {
            answer.append("📦 **GÓI BỎ QUẢNG CÁO (NO_ADS)**\n");
            formatPackageGroup(answer, packagesByType.get("NO_ADS"));
            answer.append("\n");
        }

        // Format PREMIUM packages
        if (packagesByType.containsKey("PREMIUM")) {
            answer.append("⭐ **GÓI PREMIUM**\n");
            formatPackageGroup(answer, packagesByType.get("PREMIUM"));
            answer.append("\n");
        }

        // Format MEGA_PLUS packages
        if (packagesByType.containsKey("MEGA_PLUS")) {
            answer.append("💎 **GÓI MEGA+**\n");
            formatPackageGroup(answer, packagesByType.get("MEGA_PLUS"));
            answer.append("\n");
        }

        // Format COMBO packages
        if (packagesByType.containsKey("COMBO_PREMIUM_MEGA_PLUS")) {
            answer.append("🎁 **GÓI COMBO PREMIUM & MEGA+**\n");
            formatPackageGroup(answer, packagesByType.get("COMBO_PREMIUM_MEGA_PLUS"));
            answer.append("\n");
        }

        answer.append("💳 Thanh toán qua: Thẻ ATM, Ví điện tử (Momo, ZaloPay), Chuyển khoản\n");
        answer.append("💡 Gói dài hạn có giá trung bình/tháng rẻ hơn!\n");

        return answer.toString();
    }

    /**
     * Format a group of packages (same type, different durations)
     */
    private void formatPackageGroup(StringBuilder answer, List<flim.backendcartoon.entities.DTO.response.AssistantPackageDTO> packages) {
        packages.stream()
                .sorted(java.util.Comparator.comparing(flim.backendcartoon.entities.DTO.response.AssistantPackageDTO::getDurationDays))
                .forEach(pkg -> {
                    answer.append(String.format("   • %d ngày: %,dđ (~%,dđ/tháng)\n",
                        pkg.getDurationDays(),
                        pkg.getPrice(),
                        pkg.getPriceMonthly()
                    ));

                    // Show features for first package in group
                    if (packages.indexOf(pkg) == 0 && pkg.getFeatures() != null && !pkg.getFeatures().isEmpty()) {
                        pkg.getFeatures().forEach(feature ->
                            answer.append("     - ").append(feature).append("\n")
                        );
                    }
                });
    }

    /**
     * Build error response when pricing data is unavailable
     * ✅ IMPORTANT: Never returns movie suggestions
     */
    private ChatResponse buildPricingErrorResponse() {
        String errorMessage = "Xin lỗi, hiện không lấy được dữ liệu gói đăng ký. " +
                              "Vui lòng thử lại sau hoặc liên hệ hỗ trợ.";

        return ChatResponse.builder()
                .answer(errorMessage)
                .suggestions(java.util.List.of())  // Never show movies for pricing errors
                .showSuggestions(false)
                .promos(java.util.List.of())
                .showPromos(false)
                .build();
    }

    /**
     * Persist conversation memory
     */
    private void persistMemory(String convId, String userMsg, String aiAnswer,
                               List<MovieSuggestionDTO> shownSuggestions, boolean wantsRec) {
        if (isBlank(convId)) return;
        if (!isBlank(userMsg)) memory.append(convId, "user", userMsg);
        if (!isBlank(aiAnswer)) memory.append(convId, "assistant", aiAnswer);
        // Lưu danh sách đề xuất đã hiển thị để giữ mạch hội thoại
        if (shownSuggestions != null && (!shownSuggestions.isEmpty() || wantsRec)) {
            memory.setSuggestions(convId, shownSuggestions);
        }
    }

    /**
     * Convert Movie to info map for AI context
     */
    private Map<String, Object> toMovieInfo(Movie m) throws AuthorException {
        if (m == null) return null;
        var seasons = seasonService.findByMovieId(m.getMovieId());
        int eps = seasons.stream().mapToInt(s -> episodeService.countBySeasonId(s.getSeasonId())).sum();

        List<Author> authors;
        try {
            authors = authorService.findAuthorsByMovieId(m.getMovieId());
        } catch (AuthorException e) {
            authors = List.of(); // fallback an toàn
            log.warn("findAuthorsByMovieId failed for {}: {}", m.getMovieId(), e.getMessage());
        }

        var directors  = authors.stream()
                .filter(a -> a.getAuthorRole() == AuthorRole.DIRECTOR)
                .map(Author::getName).toList();

        var performers = authors.stream()
                .filter(a -> a.getAuthorRole() == AuthorRole.PERFORMER)
                .map(Author::getName).toList();

        Map<String, Object> info = new HashMap<>();
        info.put("movieId", m.getMovieId());
        info.put("title", m.getTitle());
        info.put("originalTitle", m.getOriginalTitle());
        info.put("description", m.getDescription());
        info.put("genres", m.getGenres());
        info.put("country", m.getCountry());
        info.put("duration", m.getDuration());
        info.put("releaseYear", m.getReleaseYear());
        info.put("status", m.getStatus()==null? null : m.getStatus().name());
        info.put("minVipLevel", m.getMinVipLevel()==null? "FREE" : m.getMinVipLevel().name());
        info.put("thumbnailUrl", m.getThumbnailUrl());
        info.put("trailerUrl", m.getTrailerUrl());
        info.put("totalSeasons", seasons.size());
        info.put("totalEpisodes", eps);
        info.put("viewCount", m.getViewCount());

        // thêm các field phục vụ QA về tác giả
        info.put("directors", directors);
        info.put("performers", performers);
        info.put("authors", authors.stream()
                .map(a -> Map.of("authorId", a.getAuthorId(),
                        "name", a.getName(),
                        "role", a.getAuthorRole().name()))
                .toList());
        return info;
    }

    /**
     * Find movies mentioned in user query
     */
    private List<Movie> findMentionedMovies(String qNoAccent) {
        return movieService.findAllMovies().stream()
                .filter(m -> {
                    String t = vnNorm(m.getTitle());
                    String o = vnNorm(m.getOriginalTitle());
                    String s = vnNorm(m.getSlug()==null? "" : m.getSlug());
                    return (!t.isEmpty() && qNoAccent.contains(t))
                            || (!o.isEmpty() && qNoAccent.contains(o))
                            || (!s.isEmpty() && qNoAccent.contains(s));
                })
                .limit(5)
                .collect(Collectors.toList());
    }


    /**
     * Collect active promotions
     */
    private List<PromoSuggestionDTO> collectActivePromos() {
        var today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        // 1) Lấy promotions đang ACTIVE và chưa hết hạn
        var activePromos = promotionService.listAll().stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getEndDate() == null || !p.getEndDate().isBefore(today))
                .toList();

        var cards = new ArrayList<PromoSuggestionDTO>();
        for (var p : activePromos) {
            // 2) Lines thuộc promotion
            List<PromotionLine> lines = promotionLineService.getPromotionLinesByPromotion(p.getPromotionId());
            for (var line : lines) {
                // line phải ACTIVE + trong khoảng ngày (repo đã có validate; nhưng ở service ta đã filter status)
                boolean inWindow =
                        (line.getStartDate() == null || !today.isBefore(line.getStartDate())) &&
                                (line.getEndDate()   == null || !today.isAfter(line.getEndDate())) &&
                                "ACTIVE".equalsIgnoreCase(line.getStatus());
                if (!inWindow) continue;

                switch (line.getPromotionLineType()) {
                    case VOUCHER -> {
                        // 3a) VOUCHER details
                        var vouchers = promotionDetailService.getAllPromotionVoucher(line.getPromotionLineId());
                        for (var v : vouchers) {
                            Integer percent = null;
                            String note;
                            if (v.getDiscountType() != null && v.getDiscountValue() != null) {
                                // enum dự án đang dùng PERCENTAGE / FIXED_AMOUNT
                                switch (v.getDiscountType()) {
                                    case PERCENTAGE -> {
                                        percent = v.getDiscountValue();
                                        Long cap = v.getMaxDiscountAmount();
                                        note = (cap != null && cap > 0)
                                                ? ("Giảm " + percent + "%, tối đa " + cap)
                                                : ("Giảm " + percent + "%");
                                    }
                                    case FIXED_AMOUNT -> {
                                        note = "Giảm " + v.getDiscountValue() +
                                                ((v.getMaxDiscountAmount()!=null && v.getMaxDiscountAmount()>0)
                                                        ? (" (tối đa " + v.getMaxDiscountAmount() + ")") : "");
                                    }
                                    default -> note = "Ưu đãi voucher";
                                }
                            } else {
                                note = "Ưu đãi voucher";
                            }

                            cards.add(new PromoSuggestionDTO(
                                    p.getPromotionId(),
                                    p.getPromotionName(),
                                    "VOUCHER",
                                    percent,                               // discountPercent (có thể null)
                                    v.getVoucherCode(),                    // voucherCode
                                    v.getMaxDiscountAmount()==null? null : v.getMaxDiscountAmount().intValue(),
                                    p.getStartDate(),
                                    p.getEndDate(),
                                    p.getStatus(),
                                    note
                            ));
                        }
                    }
                    case PACKAGE -> {
                        // 3b) PACKAGE details
                        var packs = promotionDetailService.getAllPromotionPackages(line.getPromotionLineId());
                        for (var g : packs) {
                            Integer percent = g.getDiscountPercent();
                            String note = (g.getPackageId()!=null && !g.getPackageId().isEmpty())
                                    ? ("Giảm " + percent + "% cho gói: " + String.join(", ", g.getPackageId()))
                                    : ("Giảm " + percent + "% cho các gói áp dụng");
                            cards.add(new PromoSuggestionDTO(
                                    p.getPromotionId(),
                                    p.getPromotionName(),
                                    "PACKAGE",
                                    percent,                   // discountPercent
                                    null,                      // voucherCode
                                    g.getMaxDiscountAmount()==null? null : g.getMaxDiscountAmount().intValue(),
                                    p.getStartDate(),
                                    p.getEndDate(),
                                    p.getStatus(),
                                    note
                            ));
                        }
                    }
                }
            }
        }
        return cards;
    }



    private static final Map<String, List<String>> GENRE_TOKENS = Map.ofEntries(
            Map.entry("Anime", List.of("anime", "hoat hinh", "hoathinh", "cartoon", "manga", "japanese anime")),
            Map.entry("Ẩm Thực", List.of("am thuc", "food", "cooking", "chef", "culinary")),
            Map.entry("Bí Ẩn", List.of("bi an", "mystery", "trinh tham")),
            Map.entry("Chiến Tranh", List.of("chien tranh", "war", "military", "army", "soldier")),
            Map.entry("Chiếu Rạp", List.of("chieu rap", "theatrical", "cinema", "feature film", "movie theater")),
            Map.entry("Chuyển Thể", List.of("chuyen the", "adaptation", "adapted", "based on", "live action adaptation")),
            Map.entry("Chính Kịch", List.of("chinh kich", "drama", "dramatics")),
            Map.entry("Chính Luận", List.of("chinh luan", "commentary", "op ed", "political commentary")),
            Map.entry("Chính Trị", List.of("chinh tri", "politics", "political")),
            Map.entry("Chương Trình Truyền Hình", List.of("chuong trinh truyen hinh", "tv show", "television", "tv series", "variety show")),
            Map.entry("Cung Đấu", List.of("cung dau", "palace drama", "palace intrigue", "court intrigue")),
            Map.entry("Cuối Tuần", List.of("cuoi tuan", "weekend", "weekend special")),
            Map.entry("Cách Mạng", List.of("cach mang", "revolution", "revolutionary")),
            Map.entry("Cổ Trang", List.of("co trang", "period", "costume drama", "historical costume")),
            Map.entry("Cổ Tích", List.of("co tich", "fairy tale", "folklore")),
            Map.entry("Cổ Điển", List.of("co dien", "classic", "classical")),
            Map.entry("DC", List.of("dc", "dc comics", "dc universe", "dceu")),
            Map.entry("Disney", List.of("disney", "pixar", "walt disney")),
            Map.entry("Đau Thương", List.of("dau thuong", "tragic", "tragedy", "melodrama")),
            Map.entry("Gia Đình", List.of("gia dinh", "family", "family friendly", "family drama")),
            Map.entry("Giáng Sinh", List.of("giang sinh", "christmas", "noel", "holiday")),
            Map.entry("Giả Tưởng", List.of("gia tuong", "fantasy", "ky ao")),
            Map.entry("Hoàng Cung", List.of("hoang cung", "imperial palace", "royal court", "palace")),
            Map.entry("Hoạt Hình", List.of("hoat hinh", "animation", "animated", "cartoon")),
            Map.entry("Hài", List.of("hai", "hai huoc", "comedy", "funny", "sitcom")),
            Map.entry("Hành Động", List.of("hanh dong", "action", "fight", "combat")),
            Map.entry("Hình Sự", List.of("hinh su", "crime", "police", "detective", "trinh tham")),
            Map.entry("Học Đường", List.of("hoc duong", "school", "campus", "high school", "college")),
            Map.entry("Khoa Học", List.of("khoa hoc", "science", "scientific")),
            Map.entry("Kinh Dị", List.of("kinh di", "horror", "scary", "ma", "ghost")),
            Map.entry("Kinh Điển", List.of("kinh dien", "classic", "cult classic")),
            Map.entry("Kịch Nói", List.of("kich noi", "stage play", "theatre", "theater")),
            Map.entry("Kỳ Ảo", List.of("ky ao", "fantasy", "huyen ao", "mythic")),
            Map.entry("LGBT+", List.of("lgbt", "lgbt plus", "dong tinh", "queer", "bl", "gl", "yuri", "yaoi")),
            Map.entry("Live Action", List.of("live action", "phim nguoi dong", "live action adaptation")),
            Map.entry("Lãng Mạn", List.of("lang man", "romance", "romcom", "love")),
            Map.entry("Lịch Sử", List.of("lich su", "history", "historical")),
            Map.entry("Marvel", List.of("marvel", "mcu", "marvel studios", "avengers")),
            Map.entry("Miền Viễn Tây", List.of("mien vien tay", "western", "cowboy")),
            Map.entry("Nghề Nghiệp", List.of("nghe nghiep", "workplace", "career", "professional")),
            Map.entry("Người Mẫu", List.of("nguoi mau", "model", "fashion model", "supermodel", "fashion")),
            Map.entry("Nhạc Kịch", List.of("nhac kich", "musical", "broadway")),
            Map.entry("Phiêu Lưu", List.of("phieu luu", "adventure", "quest", "journey")),
            Map.entry("Phép Thuật", List.of("phep thuat", "magic", "wizard", "witch", "sorcery")),
            Map.entry("Siêu Anh Hùng", List.of("sieu anh hung", "superhero", "hero", "vigilante")),
            Map.entry("Thiếu Nhi", List.of("thieu nhi", "kids", "children", "childrens")),
            Map.entry("Thần Thoại", List.of("than thoai", "mythology", "mythic", "legend")),
            Map.entry("Thể Thao", List.of("the thao", "sports", "soccer", "football", "basketball")),
            Map.entry("Truyền Hình Thực Tế", List.of("truyen hinh thuc te", "reality tv", "reality show")),
            Map.entry("Tuổi Trẻ", List.of("tuoi tre", "youth", "teen", "coming of age")),
            Map.entry("Tài Liệu", List.of("tai lieu", "documentary", "docu", "doc")),
            Map.entry("Tâm Lý", List.of("tam ly", "psychological", "psychology", "psychodrama")),
            Map.entry("Tình Cảm", List.of("tinh cam", "romance", "melodrama", "love story")),
            Map.entry("Tập Luyện", List.of("tap luyen", "training", "fitness", "gym", "workout")),
            Map.entry("Viễn Tưởng", List.of("vien tuong", "sci fi", "science fiction", "scifi")),
            Map.entry("Võ Thuật", List.of("vo thuat", "martial arts", "kung fu", "wushu", "karate")),
            Map.entry("Xuyên Không", List.of("xuyen khong", "time travel", "isekai", "transmigration")),
            Map.entry("Đời Thường", List.of("doi thuong", "slice of life")),
            Map.entry("T13+", List.of("t13", "13+", "teen", "pg 13")),
            Map.entry("T18+", List.of("t18", "18+", "adult"))
    );

    /**
     * Detect wanted genres from query
     */
    private Set<String> detectWantedGenres(String qNoAccent) {
        Set<String> wanted = new java.util.HashSet<>();
        GENRE_TOKENS.forEach((canonical, tokens) -> {
            for (String t : tokens) if (qNoAccent.contains(t)) { wanted.add(canonical); break; }
        });
        return wanted;
    }

    /**
     * Check if movie has any wanted genres (normalized)
     */
    private boolean movieHasAnyGenreNormalized(Movie m, Set<String> wantedGenres) {
        if (m.getGenres()==null || wantedGenres.isEmpty()) return false;
        var set = new java.util.HashSet<String>();
        for (String g : m.getGenres()) set.add(vnNorm(g));
        for (String w : wantedGenres) if (set.contains(vnNorm(w))) return true;
        return false;
    }


    /* ============================ UTIL ============================ */

    private String safeClaim(Jwt jwt, String key) {
        try { return jwt.getClaimAsString(key); } catch (Exception e) { return null; }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String nullSafe(String s) { return s == null ? "" : s; }

    /** Chuẩn hoá tiếng Việt: bỏ dấu, thường hoá, loại ký tự lạ, co cụm spaces */
    private static String vnNorm(String s) {
        if (s == null) return "";
        String noAccent = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccent.toLowerCase().replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String t : tokens) if (text.contains(t)) return true;
        return false;
    }

    private static <T> List<T> nullSafe(List<T> list) { return list == null ? List.of() : list; }

    /**
     * Convert English country name to Vietnamese for friendly display
     */
    private String toVietnameseCountryName(String englishName) {
        return switch (englishName) {
            case "South Korea" -> "Hàn Quốc";
            case "Japan" -> "Nhật Bản";
            case "United States" -> "Mỹ";
            case "China" -> "Trung Quốc";
            case "Thailand" -> "Thái Lan";
            case "Vietnam" -> "Việt Nam";
            case "Taiwan" -> "Đài Loan";
            case "Hong Kong" -> "Hồng Kông";
            case "United Kingdom" -> "Anh";
            case "France" -> "Pháp";
            default -> englishName; // Giữ nguyên nếu không có mapping
        };
    }

    /**
     * Convert genre key to Vietnamese for friendly display
     */
    private String toVietnameseGenreName(String genreKey) {
        return switch (genreKey) {
            case "hanh dong" -> "hành động";
            case "hai" -> "hài";
            case "tinh cam" -> "tình cảm";
            case "kinh di" -> "kinh dị";
            case "hoat hinh" -> "hoạt hình";
            case "phieu luu" -> "phiêu lưu";
            case "tam ly" -> "tâm lý";
            case "gia dinh" -> "gia đình";
            case "vien tuong" -> "viễn tưởng";
            case "khoa hoc" -> "khoa học";
            case "chien tranh" -> "chiến tranh";
            case "vo thuat" -> "võ thuật";
            case "bi an" -> "bí ẩn";
            case "hinh su" -> "hình sự";
            case "the thao" -> "thể thao";
            default -> genreKey; // Giữ nguyên nếu không có mapping
        };
    }
}
