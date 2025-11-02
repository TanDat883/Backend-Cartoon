# 🚀 LỚP 1 IMPLEMENTATION CHECKLIST

**Target:** Cải thiện ChatBot từ 9.3/10 → 9.8-10.0/10  
**Timeline:** 2-3 tuần  
**Effort:** 1 developer, part-time

---

## ✅ WEEK 1: Data Infrastructure

### 📦 **Phase 1.1: Create Entities** (Day 1-2)

- [x] ✅ **UserProfile.java** - CREATED
  - Lưu user vector, genres preferences, intents
  - Fields: userId, userVector (384-dim), topGenres, priceTier, lastIntents
  
- [x] ✅ **ItemEmbedding.java** - CREATED
  - Lưu movie embeddings cho semantic search
  - Fields: movieId, vector (384-dim), genres, releaseYear
  
- [x] ✅ **UserSignal.java** - CREATED
  - Track user behavior (clicks, views, searches)
  - Fields: userId, timestamp, eventType, movieId, metadata

### 📦 **Phase 1.2: Create Repositories** (Day 2-3)

- [ ] **UserProfileRepository.java**
```java
@Repository
public class UserProfileRepository {
    private final DynamoDbTable<UserProfile> table;
    
    public UserProfileRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("UserProfile", 
                                   TableSchema.fromBean(UserProfile.class));
    }
    
    public UserProfile findById(String userId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(userId)));
    }
    
    public void save(UserProfile profile) {
        table.putItem(profile);
    }
}
```

- [ ] **ItemEmbeddingRepository.java**
```java
@Repository
public class ItemEmbeddingRepository {
    private final DynamoDbTable<ItemEmbedding> table;
    
    public ItemEmbeddingRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("ItemEmbedding", 
                                   TableSchema.fromBean(ItemEmbedding.class));
    }
    
    public ItemEmbedding findById(String movieId) {
        return table.getItem(r -> r.key(k -> k.partitionValue(movieId)));
    }
    
    public void save(ItemEmbedding embedding) {
        table.putItem(embedding);
    }
    
    public List<ItemEmbedding> findAll() {
        return table.scan().items().stream().toList();
    }
}
```

- [ ] **UserSignalRepository.java**
```java
@Repository
public class UserSignalRepository {
    private final DynamoDbTable<UserSignal> table;
    
    public UserSignalRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("UserSignal", 
                                   TableSchema.fromBean(UserSignal.class));
    }
    
    public void save(UserSignal signal) {
        table.putItem(signal);
    }
    
    // Query last N signals for a user
    public List<UserSignal> findRecent(String userId, int days) {
        long cutoff = Instant.now().minusSeconds(days * 86400L).toEpochMilli();
        
        QueryConditional condition = QueryConditional
            .sortGreaterThanOrEqualTo(k -> k.partitionValue(userId)
                                             .sortValue(cutoff));
        
        return table.query(r -> r.queryConditional(condition))
                   .items()
                   .stream()
                   .toList();
    }
    
    // Count signals by type
    public long countByUserAndMovieAndType(String userId, String movieId, String eventType) {
        return findRecent(userId, 30).stream()
            .filter(s -> s.getMovieId() != null && s.getMovieId().equals(movieId))
            .filter(s -> s.getEventType().equals(eventType))
            .count();
    }
}
```

### 📦 **Phase 1.3: Create DynamoDB Tables** (Day 3)

- [ ] **AWS Console hoặc CDK/Terraform:**

```bash
# UserProfile Table
aws dynamodb create-table \
  --table-name UserProfile \
  --attribute-definitions AttributeName=userId,AttributeType=S \
  --key-schema AttributeName=userId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --tags Key=Environment,Value=Production

# ItemEmbedding Table
aws dynamodb create-table \
  --table-name ItemEmbedding \
  --attribute-definitions AttributeName=movieId,AttributeType=S \
  --key-schema AttributeName=movieId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# UserSignal Table (with TTL)
aws dynamodb create-table \
  --table-name UserSignal \
  --attribute-definitions \
    AttributeName=userId,AttributeType=S \
    AttributeName=timestamp,AttributeType=N \
  --key-schema \
    AttributeName=userId,KeyType=HASH \
    AttributeName=timestamp,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST

# Enable TTL on UserSignal
aws dynamodb update-time-to-live \
  --table-name UserSignal \
  --time-to-live-specification Enabled=true,AttributeName=ttl
```

