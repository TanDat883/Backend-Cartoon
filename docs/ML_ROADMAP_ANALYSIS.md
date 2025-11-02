# 🤖 PHÂN TÍCH CHATBOT AI & LỘ TRÌNH MACHINE LEARNING

**CartoonToo Backend - Chatbot Intelligence Enhancement**  
**Date:** 2025-11-02  
**Current Score:** 9.3/10 → **Target Score:** 9.7-10.0/10

---

## 📊 ĐÁNH GIÁ HỆ THỐNG HIỆN TẠI

### ✅ **Điểm Mạnh Xuất Sắc** (9.3/10)

| Metric | Score | Evidence |
|--------|-------|----------|
| **Performance** | 10/10 | • Fast-path optimization (300ms)<br>• Off-topic detection pre-filter<br>• Pure filter queries bypass LLM |
| **Intelligence** | 9/10 | • Semantic understanding (IntentParser)<br>• Context-aware (current movie + mentioned movies)<br>• Genre normalization ("hoạt hình" → "anime") |
| **Accuracy** | 9.5/10 | • JSON Schema validation<br>• Structured response format<br>• Error handling with fallbacks |
| **Robustness** | 10/10 | • Timeout handling (12s)<br>• Off-topic fallback<br>• Multiple error recovery paths |
| **Cost Efficiency** | 10/10 | • Token optimization (5-message history)<br>• Conditional context injection<br>• Caching suggestions in memory |

### ❌ **Điểm Yếu Cần Cải Thiện** (0.7 điểm còn thiếu)

| Area | Current | Gap | Impact |
|------|---------|-----|--------|
| **Personalization** | Rule-based (wishlist + genre) | -0.3 | Không học hành vi cá nhân |
| **Conversational Personality** | Generic prompt | -0.2 | Thiếu giọng điệu nhất quán |
| **Proactive Suggestions** | Reactive only | -0.2 | Không chủ động gợi ý |

---

## 🏗️ KIẾN TRÚC HIỆN TẠI

### **Data Flow**
```
User Query → IntentParser → [Fast-path | LLM Path]
                                   ↓
                            AiService (OpenAI)
                                   ↓
                         ChatResponse + Memory
```

### **Tech Stack**
- **Backend:** Spring Boot + Java 17
- **Database:** DynamoDB (Movies, Users, Promotions)
- **Cache:** ChatMemoryService (in-memory, TTL=1h)
- **AI:** OpenAI GPT-4o-mini
- **Current Features:**
  - ✅ Intent detection (filter, promo, rec, info)
  - ✅ Genre-based recommendations (RecommendationService)
  - ✅ Context awareness (current movie + mentioned movies)
  - ✅ Memory (12 messages, 5 sent to LLM)
  - ✅ Fast-path for pure filters
  - ❌ **NO user behavior tracking**
  - ❌ **NO personalization beyond wishlist**
  - ❌ **NO proactive suggestions**

---

## 🎯 LỘ TRÌNH ML - 3 LỚP TĂNG DẦN

> **Khuyến nghị:** Bắt đầu **LỚP 1** ngay (2-3 tuần), đủ để đạt **9.7-9.8/10**

---

## 🥉 LỚP 1: "NO-TRAIN ML" (Dễ, Rủi Ro Thấp)

**Timeline:** 2-3 tuần  
**Target Score:** +0.5-0.7 điểm → **9.8-10.0/10**  
**Tech:** Embedding + Re-ranking + Persona Prompt

### 📦 **1.1 User Behavior Tracking (+0.3 Personalization)**

#### **A. Tạo Entity mới: `UserProfile`**
```java
@DynamoDbBean
public class UserProfile {
    private String userId;              // PK
    private List<Float> userVector;     // 384-dim embedding (avg của items clicked)
    private List<String> topGenres;     // Top 5 genres user xem nhiều nhất
    private String priceTier;           // "FREE" | "BASIC" | "PREMIUM"
    private List<String> lastIntents;   // 5 intent gần nhất: ["rec", "promo", "filter:action"]
    private Map<String, Integer> genreCount; // {"Action": 15, "Romance": 8}
    private Long lastUpdated;
    private Long ttl;                   // 90 days
}
```

