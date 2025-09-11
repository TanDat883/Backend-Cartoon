package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.request.ChatRequest;
import flim.backendcartoon.entities.DTO.response.ChatResponse;
import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import flim.backendcartoon.entities.DTO.response.PromoSuggestionDTO;
import flim.backendcartoon.entities.DiscountType;
import flim.backendcartoon.entities.PromotionType;
import flim.backendcartoon.entities.PromotionVoucher;
import flim.backendcartoon.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final UserService userService;
    private final PromotionService promotionService;
    private final PromotionVoucherService voucherService;
    private final RecommendationService recService;
    private final AiService aiService;

    @PostMapping(value = "/chat", produces = "application/json;charset=UTF-8")
    public ResponseEntity<ChatResponse> chat(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody ChatRequest req) {
        String userName = "bạn"; String userId = null;
        if (jwt != null) {
            String phone = safeClaim(jwt,"phone_number");
            String username = phone != null ? phone : safeClaim(jwt,"username");
            if (username == null) username = safeClaim(jwt,"cognito:username");
            if (username != null) {
                var u = userService.findUserByPhoneNumber(username);
                if (u != null) {
                    userId = u.getUserId();
                    if (u.getUserName() != null) userName = u.getUserName();
                }
            }
        }

        String q = req.getMessage() == null ? "" : req.getMessage().toLowerCase();

        boolean wantsRec   = q.matches(".*(gợi ý|đề xuất|phim|xem gì|có phim nào|top|trending|thể loại|hay nhất).*");
        boolean wantsPromo = q.matches(".*(khuyến mãi|khuyen mai|ưu đãi|uu dai|voucher|mã giảm|ma giam|promo|giảm giá|giam gia).*");

        // luôn chuẩn bị đề cử phim
        var candidates = recService.recommendForUser(userId, req.getCurrentMovieId(), 8);

        // Nếu user hỏi voucher/khuyến mãi -> trả dữ liệu có mã thật, bỏ model
        if (wantsPromo) {
            var today = java.time.LocalDate.now();
            var activePromos = promotionService.listAll().stream()
                    .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus())
                            && (p.getEndDate() == null || !p.getEndDate().isBefore(today)))
                    .toList();

            java.util.List<PromoSuggestionDTO> promoCards = new java.util.ArrayList<>();

            for (var p : activePromos) {
                if (p.getPromotionType() == PromotionType.VOUCHER) {
                    // lấy các voucher thuộc promotion này
                    var vouchers = voucherService.getAllPromotionVoucher(p.getPromotionId());
                    for (PromotionVoucher v : vouchers) {
                        Integer percent = (v.getDiscountType() == DiscountType.PERCENTAGE) ? v.getDiscountValue() : null;
                        Integer maxAmt  = v.getMaxDiscountAmount();
                        String note = (v.getDiscountType() == DiscountType.PERCENTAGE)
                                ? ("Giảm " + v.getDiscountValue() + "%, tối đa " + maxAmt)
                                : ("Giảm " + v.getDiscountValue() + ", tối đa " + maxAmt);
                        promoCards.add(new PromoSuggestionDTO(
                                p.getPromotionId(),
                                p.getPromotionName(),
                                "VOUCHER",
                                percent,
                                v.getVoucherCode(),           // ✅ MÃ THẬT
                                maxAmt,
                                p.getStartDate(),
                                p.getEndDate(),
                                p.getStatus(),
                                note
                        ));
                    }
                } else if (p.getPromotionType() == PromotionType.PACKAGE) {
                    // Nếu muốn hỗ trợ khuyến mãi gói, map tương tự từ PromotionPackageService
                    promoCards.add(new PromoSuggestionDTO(
                            p.getPromotionId(), p.getPromotionName(), "PACKAGE",
                            null, null, null, p.getStartDate(), p.getEndDate(), p.getStatus(),
                            p.getDescription()
                    ));
                }
            }

            String answer = promoCards.isEmpty()
                    ? "Hiện chưa có khuyến mãi/voucher đang hoạt động."
                    : "Đây là các khuyến mãi/voucher đang hoạt động. Bấm vào để sao chép mã và dùng khi thanh toán:";

            return ResponseEntity.ok(
                    ChatResponse.builder()
                            .answer(answer)
                            .suggestions(wantsRec ? candidates : java.util.List.of())
                            .showSuggestions(wantsRec && !candidates.isEmpty())
                            .promos(promoCards)
                            .showPromos(!promoCards.isEmpty())
                            .build()
            );
        }

        // Không hỏi khuyến mãi -> dùng AI cho câu trả lời chung + phim gợi ý
        var resp = aiService.composeAnswer(userName, candidates, req.getMessage(), wantsRec, false, java.util.List.of());
        return ResponseEntity.ok(resp);
    }


    @GetMapping(value = "/welcome", produces = "application/json;charset=UTF-8")
    public ResponseEntity<ChatResponse> welcome(@AuthenticationPrincipal Jwt jwt) {
        String userName = "bạn"; String userId = null;
        if (jwt != null) {
            String phone = safeClaim(jwt,"phone_number");
            String username = phone != null ? phone : safeClaim(jwt,"username");
            if (username == null) username = safeClaim(jwt,"cognito:username");
            var u = username==null?null:userService.findUserByPhoneNumber(username);
            if (u != null) { userId = u.getUserId(); if (u.getUserName()!=null) userName = u.getUserName(); }
        }

        var suggestions = recService.recommendForUser(userId, null, 6);
        String answer =
                "Chào " + userName + "! Mình có thể tìm phim theo thể loại, quốc gia, chủ đề, hoặc gợi ý dựa trên sở thích của bạn.\n" +
                        "Bạn thử các câu như:\n- \"Gợi ý phim hành động Hàn\"\n- \"Top phim gia đình hot\"\n- \"Phim chiếu rạp mới\"\nDưới đây là vài đề xuất dành cho bạn:";

        ChatResponse resp = ChatResponse.builder()
                .answer(answer)
                .suggestions(suggestions)
                .showSuggestions(true)
                .promos(List.of())
                .showPromos(false)
                .build();

        return ResponseEntity.ok(resp);
    }

    private String safeClaim(Jwt jwt, String key){
        try { return jwt.getClaimAsString(key); } catch (Exception e) { return null; }
    }
}