### 📦 **Phase 1.4: Implement Services** (Day 4-5)

- [x] ✅ **EmbeddingService.java** - CREATED
  - generateMovieEmbedding()
  - cosineSimilarity()
  - ema() for user vector update

- [ ] **UserBehaviorService.java**
```java
@Service
@RequiredArgsConstructor
public class UserBehaviorService {
    
    private final UserProfileRepository profileRepo;
    private final UserSignalRepository signalRepo;
    private final ItemEmbeddingRepository embeddingRepo;
    private final EmbeddingService embeddingService;
    private final MovieService movieService;
    
    /**
     * Track user signal and update profile
     */
    public void trackSignal(String userId, String eventType, 
                           String movieId, Map<String, Object> metadata) {
        // 1. Save signal
        UserSignal signal = new UserSignal();
        signal.setUserId(userId);
        signal.setTimestamp(Instant.now().toEpochMilli());
        signal.setEventType(eventType);
        signal.setMovieId(movieId);
        signal.setMetadata(metadata != null ? metadata : new HashMap<>());
        signal.setTtl(Instant.now().plusSeconds(90L * 86400).getEpochSecond());
        
        if (movieId != null) {
            Movie movie = movieService.findMovieById(movieId);
            signal.setMovieTitle(movie != null ? movie.getTitle() : null);
        }
        
        signalRepo.save(signal);
        
        // 2. Update user profile (async recommended)
        updateUserProfile(userId, movieId, eventType);
    }
    
    /**
     * Update user profile based on signal
     */
    private void updateUserProfile(String userId, String movieId, String eventType) {
        UserProfile profile = profileRepo.findById(userId);
        if (profile == null) {
            profile = initializeProfile(userId);
        }
        
        // Update interaction count
        profile.setTotalInteractions(profile.getTotalInteractions() + 1);
        
        // Update user vector (EMA) if movie clicked/viewed
        if (movieId != null && (eventType.equals("click_movie") 
                             || eventType.equals("view_engaged"))) {
            ItemEmbedding itemEmb = embeddingRepo.findById(movieId);
            if (itemEmb != null && itemEmb.getVector() != null) {
                List<Float> newVector = embeddingService.ema(
                    profile.getUserVector(), 
                    itemEmb.getVector(), 
                    0.7f  // 70% weight on new item
                );
                profile.setUserVector(newVector);
            }
            
            // Update genre preferences
            Movie movie = movieService.findMovieById(movieId);
            if (movie != null && movie.getGenres() != null) {
                updateGenreCount(profile, movie.getGenres());
            }
        }
        
        profile.setLastUpdated(Instant.now().toEpochMilli());
        profileRepo.save(profile);
    }
    
    private void updateGenreCount(UserProfile profile, List<String> genres) {
        Map<String, Integer> genreCount = profile.getGenreCount();
        for (String genre : genres) {
            genreCount.put(genre, genreCount.getOrDefault(genre, 0) + 1);
        }
        profile.setGenreCount(genreCount);
        
        // Update top genres (sort by count, take top 5)
        List<String> topGenres = genreCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .toList();
        profile.setTopGenres(topGenres);
    }
    
    private UserProfile initializeProfile(String userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setPriceTier("FREE");
        profile.setPersonalizationEnabled(true);
        profile.setTotalInteractions(0L);
        profile.setLastUpdated(Instant.now().toEpochMilli());
        profile.setTtl(Instant.now().plusSeconds(90L * 86400).getEpochSecond());
        return profile;
    }
    
    public UserProfile getUserProfile(String userId) {
        return profileRepo.findById(userId);
    }
}
```