#### **B. Tạo Service: `UserBehaviorService`**
```java
@Service
public class UserBehaviorService {
    
    // Capture user signals
    public void trackSignal(String userId, String eventType, 
                           String movieId, Map<String,Object> metadata) {
        // eventType: view_start, view_end, click_like, add_wishlist, search_query
        // Lưu vào DynamoDB table: UserSignals
        // Columns: userId, timestamp, eventType, movieId, metadata
    }
    
    // Update user vector (EMA)
    public void updateUserVector(String userId, List<Float> itemVector) {
        UserProfile profile = getUserProfile(userId);
        if (profile.getUserVector() == null) {
            profile.setUserVector(itemVector);
        } else {
            // EMA: u = 0.7*v + 0.3*u
            List<Float> updated = ema(profile.getUserVector(), itemVector, 0.7f);
            profile.setUserVector(updated);
        }
        save(profile);
    }
    
    private List<Float> ema(List<Float> u, List<Float> v, float alpha) {
        List<Float> result = new ArrayList<>();
        for (int i = 0; i < u.size(); i++) {
            result.add(alpha * v.get(i) + (1 - alpha) * u.get(i));
        }
        return result;
    }
}
```

#### **C. Tạo FE Endpoint: `/api/signals`**
```java
@PostMapping("/api/signals")
public ResponseEntity<Void> captureSignal(
    @AuthenticationPrincipal Jwt jwt,
    @RequestBody SignalRequest req) {
    
    String userId = jwt.getSubject();
    behaviorService.trackSignal(userId, req.getEventType(), 
                                req.getMovieId(), req.getMetadata());
    return ResponseEntity.ok().build();
}
```

**FE gửi signals:**
```javascript
// Khi user click vào phim
fetch('/api/signals', {
  method: 'POST',
  body: JSON.stringify({
    eventType: 'click_movie',
    movieId: 'movie123',
    metadata: { source: 'chatbot_suggestion' }
  })
});

// Khi user xem phim > 30s
fetch('/api/signals', {
  method: 'POST',
  body: JSON.stringify({
    eventType: 'view_engaged',
    movieId: 'movie123',
    metadata: { watchTime: 35 }
  })
});
```

---

### 📦 **1.2 Item Embedding Storage (+0.1 Personalization)**

#### **A. Tạo Entity: `ItemEmbedding`**
```java
@DynamoDbBean
public class ItemEmbedding {
    private String movieId;           // PK
    private List<Float> vector;       // 384-dim từ description + genres + tags
    private List<String> genres;
    private List<String> tags;
    private Integer releaseYear;
    private Long lastUpdated;
}
```

#### **B. Tạo Service: `EmbeddingService`**
```java
@Service
public class EmbeddingService {
    
    private final WebClient openAI;
    
    // Generate embedding cho phim mới/updated
    public List<Float> generateMovieEmbedding(Movie movie) {
        String text = movie.getTitle() + ". " + 
                     movie.getDescription() + ". " +
                     "Genres: " + String.join(", ", movie.getGenres());
        
        Map<String, Object> payload = Map.of(
            "model", "text-embedding-3-small", // 384-dim, cheap
            "input", text
        );
        
        JsonNode response = openAI.post()
            .uri("/embeddings")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        
        // Parse embedding từ response.data[0].embedding
        return parseEmbedding(response);
    }
    
    // Cosine similarity
    public double cosineSimilarity(List<Float> a, List<Float> b) {
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

#### **C. Scheduler: Generate embeddings nightly**
```java
@Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
public void refreshEmbeddings() {
    List<Movie> movies = movieService.findAllMovies();
    for (Movie m : movies) {
        if (needsEmbedding(m)) {
            List<Float> vec = embeddingService.generateMovieEmbedding(m);
            ItemEmbedding item = new ItemEmbedding();
            item.setMovieId(m.getMovieId());
            item.setVector(vec);
            item.setGenres(m.getGenres());
            embeddingRepo.save(item);
        }
    }
}
```

---

### 📦 **1.3 Smart Re-Ranking (+0.2 Personalization)**

#### **Update `RecommendationService`**
```java
public List<MovieSuggestionDTO> recommendForUser(String userId, 
                                                 String currentMovieId, 
                                                 int limit) {
    // 1. Get user profile
    UserProfile profile = behaviorService.getUserProfile(userId);
    List<Float> userVector = profile != null ? profile.getUserVector() : null;
    
    // 2. Get candidates (existing logic)
    List<Movie> candidates = getCandidates(userId, currentMovieId);
    
    // 3. RE-RANK with hybrid score
    List<ScoredMovie> scored = candidates.stream()
        .map(m -> {
            ItemEmbedding emb = embeddingRepo.findById(m.getMovieId());
            
            double cosineSim = (userVector != null && emb != null)
                ? embeddingService.cosineSimilarity(userVector, emb.getVector())
                : 0.5; // default neutral
            
            double ctr = calculateCTR(userId, m.getMovieId()); // từ UserSignals
            double freshness = calculateFreshness(m.getReleasedDate());
            
            // Hybrid score: 60% cosine + 30% CTR + 10% freshness
            double score = 0.6 * cosineSim + 0.3 * ctr + 0.1 * freshness;
            
            return new ScoredMovie(m, score);
        })
        .sorted(Comparator.comparingDouble(ScoredMovie::getScore).reversed())
        .limit(limit)
        .toList();
    
    // 4. Convert to DTO
    return scored.stream()
        .map(sm -> toDTO(sm.getMovie()))
        .toList();
}

