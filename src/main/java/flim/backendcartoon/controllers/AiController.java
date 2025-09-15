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
    private final PromotionVoucherService voucherService;
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
        boolean wantsRec = containsAny(q, "goi y","de xuat","xem gi","nen xem","top","trending","hay nhat","phu hop");

        // Phim ngữ cảnh
        Movie current = isBlank(req.getCurrentMovieId()) ? null : movieService.findMovieById(req.getCurrentMovieId());
        List<Movie> mentioned = findMentionedMovies(q);

        // Nếu hỏi thông tin phim → tắt gợi ý
        boolean asksInfo = current != null || !mentioned.isEmpty()
                || containsAny(q, "thong tin","noi dung","tom tat","bao nhieu tap","may tap",
                "trailer","danh gia","rating","nam phat hanh","quoc gia",
                "dien vien","dao dien","season","phan","tap");
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


        ChatResponse resp = aiService.composeAnswer(
                user.userName, candidates, rawQ, prev, wantsRec, false, List.of(), extras
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

    private ChatResponse buildPromoResponse(boolean wantsRec, List<MovieSuggestionDTO> candidates) {
        LocalDate today = LocalDate.now();
        var activePromos = promotionService.listAll().stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())
                        && (p.getEndDate() == null || !p.getEndDate().isBefore(today)))
                .toList();

        List<PromoSuggestionDTO> promoCards = new ArrayList<>();
        for (var p : activePromos) {
            if (p.getPromotionType() == PromotionType.VOUCHER) {
                for (PromotionVoucher v : voucherService.getAllPromotionVoucher(p.getPromotionId())) {
                    Integer percent = (v.getDiscountType() == DiscountType.PERCENTAGE) ? v.getDiscountValue() : null;
                    Integer maxAmt  = v.getMaxDiscountAmount();
                    String note = (v.getDiscountType() == DiscountType.PERCENTAGE)
                            ? ("Giảm " + v.getDiscountValue() + "%, tối đa " + maxAmt)
                            : ("Giảm " + v.getDiscountValue() + ", tối đa " + maxAmt);
                    promoCards.add(new PromoSuggestionDTO(
                            p.getPromotionId(), p.getPromotionName(), "VOUCHER",
                            percent, v.getVoucherCode(), maxAmt,
                            p.getStartDate(), p.getEndDate(), p.getStatus(), note
                    ));
                }
            } else if (p.getPromotionType() == PromotionType.PACKAGE) {
                promoCards.add(new PromoSuggestionDTO(
                        p.getPromotionId(), p.getPromotionName(), "PACKAGE",
                        null, null, null, p.getStartDate(), p.getEndDate(), p.getStatus(), p.getDescription()
                ));
            }
        }

        String answer = promoCards.isEmpty()
                ? "Hiện chưa có khuyến mãi/voucher đang hoạt động."
                : "Đây là các khuyến mãi/voucher đang hoạt động. Bấm vào để sao chép mã và dùng khi thanh toán:";

        return ChatResponse.builder()
                .answer(answer)
                .suggestions(wantsRec ? candidates : List.of())
                .showSuggestions(wantsRec && !candidates.isEmpty())
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
        info.put("releaseYear", m.getReleaseYear());
        info.put("status", m.getStatus()==null? null : m.getStatus().name());
        info.put("minVipLevel", m.getMinVipLevel()==null? "FREE" : m.getMinVipLevel().name());
        info.put("thumbnailUrl", m.getThumbnailUrl());
        info.put("trailerUrl", m.getTrailerUrl());
        info.put("totalSeasons", seasons.size());
        info.put("totalEpisodes", eps);

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