### 📦 **Phase 1.5: Create Signal Capture Endpoint** (Day 5)

- [ ] **SignalController.java**
```java
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class SignalController {
    
    private final UserBehaviorService behaviorService;
    
    @PostMapping
    public ResponseEntity<Void> captureSignal(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody SignalRequest req) {
        
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String userId = jwt.getSubject();
        behaviorService.trackSignal(
            userId, 
            req.getEventType(), 
            req.getMovieId(), 
            req.getMetadata()
        );
        
        return ResponseEntity.ok().build();
    }
}

@Data
class SignalRequest {
    private String eventType;    // "click_movie", "view_engaged", etc.
    private String movieId;
    private Map<String, Object> metadata;
}
```

---

## ✅ WEEK 2: Smart Recommendation & Embedding Generation

### 📦 **Phase 2.1: Generate Initial Embeddings** (Day 6-7)

- [ ] **EmbeddingInitializer.java** (Run once or via admin endpoint)
```java
@Component
@RequiredArgsConstructor
public class EmbeddingInitializer implements CommandLineRunner {
    
    private final MovieService movieService;
    private final EmbeddingService embeddingService;
    
    @Value("${ml.embedding.auto-generate:false}")
    private boolean autoGenerate;
    
    @Override
    public void run(String... args) throws Exception {
        if (!autoGenerate) {
            log.info("⏭️ Skipping auto-embedding generation (ml.embedding.auto-generate=false)");
            return;
        }
        
        log.info("🚀 Starting initial embedding generation...");
        List<Movie> movies = movieService.findAllMovies();
        embeddingService.generateBatchEmbeddings(movies);
        log.info("✅ Initial embeddings generated for {} movies", movies.size());
    }
}
```

**Hoặc tạo Admin endpoint:**
```java
@PostMapping("/api/admin/embeddings/generate")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Map<String, Object>> generateEmbeddings() {
    List<Movie> movies = movieService.findAllMovies();
    embeddingService.generateBatchEmbeddings(movies);
    return ResponseEntity.ok(Map.of("generated", movies.size()));
}
```

### 📦 **Phase 2.2: Update RecommendationService** (Day 7-8)

- [ ] **Modify RecommendationService.java** với hybrid scoring:

```java
@Service
@RequiredArgsConstructor
public class RecommendationService {
    
    private final MovieRepository movieRepo;
    private final MovieService movieService;
    private final WishlistService wishlistService;
    private final UserBehaviorService behaviorService;
    private final EmbeddingService embeddingService;
    private final ItemEmbeddingRepository embeddingRepo;
    private final UserSignalRepository signalRepo;
    
    public List<MovieSuggestionDTO> recommendForUser(String userId, 
                                                     String currentMovieId, 
                                                     int limit) {
        // 1. Get user profile
        UserProfile profile = behaviorService.getUserProfile(userId);
        List<Float> userVector = (profile != null && profile.getPersonalizationEnabled()) 
            ? profile.getUserVector() 
            : null;
        
        // 2. Get candidates (existing logic)
        Set<String> likedGenres = getLikedGenres(userId, currentMovieId);
        Set<String> exclude = getExcludedMovies(userId, currentMovieId);
        
        List<Movie> candidates;
        if (!likedGenres.isEmpty()) {
            candidates = movieRepo.findTopNByGenresOrderByViewCountDesc(
                new ArrayList<>(likedGenres), 60
            );
        } else {
            candidates = movieRepo.topNMoviesByViewCount(60);
        }
        
        // 3. ✅ NEW: Re-rank with hybrid score
        List<ScoredMovie> scored = candidates.stream()
            .filter(m -> !exclude.contains(m.getMovieId()))
            .map(m -> {
                double score = calculateHybridScore(userId, m, userVector);
                return new ScoredMovie(m, score);
            })
            .sorted(Comparator.comparingDouble(ScoredMovie::getScore).reversed())
            .limit(limit)
            .toList();
        
        // 4. Convert to DTO
        return scored.stream()
            .map(sm -> new MovieSuggestionDTO(
                sm.getMovie().getMovieId(),
                sm.getMovie().getTitle(),
                sm.getMovie().getThumbnailUrl(),
                sm.getMovie().getGenres(),
                sm.getMovie().getViewCount(),
                sm.getMovie().getAvgRating()
            ))
            .toList();
    }
    
    /**
     * ✅ HYBRID SCORE: 60% cosine + 30% CTR + 10% freshness
     */
    private double calculateHybridScore(String userId, Movie movie, List<Float> userVector) {
        // Component 1: Cosine similarity (semantic)
        double cosineSim = 0.5; // default neutral
        if (userVector != null) {
            ItemEmbedding itemEmb = embeddingRepo.findById(movie.getMovieId());
            if (itemEmb != null && itemEmb.getVector() != null) {
                cosineSim = embeddingService.cosineSimilarity(userVector, itemEmb.getVector());
                // Normalize to [0, 1] from [-1, 1]
                cosineSim = (cosineSim + 1.0) / 2.0;
            }
        }
        
        // Component 2: Personal CTR
        double ctr = calculateCTR(userId, movie.getMovieId());
        
        // Component 3: Freshness (newer movies get boost)
        double freshness = calculateFreshness(movie.getReleasedDate());
        
        // Weights: can be tuned via A/B testing
        final double W_COSINE = 0.6;
        final double W_CTR = 0.3;
        final double W_FRESHNESS = 0.1;
        
        return W_COSINE * cosineSim + W_CTR * ctr + W_FRESHNESS * freshness;
    }
    
    private double calculateCTR(String userId, String movieId) {
        long impressions = signalRepo.countByUserAndMovieAndType(userId, movieId, "impression");
        long clicks = signalRepo.countByUserAndMovieAndType(userId, movieId, "click_movie");
        
        if (impressions == 0) {
            // Use global CTR as fallback (TODO: implement global stats)
            return 0.1; // 10% default
        }
        
        return (double) clicks / impressions;
    }
    
    private double calculateFreshness(String releasedDate) {
        if (releasedDate == null || releasedDate.length() < 4) {
            return 0.5; // neutral
        }
        
        try {
            int releaseYear = Integer.parseInt(releasedDate.substring(0, 4));
            int currentYear = java.time.Year.now().getValue();
            int age = currentYear - releaseYear;
            
            // Exponential decay: fresh = e^(-age/5)
            // 0 years old = 1.0, 5 years old = 0.37, 10 years old = 0.13
            return Math.exp(-age / 5.0);
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }
    
    // ... existing helper methods ...
    
    @Data
    @AllArgsConstructor
    private static class ScoredMovie {
        private Movie movie;
        private double score;
    }
}
```

### 📦 **Phase 2.3: Nightly Embedding Refresh Scheduler** (Day 8)