private double calculateCTR(String userId, String movieId) {
    // Query UserSignals: clicks / impressions
    long clicks = signalRepo.countByUserAndMovieAndType(userId, movieId, "click");
    long impressions = signalRepo.countByUserAndMovieAndType(userId, movieId, "impression");
    return impressions > 0 ? (double) clicks / impressions : 0.0;
}
```

---

### 📦 **1.4 Conversational Persona (+0.2 Personality)**

#### **Update `AiService` System Prompt**
```java
public ChatResponse composeAnswer(...) {
    
    // Get user profile
    UserProfile profile = behaviorService.getUserProfile(userId);
    
    // Determine persona based on profile
    String persona = determinePersona(profile);
    String tone = determineTone(profile);
    
    String system = """
Bạn là trợ lý AI CartoonToo với phong cách %s.

PERSONA: %s
TOP GENRES: %s
PRICE TIER: %s

QUY TẮC:
1) Trả lời ngắn gọn (2-3 câu), thân thiện
2) Dùng "%s" khi xưng hô
3) Gợi ý tối đa 3 phim, giải thích lý do chọn trong 1 câu
4) Nếu user im lặng hoặc do dự, chủ động gợi ý 1 bước tiếp theo

MEMORY (3 facts gần nhất):
%s

Trả về JSON theo schema.
""".formatted(
    tone,                                      // "năng động" | "trầm lắng"
    persona,                                   // "Bạn thích anime shounen..."
    String.join(", ", profile.getTopGenres()),
    profile.getPriceTier(),
    safeUser,
    buildMemorySummary(profile)
);
    
    // ...rest of code
}

private String determinePersona(UserProfile profile) {
    if (profile == null) return "Người mới khám phá phim";
    
    List<String> genres = profile.getTopGenres();
    if (genres.contains("Action") && genres.contains("Shounen")) {
        return "Người yêu thích anime hành động, năng lượng cao";
    } else if (genres.contains("Romance") && genres.contains("Drama")) {
        return "Người thích phim tình cảm, cảm động";
    }
    return "Người xem phim đa dạng";
}

private String determineTone(UserProfile profile) {
    if (profile == null) return "thân thiện, hướng dẫn";
    
    List<String> intents = profile.getLastIntents();
    if (intents.stream().anyMatch(i -> i.contains("filter"))) {
        return "hiệu quả, đi thẳng vào vấn đề";
    }
    return "nhiệt tình, gợi mở";
}

private String buildMemorySummary(UserProfile profile) {
    // Tóm tắt 3 hành động gần nhất
    List<UserSignal> recent = signalRepo.findTopNByUserIdOrderByTimestampDesc(
        profile.getUserId(), 3
    );
    
    return recent.stream()
        .map(s -> String.format("- %s đã %s phim '%s'", 
            profile.getUserName(), s.getEventType(), s.getMovieTitle()))
        .collect(Collectors.joining("\n"));
}
```

---

### 📦 **1.5 Proactive Suggestions (+0.2 Proactive)**

#### **A. Tạo `ProactiveSuggestionService`**
```java
@Service
public class ProactiveSuggestionService {
    
