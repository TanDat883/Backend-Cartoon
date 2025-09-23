package flim.backendcartoon.utils;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.regex.Pattern;

public final class MovieValidationUtils {
    private MovieValidationUtils() {}

    // Chỉ chữ (Unicode), số, khoảng trắng. Nếu cần cho phép thêm ký tự, xem ALLOWED_TITLE_CHARS ở dưới.
    public static final Pattern TITLE_PATTERN =
            Pattern.compile("^[\\p{L}\\p{N}\\s]{1,200}$");

    // Ví dụ cho phép thêm `- : , . ! ?` vào tiêu đề:
    // public static final Pattern ALLOWED_TITLE_CHARS =
    //         Pattern.compile("^[\\p{L}\\p{N}\\s\\-:,.!?]{1,200}$");

    public static final Pattern DURATION_PATTERN =
            Pattern.compile("^\\d{1,4}(?:\\s*(?:p|phút|phut)(?:\\s*/\\s*tập)?)?\\s*$", Pattern.CASE_INSENSITIVE);

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(msg);
    }

    public static boolean validTitle(String title) {
        return title != null && TITLE_PATTERN.matcher(title.trim()).matches();
        // Hoặc nếu dùng ALLOWED_TITLE_CHARS:
        // return title != null && ALLOWED_TITLE_CHARS.matcher(title.trim()).matches();
    }

    public static boolean validDuration(String duration) {
        return duration == null || duration.isBlank()
                || DURATION_PATTERN.matcher(duration.trim()).matches();
    }

    public static boolean arrayHasBlank(List<String> arr) {
        return arr != null && arr.stream().anyMatch(MovieValidationUtils::isBlank);
    }

    public static boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    public static boolean isVideo(String contentType) {
        return contentType != null && contentType.startsWith("video/");
    }
}