- [ ] **EmbeddingRefreshScheduler.java**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingRefreshScheduler {
    
    private final MovieService movieService;
    private final EmbeddingService embeddingService;
    private final ItemEmbeddingRepository embeddingRepo;
    
    /**
     * Refresh embeddings for new/updated movies
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshEmbeddings() {
        log.info("🔄 Starting nightly embedding refresh...");
        
        List<Movie> movies = movieService.findAllMovies();
        int refreshed = 0;
        
        for (Movie movie : movies) {
            ItemEmbedding existing = embeddingRepo.findById(movie.getMovieId());
            
            // Refresh if:
            // 1. No embedding exists
            // 2. Movie updated after embedding generated
            // 3. Embedding version outdated
            if (needsRefresh(movie, existing)) {
                embeddingService.getOrCreateEmbedding(movie);
                refreshed++;
            }
        }
        
        log.info("✅ Embedding refresh completed | refreshed={} total={}", refreshed, movies.size());
    }
    
    private boolean needsRefresh(Movie movie, ItemEmbedding existing) {
        if (existing == null || existing.getVector() == null) {
            return true;
        }
        
        // Check if embedding version is outdated
        if (!"v1".equals(existing.getEmbeddingVersion())) {
            return true;
        }
        
        // Check if movie updated recently (optional: requires movie.lastUpdated field)
        // if (movie.getLastUpdated() != null && existing.getLastUpdated() != null) {
        //     return movie.getLastUpdated() > existing.getLastUpdated();
        // }
        
        return false;
    }
}
```

---

## ✅ WEEK 3: Persona & Proactive Suggestions

### 📦 **Phase 3.1: Persona Detection** (Day 9-10)

- [ ] **Update AiService.java** để inject persona:

```java
// In AiService.composeAnswer()

// Get user profile for persona
UserProfile profile = behaviorService.getUserProfile(userId);

// Determine persona
String persona = determinePersona(profile);
String tone = determineTone(profile);

String system = """
Bạn là trợ lý AI CartoonToo với phong cách %s.

PERSONA: %s
TOP GENRES: %s
PRICE TIER: %s
RECENT ACTIVITY: 
%s

QUY TẮC:
1) Trả lời ngắn gọn (2-3 câu), thân thiện
2) Dùng "%s" khi xưng hô
3) Gợi ý tối đa 3 phim, giải thích lý do chọn trong 1 câu
4) Nếu user im lặng hoặc do dự, chủ động gợi ý 1 bước tiếp theo

Trả về JSON theo schema.
""".formatted(
    tone,
    persona,
    profile != null ? String.join(", ", profile.getTopGenres()) : "Chưa xác định",
    profile != null ? profile.getPriceTier() : "FREE",
    buildMemorySummary(profile, userId),
    safeUser
);
```

**Helper methods:**
```java
private String determinePersona(UserProfile profile) {
    if (profile == null || profile.getTopGenres().isEmpty()) {
        return "Người mới khám phá phim hoạt hình";
    }
    
    List<String> genres = profile.getTopGenres();
    
    if (genres.contains("Action") && genres.contains("Shounen")) {
        return "Fan anime hành động năng lượng cao (Shounen)";
    } else if (genres.contains("Romance") && genres.contains("Drama")) {
        return "Người thích phim tình cảm, cảm động";
    } else if (genres.contains("Comedy")) {
        return "Người yêu thích phim hài, giải trí nhẹ nhàng";
    } else if (genres.contains("Horror") || genres.contains("Thriller")) {
        return "Fan phim kinh dị, ly kỳ";
    }
    
    return "Người xem phim đa dạng thể loại";
}

private String determineTone(UserProfile profile) {
    if (profile == null) {
        return "thân thiện, hướng dẫn";
    }
    
    List<String> intents = profile.getLastIntents();
    
    // User thích filter trực tiếp → tone hiệu quả
    if (intents.stream().anyMatch(i -> i.contains("filter"))) {
        return "hiệu quả, đi thẳng vào vấn đề";
    }
    
    // User hay hỏi promo → tone tiết kiệm
    if (intents.stream().anyMatch(i -> i.contains("promo"))) {
        return "tiết kiệm, focus vào ưu đãi";
    }
    
    return "nhiệt tình, gợi mở";
}

private String buildMemorySummary(UserProfile profile, String userId) {
    if (profile == null) {
        return "Chưa có hoạt động gần đây";
    }
    
    List<UserSignal> recent = signalRepo.findRecent(userId, 7); // 7 days
    
    if (recent.isEmpty()) {
        return "Chưa có hoạt động gần đây";
    }
    
    return recent.stream()
        .limit(3)
        .map(s -> String.format("- %s: %s", 
            formatEventType(s.getEventType()), 
            s.getMovieTitle() != null ? s.getMovieTitle() : "N/A"))
        .collect(Collectors.joining("\n"));
}

private String formatEventType(String eventType) {
    return switch (eventType) {
        case "click_movie" -> "Xem phim";
        case "view_engaged" -> "Đang xem";
        case "add_wishlist" -> "Thêm watchlist";
        case "click_like" -> "Thích";
        default -> eventType;
    };
}
```

### 📦 **Phase 3.2: Proactive Suggestions** (Day 10-11)

- [ ] **ProactiveSuggestionService.java**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProactiveSuggestionService {
    
    private final UserBehaviorService behaviorService;
    private final UserSignalRepository signalRepo;
    private final PromotionService promotionService;
    
    public Optional<ProactiveSuggestion> detectOpportunity(String userId, String currentContext) {
        UserProfile profile = behaviorService.getUserProfile(userId);
        if (profile == null) {
            return Optional.empty();
        }
        
        List<UserSignal> recent = signalRepo.findRecent(userId, 7);
        
        // Rule 1: Asked about promo ≥2 times → push voucher
        long promoQueries = recent.stream()
            .filter(s -> s.getEventType().equals("search_query"))
            .filter(s -> {
                Object metadata = s.getMetadata().get("wantsPromo");
                return metadata != null && (Boolean) metadata;
            })
            .count();
        
        if (promoQueries >= 2) {
            log.info("🎯 Proactive: User {} asked promo {} times", userId, promoQueries);
            return buildPromoSuggestion();
        }
        
        // Rule 2: Stayed on detail page > 30s without watching
        UserSignal lastSignal = recent.isEmpty() ? null : recent.get(0);
        if (lastSignal != null 
            && lastSignal.getEventType().equals("view_detail")
            && lastSignal.getDwellTime() > 30) {
            
            log.info("🎯 Proactive: User {} dwelling on movie {}", userId, lastSignal.getMovieId());
            return Optional.of(new ProactiveSuggestion(
                "trailer_suggest",
                "Bạn đang quan tâm phim này à? Muốn xem trailer trước không? 🎬",
                Map.of("movieId", lastSignal.getMovieId())
            ));
        }
        
        // Rule 3: Watched 3+ episodes of same series → suggest next season
        Map<String, Long> seriesCount = recent.stream()
            .filter(s -> s.getEventType().equals("view_end"))
            .filter(s -> s.getMovieId() != null)
            .collect(Collectors.groupingBy(UserSignal::getMovieId, Collectors.counting()));
        
        Optional<Map.Entry<String, Long>> topSeries = seriesCount.entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .max(Map.Entry.comparingByValue());
        
        if (topSeries.isPresent()) {
            String seriesId = topSeries.get().getKey();
            log.info("🎯 Proactive: User {} binge-watching series {}", userId, seriesId);
            return Optional.of(new ProactiveSuggestion(
                "series_continue",
                "Mình thấy bạn đang thích series này! Có muốn xem tiếp phần sau không? 📺",
                Map.of("seriesId", seriesId)
            ));
        }
        
        return Optional.empty();
    }
    
    private Optional<ProactiveSuggestion> buildPromoSuggestion() {
        List<Promotion> activePromos = promotionService.getActivePromotions();
        if (activePromos.isEmpty()) {
            return Optional.empty();
        }
        
        Promotion best = activePromos.get(0); // TODO: rank by value
        String message = String.format(
            "Mình thấy bạn quan tâm ưu đãi! Có mã giảm %d%% cho gói Premium, dùng thử không? 🎁",
            best.getDiscountPercent()
        );
        
        return Optional.of(new ProactiveSuggestion(
            "promo_push",
            message,
            Map.of("promotionId", best.getPromotionId(), "code", best.getVoucherCode())
        ));
    }
}

@Data
@AllArgsConstructor
class ProactiveSuggestion {
    private String type;
    private String message;
    private Map<String, Object> data;
}
```

- [ ] **Inject vào AiController.chat():**
```java
// Before calling aiService.composeAnswer()

Optional<ProactiveSuggestion> proactive = proactiveService.detectOpportunity(user.userId, rawQ);

if (proactive.isPresent()) {
    log.info("🎯 Proactive suggestion: {}", proactive.get().getType());
    
    // Inject into extras
    if (extras == null) extras = new HashMap<>();
    extras.put("proactiveSuggestion", proactive.get());
}
```

---

## ✅ WEEK 4: Testing & Monitoring

### 📦 **Phase 4.1: Frontend Integration** (Day 12-13)

- [ ] **Update FE to send signals:**
```javascript
// utils/analytics.js

export const trackSignal = async (eventType, movieId, metadata = {}) => {
  try {
    await fetch('/api/signals', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({ eventType, movieId, metadata })
    });
  } catch (err) {
    console.error('Failed to track signal:', err);
  }
};

// Usage examples:

// When user clicks movie card
onClick={() => {
  trackSignal('click_movie', movie.id, { source: 'chatbot_suggestion' });
  navigate(`/movie/${movie.id}`);
}}

// When user watches > 30s
useEffect(() => {
  const timer = setTimeout(() => {
    if (isPlaying && watchTime > 30) {
      trackSignal('view_engaged', movieId, { watchTime });
    }
  }, 30000);
  return () => clearTimeout(timer);
}, [isPlaying]);

// When user adds to wishlist
trackSignal('add_wishlist', movieId, { source: 'detail_page' });
```

### 📦 **Phase 4.2: A/B Testing Framework** (Day 13-14)

- [ ] **ABTestService.java**
```java
@Service
public class ABTestService {
    
    /**
     * Check if user is in experiment group
     * Uses consistent hashing for stability
     */
    public boolean isInExperimentGroup(String userId, String experimentId, int percentage) {
        int hash = Math.abs((userId + experimentId).hashCode());
        return (hash % 100) < percentage;
    }
    
    /**
     * Get experiment variant for user
     */
    public String getVariant(String userId, String experimentId) {
        if (isInExperimentGroup(userId, experimentId, 90)) {
            return "treatment"; // 90% get new ML-powered recommendations
        }
        return "control"; // 10% get old recommendations
    }
}
```

**Usage in RecommendationService:**
```java
public List<MovieSuggestionDTO> recommendForUser(...) {
    String variant = abTestService.getVariant(userId, "ml_ranking_v1");
    
    if ("treatment".equals(variant)) {
        // Use new hybrid scoring
        return recommendWithML(userId, currentMovieId, limit);
    } else {
        // Use old rule-based
        return recommendWithRules(userId, currentMovieId, limit);
    }
}
```

### 📦 **Phase 4.3: Monitoring Dashboard** (Day 14)

- [ ] **Metrics to track (CloudWatch/Grafana):**
```java
@Component
@RequiredArgsConstructor
public class MLMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    public void recordRecommendation(String userId, String variant, 
                                    List<MovieSuggestionDTO> suggestions) {
        // Count recommendations by variant
        meterRegistry.counter("ml.recommendations.total", 
                            "variant", variant).increment();
        
        // Track suggestion count distribution
        meterRegistry.gauge("ml.recommendations.count", 
                          Tags.of("variant", variant), suggestions.size());
    }
    
    public void recordClick(String userId, String movieId, String variant, int position) {
        // CTR by position
        meterRegistry.counter("ml.recommendations.clicks", 
                            "variant", variant,
                            "position", String.valueOf(position)).increment();
    }
    
    public void recordEmbeddingQuality(double avgCosineSim) {
        // Embedding quality metric
        meterRegistry.gauge("ml.embedding.avg_cosine_similarity", avgCosineSim);
    }
}
```

**Key metrics:**
- `CTR@5` = clicks in top 5 / impressions
- `Watch Time` = avg watch duration per session
- `Conversion Rate` = free → premium upgrades
- `Personalization Score` = avg cosine sim of clicked items

---

## 🎯 SUCCESS CRITERIA

### **Must Have (Week 3 end):**
- [x] All entities created (UserProfile, ItemEmbedding, UserSignal)
- [ ] All repositories working with DynamoDB
- [ ] Signal tracking endpoint live (`/api/signals`)
- [ ] Embeddings generated for all movies
- [ ] Hybrid scoring implemented in RecommendationService
- [ ] Persona detection working in AiService
- [ ] Proactive suggestions integrated

### **Nice to Have (Week 4):**
- [ ] A/B testing framework
- [ ] Monitoring dashboard (Grafana)
- [ ] Nightly embedding refresh scheduler
- [ ] Admin panel for viewing user profiles

---

## 💰 COST ESTIMATION

### **OpenAI Costs:**
- **Embeddings:** text-embedding-3-small = $0.02 / 1M tokens
  - Average movie description: ~200 tokens
  - 1000 movies: 200K tokens = $0.004
  - **Monthly:** ~$0.05 (with refreshes)

- **Chat Completions:** gpt-4o-mini = $0.15/1M input, $0.60/1M output
  - Average query: 1000 input + 500 output tokens
  - 10K queries/month: 10M input + 5M output = $1.50 + $3.00 = **$4.50/month**

**Total ML Cost:** ~$5/month 💰

### **DynamoDB Costs (PAY_PER_REQUEST):**
- UserProfile: ~1K items, 10K reads/month = $0.25
- ItemEmbedding: ~1K items, 100K reads/month = $2.50
- UserSignal: ~100K items/month, 200K writes = $2.50

**Total DB Cost:** ~$5/month

**GRAND TOTAL:** ~$10/month for complete ML stack 🎉

---

## 📚 TESTING CHECKLIST

### **Unit Tests:**
- [ ] `EmbeddingService.cosineSimilarity()` correctness
- [ ] `EmbeddingService.ema()` with edge cases
- [ ] `UserBehaviorService.trackSignal()` updates profile
- [ ] `ProactiveSuggestionService.detectOpportunity()` triggers

### **Integration Tests:**
- [ ] Signal capture → profile update flow
- [ ] Recommendation with ML scoring vs. baseline
- [ ] Persona detection with various user profiles
- [ ] Proactive suggestions triggered correctly

### **Manual Tests:**
- [ ] Chat with new user (no profile) → defaults work
- [ ] Chat with established user → personalized recommendations
- [ ] Ask about promo 3 times → proactive voucher push
- [ ] Click on movie → signal tracked → profile updated

---

## 🚀 DEPLOYMENT

### **Step 1: Create tables**
```bash
aws dynamodb create-table ... # (see Phase 1.3)
```

### **Step 2: Generate embeddings**
```bash
curl -X POST http://localhost:8080/api/admin/embeddings/generate \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### **Step 3: Deploy backend**
```bash
./gradlew build
java -jar build/libs/BackendCartoon-0.0.1-SNAPSHOT.jar
```

### **Step 4: Update frontend**
- Deploy signal tracking code
- Test in staging environment

### **Step 5: Monitor**
- Check CloudWatch metrics
- Watch for errors in logs
- Review A/B test results after 1 week

---

## 🎉 EXPECTED RESULTS

### **Before (Baseline):**
- CTR@5: 8%
- Watch Time: 100%
- Personalization: Rule-based (wishlist only)
- Personality: Generic
- Proactive: None

### **After (Layer 1):**
- CTR@5: **12-15%** (+50-87%)
- Watch Time: **107-110%** (+7-10%)
- Personalization: **Semantic similarity + EMA**
- Personality: **Adaptive tone + persona**
- Proactive: **Rule-based triggers**

### **Score Improvement:**
- Personalization: 5/10 → **8/10** (+0.3)
- Personality: 5/10 → **7/10** (+0.2)
- Proactive: 0/10 → **5/10** (+0.2)
- **Overall: 9.3/10 → 9.8-10.0/10** ⭐⭐⭐⭐⭐

---

**Next:** Review & approve, then start Week 1! 🚀

