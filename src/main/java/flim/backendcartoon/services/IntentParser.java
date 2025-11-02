package flim.backendcartoon.services;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Rule-based intent extractor cho fast-path.
 * Phát hiện query lọc thuần (country, genre, year...) để tránh gọi LLM không cần thiết.
 */
@Component
public class IntentParser {

    @Data
    public static class Intent {
        private boolean isPureFilter; // true = chỉ lọc, không cần LLM
        private Set<String> genres = new HashSet<>();
        private Set<String> countries = new HashSet<>();
        private Integer yearMin;
        private Integer yearMax;
        private boolean wantsPromo;
        private boolean wantsRec;
        private boolean asksInfo; // hỏi thông tin chi tiết về phim
    }

    // Map genre keywords
    private static final Map<String, Set<String>> GENRE_KEYWORDS = Map.ofEntries(
            Map.entry("hanh dong", Set.of("hanh dong", "action", "fight")),
            Map.entry("hai", Set.of("hai", "comedy", "hai huoc", "sitcom")),
            Map.entry("tinh cam", Set.of("tinh cam", "romance", "lang man", "love")),
            Map.entry("kinh di", Set.of("kinh di", "horror", "ma", "scary", "ghost")),
            Map.entry("hoat hinh", Set.of("hoat hinh", "anime", "cartoon", "animation", "애니메이션")),
            Map.entry("phieu luu", Set.of("phieu luu", "adventure", "quest")),
            Map.entry("tam ly", Set.of("tam ly", "drama", "chinh kich", "psychological")),
            Map.entry("gia dinh", Set.of("gia dinh", "family", "tre em", "kids")),
            Map.entry("vien tuong", Set.of("vien tuong", "fantasy", "than thoai", "magic")),
            Map.entry("khoa hoc", Set.of("khoa hoc", "sci-fi", "science fiction", "vien tuong khoa hoc")),
            Map.entry("chien tranh", Set.of("chien tranh", "war", "military", "army", "soldier")),
            Map.entry("vo thuat", Set.of("vo thuat", "martial arts", "kung fu", "wushu")),
            Map.entry("bi an", Set.of("bi an", "mystery", "trinh tham", "detective")),
            Map.entry("hinh su", Set.of("hinh su", "crime", "police", "criminal")),
            Map.entry("the thao", Set.of("the thao", "sports", "sport"))
    );

    // Map country keywords → CANONICAL ENGLISH NAME (match database)
    // Key = Canonical name in DB, Value = Vietnamese + English keywords
    private static final Map<String, Set<String>> COUNTRY_KEYWORDS = Map.of(
            "South Korea", Set.of("han quoc", "han", "korea", "korean", "south korea", "대한민국", "kdrama"),
            "Japan", Set.of("nhat ban", "nhat", "japan", "japanese", "日本"),
            "United States", Set.of("my", "america", "american", "usa", "united states", "hollywood"),
            "China", Set.of("trung quoc", "trung", "china", "chinese", "中国"),
            "Thailand", Set.of("thai lan", "thai", "thailand"),
            "Vietnam", Set.of("viet nam", "viet", "vietnam", "vietnamese"),
            "Taiwan", Set.of("dai loan", "taiwan", "taiwanese", "台灣"),
            "Hong Kong", Set.of("hong kong", "hongkong", "hk", "香港"),
            "United Kingdom", Set.of("uk", "britain", "british", "united kingdom", "england", "phim anh"),  // Removed standalone "anh"
            "France", Set.of("phap", "france", "french")
    );

    // Info question patterns
    private static final Set<String> INFO_KEYWORDS = Set.of(
            "thong tin", "noi dung", "tom tat", "bao nhieu tap", "may tap",
            "trailer", "danh gia", "rating", "nam phat hanh", "luot xem",
            "dien vien", "dao dien", "season", "phan", "tap", "xem o dau"
    );

    // Promo patterns
    private static final Set<String> PROMO_KEYWORDS = Set.of(
            "khuyen mai", "uu dai", "voucher", "ma giam", "promo", "giam gia",
            "discount", "coupon", "sale"
    );

    // Recommendation patterns
    private static final Set<String> REC_KEYWORDS = Set.of(
            "goi y", "de xuat", "xem gi", "nen xem", "top", "trending",
            "hay nhat", "phu hop", "phim nao hay", "co phim nao hay", "co gi xem",
            "recommend", "suggest", "hay khong", "hay ko"
    );

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

    public Intent parse(String query) {
        Intent intent = new Intent();
        if (query == null || query.isBlank()) {
            return intent;
        }

        String q = vnNorm(query.toLowerCase());

        // Detect promo intent
        intent.setWantsPromo(containsAny(q, PROMO_KEYWORDS));

        // Detect recommendation intent
        intent.setWantsRec(containsAny(q, REC_KEYWORDS));

        // Detect info questions (asks about specific movie details)
        intent.setAsksInfo(containsAny(q, INFO_KEYWORDS));

        // Extract genres
        for (Map.Entry<String, Set<String>> entry : GENRE_KEYWORDS.entrySet()) {
            if (containsAny(q, entry.getValue())) {
                intent.getGenres().add(entry.getKey());
            }
        }

        // Extract countries (with context-aware filtering)
        for (Map.Entry<String, Set<String>> entry : COUNTRY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                // Skip single-letter or ambiguous keywords in question contexts
                if (keyword.length() <= 2 && (q.contains("hay ko") || q.contains("hay khong") ||
                    q.contains("nao hay") || q.contains("gi hay"))) {
                    continue; // Skip ambiguous short keywords in question patterns
                }

                if (q.contains(keyword)) {
                    intent.getCountries().add(entry.getKey());
                    break; // Found one keyword for this country, no need to check others
                }
            }
        }

        // Extract year
        var matcher = YEAR_PATTERN.matcher(query);
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group());
                intent.setYearMin(year);
                intent.setYearMax(year);
            } catch (NumberFormatException ignored) {}
        }

        // Determine if it's a pure filter query
        // Pure filter = có genre hoặc country, KHÔNG hỏi thông tin chi tiết, KHÔNG hỏi khuyến mãi
        boolean hasFilter = !intent.getGenres().isEmpty() || !intent.getCountries().isEmpty() || intent.getYearMin() != null;
        intent.setPureFilter(hasFilter && !intent.isAsksInfo() && !intent.isWantsPromo());

        return intent;
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String vnNorm(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .trim();
    }
}