    public Optional<ProactiveSuggestion> detectOpportunity(
        String userId, String currentContext) {
        
        UserProfile profile = behaviorService.getUserProfile(userId);
        List<UserSignal> recent = signalRepo.findRecent(userId, 7); // 7 days
        
        // Rule 1: Hỏi promo ≥2 lần trong 7 ngày → push voucher
        long promoQueries = recent.stream()
            .filter(s -> s.getEventType().equals("query") 
                      && s.getMetadata().containsKey("wantsPromo"))
            .count();
        
        if (promoQueries >= 2) {
            List<Promotion> activePromos = promotionService.getActivePromotions();
            if (!activePromos.isEmpty()) {
                return Optional.of(new ProactiveSuggestion(
                    "promo_push",
                    "Mình thấy bạn quan tâm đến khuyến mãi. " +
                    "Có mã giảm 30% cho gói Premium, dùng thử không?",
                    activePromos.get(0)
                ));
            }
        }
        
        // Rule 2: Dừng ở detail page > 30s → gợi ý trailer
        UserSignal lastSignal = recent.isEmpty() ? null : recent.get(0);
        if (lastSignal != null 
            && lastSignal.getEventType().equals("view_detail")
            && lastSignal.getDwellTime() > 30) {
            
            return Optional.of(new ProactiveSuggestion(
                "trailer_suggest",
                "Bạn đang xem thông tin phim này à? Muốn xem trailer không?",
                Map.of("movieId", lastSignal.getMovieId())
            ));
        }
        
        return Optional.empty();
    }
}
```

#### **B. Inject vào AiController**
```java
@PostMapping("/chat")
public ResponseEntity<ChatResponse> chat(...) {
    
    // ... existing code ...
    
    // ✅ Check proactive opportunity BEFORE calling LLM
    Optional<ProactiveSuggestion> proactive = 
        proactiveService.detectOpportunity(user.userId, rawQ);
    
    if (proactive.isPresent()) {
        log.info("🎯 Proactive suggestion triggered: {}", 
                 proactive.get().getType());
        
        // Inject into extras context
        extras.put("proactiveSuggestion", proactive.get());
    }
    
    // ... call AiService ...
}
```

---

## 📈 LỚP 1 - EXPECTED IMPACT

| Metric | Before | After | Gain |
|--------|--------|-------|------|
| **CTR@5** | 8% | 12-15% | +50-87% |
| **Watch Time** | 100% | 107-110% | +7-10% |
| **Personalization Score** | 5/10 | 8/10 | +0.3 |
| **Personality Score** | 5/10 | 7/10 | +0.2 |
| **Proactive Score** | 0/10 | 5/10 | +0.2 |
| **Overall Score** | 9.3/10 | **9.8-10.0/10** | **+0.5-0.7** |

---

## 🥈 LỚP 2: "LIGHT ML" (Vừa Phải) - OPTIONAL

**Timeline:** 4-6 tuần sau Lớp 1  
**Target:** +0.1-0.2 điểm → **10.0/10**  
**Tech:** XGBoost/LightGBM, Offline Training

### **2.1 Ranking Model (XGBoost)**
```python
# features.py
def extract_features(user_id, movie_id, context):
    return {
        'cosine_sim': cosine(user_vector, item_vector),
        'watch_time_norm': get_avg_watch_time(user_id, movie_id) / movie_duration,
        'genre_overlap': jaccard(user_genres, movie_genres),
        'recency_days': days_since_release(movie_id),
        'price_sensitivity': user_profile.price_tier == 'FREE',
        'hour_of_day': context.timestamp.hour,
        'day_of_week': context.timestamp.weekday(),
        'ctr_personal': get_ctr(user_id, movie_id),
        'ctr_global': get_global_ctr(movie_id),
    }

# train.py
import xgboost as xgb

X_train, y_train = load_training_data()  # y = clicked (0/1)
model = xgb.XGBClassifier(max_depth=5, n_estimators=100)
model.fit(X_train, y_train)
model.save_model('ranking_model.json')
```

**Serve via HTTP:**
```java
@Service
public class MLRankingService {
    
    private final RestTemplate restTemplate;
    
