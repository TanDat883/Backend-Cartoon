package flim.backendcartoon.services;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Semantic Genre Mapper - Hiểu quan hệ giữa các thể loại phim
 * Fix vấn đề: "hoạt hình" vs "anime" vs "thiếu nhi" - đều liên quan nhưng chatbot không hiểu
 */
@Component
public class GenreSemantics {

    /**
     * Genre clusters - Nhóm các genres có semantic similarity
     * Khi user hỏi 1 genre, tự động search cả cluster
     */
    private static final Map<String, Set<String>> GENRE_CLUSTERS = Map.of(
            // Cluster 1: Animation for all ages
            "animation", Set.of("Hoạt Hình", "Anime", "Animation", "Cartoon", "Disney"),

            // Cluster 2: Kids & Family content
            "kids", Set.of("Thiếu Nhi", "Gia Đình", "Family", "Kids", "Children", "Trẻ Em"),

            // Cluster 3: Animation + Kids (overlap cluster)
            "kids_animation", Set.of("Hoạt Hình", "Anime", "Thiếu Nhi", "Gia Đình", "Animation", "Cartoon", "Disney"),

            // Cluster 4: Action genres
            "action", Set.of("Hành Động", "Action", "Võ Thuật", "Martial Arts", "Chiến Tranh", "War"),

            // Cluster 5: Drama & Psychology
            "drama", Set.of("Tâm Lý", "Drama", "Chính Kịch", "Psychological"),

            // Cluster 6: Horror & Thriller
            "horror", Set.of("Kinh Dị", "Horror", "Bí Ẩn", "Mystery", "Thriller"),

            // Cluster 7: Romance & Emotion
            "romance", Set.of("Tình Cảm", "Romance", "Lãng Mạn", "Love"),

            // Cluster 8: Sci-fi & Fantasy
            "scifi", Set.of("Khoa Học", "Sci-Fi", "Viễn Tưởng", "Fantasy", "Giả Tưởng")
    );

    /**
     * Genre synonyms - Các tên gọi khác nhau của cùng 1 thể loại
     */
    private static final Map<String, Set<String>> GENRE_SYNONYMS = Map.of(
            "Hoạt Hình", Set.of("Anime", "Animation", "Cartoon", "hoạt hình", "hoat hinh", "anime"),
            "Anime", Set.of("Hoạt Hình", "Animation", "anime", "hoat hinh"),
            "Thiếu Nhi", Set.of("Trẻ Em", "Kids", "Children", "Gia Đình", "Family", "thieu nhi", "tre em"),
            "Gia Đình", Set.of("Family", "Thiếu Nhi", "Kids", "gia dinh", "family"),
            "Hành Động", Set.of("Action", "hanh dong", "action"),
            "Tâm Lý", Set.of("Drama", "Chính Kịch", "tam ly", "drama"),
            "Kinh Dị", Set.of("Horror", "kinh di", "horror", "scary"),
            "Tình Cảm", Set.of("Romance", "Lãng Mạn", "tinh cam", "romance")
    );

    /**
     * Hierarchical relationships - Quan hệ cha-con giữa genres
     * Parent → Children (broad → specific)
     */
    private static final Map<String, Set<String>> GENRE_HIERARCHY = Map.of(
            // Animation (parent) contains specific types
            "animation_parent", Set.of("Hoạt Hình", "Anime", "Animation", "Cartoon", "Disney"),

            // Kids content (parent) includes animation + family
            "kids_parent", Set.of("Thiếu Nhi", "Gia Đình", "Hoạt Hình", "Anime", "Animation"),

            // Action (parent) includes martial arts, war
            "action_parent", Set.of("Hành Động", "Võ Thuật", "Chiến Tranh"),

            // Drama (parent) includes psychological, romance
            "drama_parent", Set.of("Tâm Lý", "Chính Kịch", "Tình Cảm")
    );

