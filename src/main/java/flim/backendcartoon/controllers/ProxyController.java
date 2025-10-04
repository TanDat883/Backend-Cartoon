package flim.backendcartoon.controllers;

import flim.backendcartoon.utils.SubtitleCleaner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    @GetMapping("/subtitle")
    public ResponseEntity<byte[]> proxySubtitle(
            @RequestParam String url,
            @RequestParam(name = "clean", required = false, defaultValue = "false") boolean clean) throws IOException {
        var u = java.net.URI.create(url).toURL();
        byte[] raw;
        try (var in = u.openStream()) { raw = in.readAllBytes(); }

        String lower = url.toLowerCase();
        String content = SubtitleCleaner.toNormalizedUtf8(raw);

        String vttText;
        if (lower.endsWith(".srt")) {
            // SRT -> VTT
            // 1) đổi comma sang dot ở timestamp
            content = content.replaceAll("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})", "$1.$2");
            // 2) bỏ dòng số thứ tự cue
            content = content.replaceAll("(?m)^\\d+\\s*$", "");
            // 3) gộp newline
            content = content.replaceAll("\\n{3,}", "\n\n");
            // 4) thêm header WEBVTT
            vttText = "WEBVTT\n\n" + content.trim() + "\n";
        } else {
            // VTT (hoặc format khác nhưng coi như VTT)
            if (!content.startsWith("WEBVTT")) {
                vttText = "WEBVTT\n\n" + content.trim() + "\n";
            } else {
                vttText = content;
            }
        }

        if (clean) {
            // chỉ clean phần thoại, không động chạm timestamp
            vttText = SubtitleCleaner.cleanContent(vttText);
        }

        byte[] out = vttText.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Type", "text/vtt; charset=utf-8")
                .header("Cross-Origin-Resource-Policy", "cross-origin") // tránh CORP
                .header("Cache-Control", "public, max-age=300")
                .body(out);
    }
}