    public double predictClickProbability(String userId, String movieId) {
        Map<String, Object> features = extractFeatures(userId, movieId);
        
        ResponseEntity<Double> response = restTemplate.postForEntity(
            "http://ml-service:8080/predict", 
            features, 
            Double.class
        );
        
        return response.getBody();
    }
}
```

### **2.2 Intent Next-Step Classifier**
Dự đoán: "user muốn làm gì tiếp theo?"
- `watch_trailer` (0.7) → gợi ý xem trailer
- `buy_subscription` (0.5) → push voucher
- `ask_more_info` (0.3) → đợi câu hỏi

---

## 🥇 LỚP 3: "DEEP ML" (Khó) - FUTURE

**Timeline:** 3-6 tháng  
**Tech:** Transformer, GRU4Rec, Contextual Bandits

### **3.1 Session-based Recommender (GRU4Rec)**
Học chuỗi hành vi: `[view(A) → click(B) → watch(C)]` → predict next: `D`

### **3.2 Contextual Bandits (LinUCB)**
Tự động học trọng số `w1, w2, w3` cho từng user.

### **3.3 LoRA Finetuning for Personality**
Finetune GPT-4o-mini trên 2000-5000 mẫu hội thoại tốt.

---

## 🚀 IMPLEMENTATION PLAN (LỚP 1)

### **Week 1-2: Data Infrastructure**
- [ ] Tạo `UserProfile` entity + repository
- [ ] Tạo `ItemEmbedding` entity + repository
- [ ] Tạo `UserSignal` entity + repository
- [ ] Implement `UserBehaviorService`
- [ ] Implement `EmbeddingService`
- [ ] Tạo `/api/signals` endpoint
- [ ] Update FE để gửi signals

### **Week 2-3: Smart Recommendation**
- [ ] Generate embeddings cho toàn bộ phim
- [ ] Implement re-ranking logic trong `RecommendationService`
- [ ] Tạo scheduler refresh embeddings nightly
- [ ] Test hybrid scoring (cosine + CTR + freshness)

### **Week 3: Persona & Proactive**
- [ ] Implement persona detection
- [ ] Update `AiService` system prompt
- [ ] Implement `ProactiveSuggestionService`
- [ ] Integrate proactive triggers vào chat flow

### **Week 4: Testing & Monitoring**
- [ ] A/B test: 90% new logic, 10% old
- [ ] Monitor metrics: CTR@5, watch time, conversion
- [ ] Log embedding quality (cosine sim distribution)
- [ ] Dashboard: personalization effectiveness

---

## 📊 SUCCESS METRICS

### **Primary KPIs**
| Metric | Baseline | Target (Lớp 1) | Measurement |
|--------|----------|----------------|-------------|
| CTR@5 | 8% | 12%+ | `clicks / impressions` in top 5 |
| Watch Time | 100% | 107%+ | Avg watch time per session |
| Conversion Rate | 2% | 2.5%+ | Free → Premium upgrades |
| Chat Engagement | 3 msg/session | 4 msg/session | Avg messages per conversation |

### **Secondary KPIs**
- **Personalization Score:** Cosine sim between user vector & clicked items > 0.6
- **Proactive Hit Rate:** % of proactive suggestions that get clicked (target: >15%)
- **Persona Consistency:** User satisfaction survey (target: 8/10)

---

## 💡 BEST PRACTICES

### **1. Cold Start Problem**
```java
// User mới → dùng popular items + genre từ onboarding
if (profile == null || profile.getUserVector() == null) {
    return getPopularMoviesForNewUser(onboardingGenres);
}
```

### **2. Privacy & Consent**
```java
@DynamoDbAttribute("personalizationEnabled")
private Boolean personalizationEnabled; // Default: true

// UI: "Cho phép sử dụng lịch sử xem để cá nhân hóa gợi ý"
```

### **3. Embedding Versioning**
```java
@DynamoDbAttribute("embeddingVersion")
private String embeddingVersion; // "v1" | "v2"

// Khi đổi model embedding → migrate data
```

### **4. A/B Testing Framework**
```java
@Service
public class ABTestService {
    public boolean isInExperimentGroup(String userId, String experimentId) {
        int hash = (userId + experimentId).hashCode();
        return Math.abs(hash) % 100 < 90; // 90% in experiment
    }
}
```

---

## 🎯 SUMMARY

### **Khuyến Nghị Ngắn Gọn:**

1. **Bắt đầu LỚP 1 ngay** (2-3 tuần)
   - Đủ để đạt **9.8-10.0/10** với rủi ro thấp
   - Chi phí thấp (chỉ cần OpenAI embeddings API)
   - Không cần hạ tầng ML riêng

2. **LỚP 2 sau 3-6 tháng** nếu cần optimize thêm
   - XGBoost ranking model
   - Offline training pipeline

3. **LỚP 3 là FUTURE** (6-12 tháng)
   - Deep learning models
   - Chỉ cần khi scale lớn (>100K users)

### **ROI Expected:**
- **Lớp 1:** +50-87% CTR, +7-10% watch time, +0.7 điểm
- **Cost:** $50-100/month (OpenAI embeddings)
- **Dev Time:** 2-3 tuần (1 dev)

---

## 📚 REFERENCES

- [OpenAI Embeddings API](https://platform.openai.com/docs/guides/embeddings)
- [EMA (Exponential Moving Average)](https://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average)
- [Hybrid Recommender Systems](https://arxiv.org/abs/2106.08725)
- [GRU4Rec Paper](https://arxiv.org/abs/1511.06939)
- [LinUCB Contextual Bandits](https://arxiv.org/abs/1003.0146)

---

**Next Steps:**
1. Review & approve lộ trình
2. Tạo Jira tickets cho Week 1-2
3. Setup monitoring dashboard (Grafana/CloudWatch)
4. Kick-off meeting 🚀

