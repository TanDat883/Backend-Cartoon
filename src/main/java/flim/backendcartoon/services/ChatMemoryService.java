package flim.backendcartoon.services;

import flim.backendcartoon.entities.DTO.response.MovieSuggestionDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemoryService {

    @Data @AllArgsConstructor
    public static class ChatMsg {
        private String role;    // "user" | "assistant"
        private String content;
        private long ts;
    }

    private static final int MAX_PER_CONV = 40;     // giữ tối đa 40 message/phiên
    private static final long TTL_MILLIS = 1 * 60 * 60 * 1000L; // 2 giờ

    private final Map<String, Deque<ChatMsg>> store = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();
    private final Map<String, List<MovieSuggestionDTO>> lastSuggestions = new ConcurrentHashMap<>();

    public void setSuggestions(String convId, List<MovieSuggestionDTO> sugs) {
        if (convId == null || convId.isBlank()) return;
        lastSuggestions.put(convId, sugs == null ? List.of() : new ArrayList<>(sugs));
        lastAccess.put(convId, System.currentTimeMillis());
    }

    public List<MovieSuggestionDTO> getSuggestions(String convId) {
        return lastSuggestions.getOrDefault(convId, List.of());
    }

    public void reset(String convId) {
        if (convId == null || convId.isBlank()) return;
        store.remove(convId);
        lastAccess.remove(convId);
        lastSuggestions.remove(convId);
    }

    public void append(String convId, String role, String content) {
        if (convId == null || convId.isBlank()) return;
        cleanup(convId);

        store.computeIfAbsent(convId, k -> new ArrayDeque<>());
        var q = store.get(convId);
        q.addLast(new ChatMsg(role, content, Instant.now().toEpochMilli()));

        while (q.size() > MAX_PER_CONV) q.pollFirst();
        lastAccess.put(convId, System.currentTimeMillis());
    }

    /** Lấy L message cuối (theo đúng thứ tự cũ -> mới) */
    public List<ChatMsg> history(String convId, int lastN) {
        if (convId == null || convId.isBlank()) return List.of();
        cleanup(convId);

        var q = store.get(convId);
        if (q == null || q.isEmpty()) return List.of();

        int n = Math.max(0, q.size() - lastN);
        var it = q.iterator();
        List<ChatMsg> out = new ArrayList<>();
        int i = 0;
        while (it.hasNext()) {
            var m = it.next();
            if (i++ >= n) out.add(m);
        }
        lastAccess.put(convId, System.currentTimeMillis());
        return out;
    }

    private void cleanup(String convId) {
        // dọn TTL riêng từng conv
        Long last = lastAccess.get(convId);
        if (last != null && System.currentTimeMillis() - last > TTL_MILLIS) {
            store.remove(convId);
            lastAccess.remove(convId);
        }
    }
}