    /**
     * Get related genres for semantic search
     * When user searches for "hoạt hình", also search "anime", "thiếu nhi", etc.
     */
    public Set<String> getRelatedGenres(String genre) {
        Set<String> related = new HashSet<>();
        related.add(genre); // Always include original

        String genreNorm = normalizeGenre(genre);

        // 1. Add direct synonyms
        GENRE_SYNONYMS.forEach((key, values) -> {
            if (normalizeGenre(key).equals(genreNorm) ||
                values.stream().anyMatch(v -> normalizeGenre(v).equals(genreNorm))) {
                related.add(key);
                related.addAll(values);
            }
        });

        // 2. Add cluster members
        GENRE_CLUSTERS.forEach((clusterName, members) -> {
            if (members.stream().anyMatch(m -> normalizeGenre(m).equals(genreNorm))) {
                related.addAll(members);
            }
        });

        return related;
    }

    /**
     * Get cluster for a genre (for smarter recommendations)
     */
    public String getGenreCluster(String genre) {
        String genreNorm = normalizeGenre(genre);

        for (Map.Entry<String, Set<String>> entry : GENRE_CLUSTERS.entrySet()) {
            if (entry.getValue().stream().anyMatch(g -> normalizeGenre(g).equals(genreNorm))) {
                return entry.getKey();
            }
        }

        return "general";
    }

    /**
     * Check if two genres are semantically similar
     */
    public boolean areSimilar(String genre1, String genre2) {
        String g1 = normalizeGenre(genre1);
        String g2 = normalizeGenre(genre2);

        if (g1.equals(g2)) return true;

        // Check if they're in the same cluster
        for (Set<String> cluster : GENRE_CLUSTERS.values()) {
            boolean has1 = cluster.stream().anyMatch(g -> normalizeGenre(g).equals(g1));
            boolean has2 = cluster.stream().anyMatch(g -> normalizeGenre(g).equals(g2));
            if (has1 && has2) return true;
        }

        // Check synonyms
        for (Map.Entry<String, Set<String>> entry : GENRE_SYNONYMS.entrySet()) {
            boolean has1 = normalizeGenre(entry.getKey()).equals(g1) ||
                          entry.getValue().stream().anyMatch(v -> normalizeGenre(v).equals(g1));
            boolean has2 = normalizeGenre(entry.getKey()).equals(g2) ||
                          entry.getValue().stream().anyMatch(v -> normalizeGenre(v).equals(g2));
            if (has1 && has2) return true;
        }

        return false;
    }

    /**
     * Get smart fallback genres if no exact match found
     * Example: "hoạt hình" not found → suggest "anime", "thiếu nhi"
     */
    public List<String> getSuggestedFallbackGenres(String genre) {
        Set<String> related = getRelatedGenres(genre);
        List<String> fallbacks = new ArrayList<>(related);
        fallbacks.remove(genre); // Remove original
        return fallbacks;
    }

    /**
     * Check if a movie matches the genre semantically (not just exact match)
     */
    public boolean movieMatchesGenreSemantically(Set<String> movieGenres, String searchGenre) {
        if (movieGenres == null || movieGenres.isEmpty()) return false;

        // Get all semantically related genres
        Set<String> relatedGenres = getRelatedGenres(searchGenre);

        // Check if movie has any of the related genres
        for (String movieGenre : movieGenres) {
            for (String related : relatedGenres) {
                if (normalizeGenre(movieGenre).contains(normalizeGenre(related)) ||
                    normalizeGenre(related).contains(normalizeGenre(movieGenre))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Normalize genre name for comparison
     */
    private String normalizeGenre(String genre) {
        if (genre == null) return "";
        return genre.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    /**
     * Get display name with explanation for fallback
     */
    public String getGenreExplanation(String originalGenre, String fallbackGenre) {
        if (areSimilar(originalGenre, fallbackGenre)) {
            return String.format("%s (liên quan đến %s)", fallbackGenre, originalGenre);
        }
        return fallbackGenre;
    }
}

