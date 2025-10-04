package flim.backendcartoon.utils;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Dùng để clean subtitle text (cả SRT/VTT) trước khi trả về player.
 * - Loại nhạc nền, hiệu ứng âm thanh, applause... theo nhiều ngôn ngữ
 * - Gỡ [] rỗng, khoảng trắng thừa, nhiều dòng trống
 */
public final class SubtitleCleaner {

    // Các pattern mặc định (bạn có thể mở rộng)
    // Tip: (?i) = ignore case, (?m) = multi-line
    private static final Pattern[] REMOVE_PATTERNS = new Pattern[] {
            // [music], [âm nhạc], (music), {music}

            // [music], [âm nhạc], (music), {music}
                        Pattern.compile("(?im)^\\s*[\\[\\(\\{]\\s*(âm nhạc|music|nhạc nền)\\s*[\\]\\)\\}]\\s*$"),
            // [sound effects], [hiệu ứng âm thanh]
                        Pattern.compile("(?im)^\\s*[\\[\\(\\{]\\s*(sound\\s*effects?|hiệu ứng âm thanh)\\s*[\\]\\)\\}]\\s*$"),
            // [applause], [vỗ tay], [tiếng cười], [laughter]
                        Pattern.compile("(?im)^\\s*[\\[\\(\\{]\\s*(applause|vỗ tay|tiếng cười|laughter)\\s*[\\]\\)\\}]\\s*$"),
            // Cues descriptive kiểu <i>[...]</i> trong một dòng
                        Pattern.compile("(?im)^\\s*</?i>\\s*[\\[\\(\\{].*?[\\]\\)\\}]\\s*</?i>\\s*$"),

            // Dòng chỉ còn [] hoặc () hoặc {} rỗng
            Pattern.compile("(?m)^\\s*\\[\\s*\\]\\s*$"),
            Pattern.compile("(?m)^\\s*\\(\\s*\\)\\s*$"),
            Pattern.compile("(?m)^\\s*\\{\\s*\\}\\s*$"),
    };

    private SubtitleCleaner() {}

    /** Clean toàn bộ nội dung subtitle (UTF-8 String) */
    public static String cleanContent(String input) {
        if (input == null || input.isBlank()) return input;

        // Chuẩn hóa newline
        String s = input.replace("\r\n", "\n").replace('\r', '\n');

        // Loại các dòng mô tả âm thanh
        for (Pattern p : REMOVE_PATTERNS) {
            s = p.matcher(s).replaceAll("");
        }

        // Nếu còn các đoạn [ ... ] trong câu, cân nhắc xóa (chỉ khi cả dòng là mô tả)
        // Ở đây giữ nguyên trong câu thoại, chỉ loại dòng “thuần mô tả”

        // Gộp multi-blank-lines -> 1 blank line
        s = s.replaceAll("\\n{3,}", "\n\n");

        // Trim tổng thể + thêm newline cuối
        s = s.trim() + "\n";
        return s;
    }

    /** Preview những dòng sẽ bị xóa (đơn giản: trả về list dòng match) */
    public static List<String> previewRemovals(String input) {
        List<String> out = new ArrayList<>();
        if (input == null) return out;
        String[] lines = input.replace("\r\n","\n").replace('\r','\n').split("\n", -1);
        for (String line : lines) {
            for (Pattern p : REMOVE_PATTERNS) {
                if (p.matcher(line).matches()) {
                    out.add(line);
                    break;
                }
            }
        }
        return out;
    }

    /** Kiểm tra sau khi clean có “thực sự thay đổi” không (để UI báo hiệu) */
    public static boolean validateCleaning(String before, String after) {
        if (before == null && after == null) return true;
        if (before == null || after == null) return false;
        return !before.equals(after);
    }

    /** Strip UTF-8 BOM nếu có */
    public static byte[] stripBom(byte[] raw) {
        if (raw != null && raw.length >= 3 &&
                raw[0] == (byte)0xEF && raw[1] == (byte)0xBB && raw[2] == (byte)0xBF) {
            byte[] out = new byte[raw.length - 3];
            System.arraycopy(raw, 3, out, 0, out.length);
            return out;
        }
        return raw;
    }

    /** Helper: bytes -> UTF-8 string (strip BOM + normalize newline) */
    public static String toNormalizedUtf8(byte[] raw) {
        byte[] nb = stripBom(raw);
        String s = new String(nb, StandardCharsets.UTF_8);
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }
}
