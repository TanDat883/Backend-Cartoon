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

    /* ============================ PUBLIC APIs ============================ */

    @PostMapping(value = "/chat", produces = "application/json;charset=UTF-8")
    public ResponseEntity<ChatResponse> chat(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody ChatRequest req) throws AuthorException {
        var user = resolveUser(jwt);
        final String convId = nullSafe(req.getConversationId());

        final String rawQ = nullSafe(req.getMessage());
        final String q = vnNorm(rawQ); // chuẩn hoá để match không dấu

        // Ý định người dùng
        final boolean wantsPromo = containsAny(q, "khuyen mai","uu dai","voucher","ma giam","promo","giam gia");

        // Phim ngữ cảnh
        Movie current = isBlank(req.getCurrentMovieId()) ? null : movieService.findMovieById(req.getCurrentMovieId());
        List<Movie> mentioned = findMentionedMovies(q);

        //nhận diện gợi ý
        final boolean explicitRec = containsAny(q,
                "goi y","de xuat","xem gi","nen xem","top","trending",
                "hay nhat","phu hop",
                "phim nao hay","co phim nao hay","co gi xem", "hay khong", "hay ko",
                "recommend","suggest"
        );
        // Nếu hỏi thông tin phim → tắt gợi ý
        boolean asksInfo = current != null || !mentioned.isEmpty()
                || containsAny(q, "thong tin","noi dung","tom tat","bao nhieu tap","may tap",
                "trailer","danh gia","rating","nam phat hanh","quoc gia","luot xem",
                "dien vien","dao dien","season","phan","tap");
        boolean wantsRec = explicitRec || (!asksInfo && !wantsPromo);
        if (asksInfo) wantsRec = false;

        // Candidate suggestions: ưu tiên những gì đã hiển thị ở phiên trước
        List<MovieSuggestionDTO> prior = isBlank(convId) ? List.of() : memory.getSuggestions(convId);
        List<MovieSuggestionDTO> candidates = !prior.isEmpty()
                ? prior
                : recService.recommendForUser(user.userId, req.getCurrentMovieId(), 8);

        // Lịch sử hội thoại
        List<ChatMemoryService.ChatMsg> prev = isBlank(convId)
                ? List.of()
                : memory.history(convId, HISTORY_LIMIT);

        // Nếu hỏi khuyến mãi → trả thẳng dữ liệu, không gọi AI
        if (wantsPromo) {
            ChatResponse promoResp = buildPromoResponse(wantsRec, candidates);
            persistMemory(convId, rawQ, promoResp.getAnswer(), promoResp.getSuggestions(), wantsRec);
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

        // Lưu lịch sử + danh sách đề xuất đã hiển thị (để hiểu “hai phim đó” ở lượt sau)
        persistMemory(convId, rawQ, resp.getAnswer(),
                (resp.getShowSuggestions()!=null && resp.getShowSuggestions())
                        ? nullSafe(resp.getSuggestions()) : candidates,
                wantsRec);

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
        var activePromos = promotionService.listAll().stream()
                .filter(p -> {
                    String st = normStatus(p.getStatus());
                    boolean okStatus = "ACTIVE".equals(st) || st.isBlank();
                    boolean okStart  = (p.getStartDate() == null) || !today.isBefore(p.getStartDate());
                    boolean okEnd    = (p.getEndDate()   == null) || !today.isAfter(p.getEndDate());
                    return okStatus && okStart && okEnd;
                })
                .toList();

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

        return ChatResponse.builder()
                .answer(answer)
                .suggestions(wantsRec ? (candidates == null ? java.util.List.of() : candidates) : java.util.List.of())
                .showSuggestions(wantsRec && candidates != null && !candidates.isEmpty())
                .promos(promoCards)
                .showPromos(!promoCards.isEmpty())
                .build();
    }


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
}
