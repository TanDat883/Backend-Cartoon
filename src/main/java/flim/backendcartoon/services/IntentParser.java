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
        private boolean isTitleSearch; // ✅ true = user đang tìm phim theo tên cụ thể
        private String searchTitle; // ✅ Extracted movie title from query
    }

    // Map genre keywords - CANONICAL NAMES match với database
    // ✅ FIX: Add "Anime" as separate genre since database uses "Anime" not "Hoạt Hình"
    private static final Map<String, Set<String>> GENRE_KEYWORDS = Map.ofEntries(
            Map.entry("Hành Động", Set.of("hanh dong", "action", "fight")),
            Map.entry("Hài", Set.of("hai", "comedy", "hai huoc", "sitcom")),
            Map.entry("Tình Cảm", Set.of("tinh cam", "romance", "lang man", "love")),
            Map.entry("Kinh Dị", Set.of("kinh di", "horror", "ma", "scary", "ghost")),
            Map.entry("Hoạt Hình", Set.of("hoat hinh", "cartoon", "animation", "애니메이션")),
            Map.entry("Anime", Set.of("anime")),  // ✅ Separate entry for Anime
            Map.entry("Phiêu Lưu", Set.of("phieu luu", "adventure", "quest")),
            Map.entry("Tâm Lý", Set.of("tam ly", "drama", "chinh kich", "psychological")),
            Map.entry("Gia Đình", Set.of("gia dinh", "family", "tre em", "kids")),
            Map.entry("Thiếu Nhi", Set.of("thieu nhi", "tre em nho")),  // ✅ Add Thiếu Nhi
            Map.entry("Viễn Tưởng", Set.of("vien tuong", "fantasy", "than thoai", "magic")),
            Map.entry("Khoa Học", Set.of("khoa hoc", "sci-fi", "science fiction", "vien tuong khoa hoc")),
            Map.entry("Chiến Tranh", Set.of("chien tranh", "war", "military", "army", "soldier")),
            Map.entry("Võ Thuật", Set.of("vo thuat", "martial arts", "kung fu", "wushu")),
            Map.entry("Bí Ẩn", Set.of("bi an", "mystery", "trinh tham", "detective")),
            Map.entry("Hình Sự", Set.of("hinh su", "crime", "police", "criminal")),
            Map.entry("Thể Thao", Set.of("the thao", "sports", "sport"))
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

    // ✅ Negative context patterns - to avoid extracting keywords from negative sentences
    private static final Set<String> NEGATIVE_PATTERNS = Set.of(
            "khong phai", "ko phai", "chu ko", "chu khong",
            "khong muon", "ko muon", "khong thich", "ko thich",
            "dung", "dung la", "stop"
    );

    // ✅ Chatbot conversation keywords - questions about the chatbot itself, NOT movie searches
    private static final Set<String> CHATBOT_CONVERSATION_KEYWORDS = Set.of(
            "ban la", "ban co phai", "ban biet", "ban ten gi", "ban may tuoi",
            "gioi tinh", "con trai", "con gai", "con nguoi", "ai tao ra ban",
            "ban song o dau", "ban lam gi", "ban thich gi", "tam su",
            "cho toi tien", "giup toi", "alo alo", "hello", "hi bot"
    );

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

    // ✅ Title search patterns: "tôi muốn xem phim X", "phim X có không", "tìm phim X"
    private static final Set<String> TITLE_SEARCH_TRIGGERS = Set.of(
            "muon xem phim", "tim phim", "phim", "xem phim", "co phim",
            "phim nao ten", "ten phim", "phim ten la"
    );

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

        // ✅ Detect title search: "tôi muốn xem phim đảo ấu trùng 2018"
        // Extract potential movie title by removing common keywords
        String titleCandidate = extractTitleCandidate(query, q);

        // ✅ Check if query has negative context (e.g., "không phải Trung Quốc")
        boolean hasNegativeContext = containsAny(q, NEGATIVE_PATTERNS);

        // Extract genres (skip if negative context)
        if (!hasNegativeContext) {
            for (Map.Entry<String, Set<String>> entry : GENRE_KEYWORDS.entrySet()) {
                if (containsAny(q, entry.getValue())) {
                    intent.getGenres().add(entry.getKey());
                }
            }
        }

        // Extract countries (with context-aware filtering, skip if negative context)
        if (!hasNegativeContext) {
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

        // ✅ Check if this is a title search (has movie title candidate but NO genre/country keywords)
        // This distinguishes "phim đảo ấu trùng" (title search) from "phim hành động" (genre filter)
        if (titleCandidate != null && !titleCandidate.isBlank() &&
            intent.getGenres().isEmpty() && intent.getCountries().isEmpty()) {
            intent.setTitleSearch(true);
            intent.setSearchTitle(titleCandidate);
        }

        // Determine if it's a pure filter query
        // Pure filter = có genre hoặc country, KHÔNG hỏi thông tin chi tiết, KHÔNG hỏi khuyến mãi, KHÔNG phải title search
        boolean hasFilter = !intent.getGenres().isEmpty() || !intent.getCountries().isEmpty() || intent.getYearMin() != null;
        intent.setPureFilter(hasFilter && !intent.isAsksInfo() && !intent.isWantsPromo() && !intent.isTitleSearch());

        // 🐛 DEBUG: Log intent parsing để track hallucination bugs
        if (hasFilter || intent.isTitleSearch()) {
            System.out.println("🔍 [IntentParser] Query: " + query);
            System.out.println("   └─ Genres: " + intent.getGenres());
            System.out.println("   └─ Countries: " + intent.getCountries());
            System.out.println("   └─ Year: " + intent.getYearMin() + (intent.getYearMax() != null && !intent.getYearMin().equals(intent.getYearMax()) ? "-" + intent.getYearMax() : ""));
            System.out.println("   └─ isPureFilter: " + intent.isPureFilter());
            System.out.println("   └─ isTitleSearch: " + intent.isTitleSearch());
            if (intent.isTitleSearch()) {
                System.out.println("   └─ searchTitle: " + intent.getSearchTitle());
            }
        }

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

    /**
     * ✅ Extract potential movie title from query
     * Example: "tôi muốn xem phim đảo ấu trùng 2018" → "đảo ấu trùng"
     */
    private String extractTitleCandidate(String originalQuery, String normalized) {
        // ✅ CRITICAL: Skip if this is a chatbot conversation, NOT a movie search
        if (containsAny(normalized, CHATBOT_CONVERSATION_KEYWORDS)) {
            return null;  // This is about the chatbot, not a movie title
        }

        // Remove common search triggers
        String cleaned = normalized;
        cleaned = cleaned.replaceAll("\\btoi muon xem phim\\b", "");
        cleaned = cleaned.replaceAll("\\bmuon xem phim\\b", "");
        cleaned = cleaned.replaceAll("\\bmuon xem\\b", "");  // ✅ Add "muốn xem" without "phim"
        cleaned = cleaned.replaceAll("\\btoi muon\\b", "");  // ✅ Add "tôi muốn"
        cleaned = cleaned.replaceAll("\\btim phim\\b", "");
        cleaned = cleaned.replaceAll("\\bxem phim\\b", "");
        cleaned = cleaned.replaceAll("\\bco phim\\b", "");
        cleaned = cleaned.replaceAll("\\bphim\\b", "");
        cleaned = cleaned.replaceAll("\\btoi\\b", "");  // ✅ Remove "tôi"
        cleaned = cleaned.replaceAll("\\bxem\\b", "");  // ✅ Remove "xem"
        cleaned = cleaned.replaceAll("\\bkhong\\b", "");
        cleaned = cleaned.replaceAll("\\bko\\b", "");

        // Remove year patterns
        cleaned = cleaned.replaceAll("\\b(19|20)\\d{2}\\b", "");

        // Remove question words
        cleaned = cleaned.replaceAll("\\bco\\b", "");
        cleaned = cleaned.replaceAll("\\bkhong\\b", "");

        cleaned = cleaned.trim();

        // ✅ FIX: If cleaned text is a genre/country keyword, DON'T treat as title
        // Check if it matches any genre keyword
        for (Map.Entry<String, Set<String>> entry : GENRE_KEYWORDS.entrySet()) {
            if (entry.getValue().contains(cleaned)) {
                return null;  // It's a genre, not a title
            }
        }

        // Check if it matches any country keyword
        for (Map.Entry<String, Set<String>> entry : COUNTRY_KEYWORDS.entrySet()) {
            if (entry.getValue().contains(cleaned)) {
                return null;  // It's a country, not a title
            }
        }

        // If we have something left with at least 3 characters, consider it a title candidate
        if (cleaned.length() >= 3) {
            return cleaned;
        }

        return null;
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

