# 🧪 HƯỚNG DẪN TEST CHATBOT AI - FRONTEND

**CartoonToo Backend - AI Chatbot Testing Guide**  
**Date:** November 2, 2025  
**Version:** 1.0

---

## 🎯 MỤC ĐÍCH TEST

Test các chức năng chính của chatbot:
1. ✅ **Hỏi thông tin phim** - Hiểu context và trả lời chính xác
2. ✅ **Đề xuất phim** - Gợi ý phim phù hợp với user
3. ✅ **Lọc phim theo thể loại** - Fast-path optimization
4. ✅ **Hỏi khuyến mãi** - Trả về promotions
5. ✅ **Off-topic detection** - Từ chối câu hỏi không liên quan

---

## 📋 TEST SCENARIOS

### **SCENARIO 1: HỎI THÔNG TIN PHIM** 📝

#### **Test Case 1.1: Hỏi thông tin phim đang xem**

**Setup:**
```javascript
const request = {
  message: "phim này nói về gì?",
  currentMovieId: "movie123",  // ← Important: phải có movieId
  conversationId: "conv-001"
};
```

**API Call:**
```bash
POST http://localhost:8080/api/ai/chat
Content-Type: application/json
Authorization: Bearer YOUR_JWT_TOKEN

{
  "message": "phim này nói về gì?",
  "currentMovieId": "movie123",
  "conversationId": "conv-001"
}
```

**Expected Response:**
```json
{
  "answer": "One Piece là câu chuyện về Monkey D. Luffy, cậu bé có ước mơ trở thành Vua Hải Tặc. Sau khi ăn trái ác quỷ Gomu Gomu, cậu có khả năng đàn hồi như cao su...",
  "suggestions": [],
  "showSuggestions": false,
  "showPromos": false,
  "promos": []
}
```

**Verify:**
- ✅ Answer chứa thông tin về phim (description, plot)
- ✅ Không có suggestions (vì đang hỏi info)
- ✅ Response time < 3 giây

---

#### **Test Case 1.2: Hỏi số tập**

**Request:**
```json
{
  "message": "phim này có bao nhiêu tập?",
  "currentMovieId": "movie123",
  "conversationId": "conv-001"
}
```

**Expected Response:**
```json
{
  "answer": "One Piece hiện có 1000+ tập và vẫn đang tiếp tục phát sóng. Phim được chia thành nhiều arc khác nhau...",
  "suggestions": [],
  "showSuggestions": false
}
```

**Verify:**
- ✅ Answer chứa số tập chính xác
- ✅ Context-aware (biết đang nói về phim nào)

---

#### **Test Case 1.3: Hỏi đánh giá**

**Request:**
```json
{
  "message": "phim này có hay không?",
  "currentMovieId": "movie123",
  "conversationId": "conv-001"
}
```

**Expected Response:**
```json
{
  "answer": "One Piece có rating 9.2/10 với hơn 500,000 lượt đánh giá. Đây là một trong những anime được yêu thích nhất mọi thời đại...",
  "suggestions": [],
  "showSuggestions": false
}
```

**Verify:**
- ✅ Answer chứa rating & số lượt đánh giá
- ✅ Có opinion về chất lượng phim

---

#### **Test Case 1.4: Hỏi về phim cụ thể (không có currentMovieId)**

**Request:**
```json
{
  "message": "One Piece nói về gì?",
  "conversationId": "conv-001"
}
```

**Expected Response:**
```json
{
  "answer": "One Piece là câu chuyện về Monkey D. Luffy và băng hải tặc Mũ Rơm...",
  "suggestions": [
    {
      "movieId": "movie123",
      "title": "One Piece",
      "thumbnailUrl": "...",
      "genres": ["Action", "Adventure", "Shounen"],
      "viewCount": 5000000,
      "avgRating": 9.2
    }
  ],
  "showSuggestions": true
}
```

**Verify:**
- ✅ Chatbot hiểu tên phim "One Piece"
- ✅ Trả về thông tin chính xác
- ✅ Gợi ý phim trong suggestions

---

### **SCENARIO 2: ĐỀ XUẤT PHIM** 🎬

#### **Test Case 2.1: Đề xuất phim chung chung**

**Request:**
```json
{
  "message": "gợi ý phim hay cho mình",
  "conversationId": "conv-002"
}
```

**Expected Response:**
```json
{
  "answer": "Dựa trên sở thích của bạn, mình gợi ý 3 bộ phim xuất sắc:\n1. Attack on Titan - Phim hành động gay cấn...\n2. Your Name - Phim tình cảm cảm động...\n3. Demon Slayer - Phim chiến đấu đẹp mắt...",
  "suggestions": [
    {
      "movieId": "movie456",
      "title": "Attack on Titan",
      "genres": ["Action", "Drama"],
      "avgRating": 9.0
    },
    {
      "movieId": "movie789",
      "title": "Your Name",
      "genres": ["Romance", "Drama"],
      "avgRating": 8.9
    },
    {
      "movieId": "movie101",
      "title": "Demon Slayer",
      "genres": ["Action", "Shounen"],
      "avgRating": 8.8
    }
  ],
  "showSuggestions": true
}
```

**Verify:**
- ✅ Trả về 3-8 suggestions
- ✅ Answer giải thích lý do chọn từng phim
- ✅ Suggestions có đủ thông tin (title, genres, rating)

---

#### **Test Case 2.2: Đề xuất phim tương tự**

**Request:**
```json
{
  "message": "có phim nào giống One Piece không?",
  "currentMovieId": "movie123",
  "conversationId": "conv-002"
}
```

**Expected Response:**
```json
{
  "answer": "Nếu bạn thích One Piece, mình gợi ý:\n1. Naruto - Cũng là anime shounen với chủ đề phiêu lưu...\n2. Fairy Tail - Về băng nhóm phiêu lưu tương tự...",
  "suggestions": [
    {
      "movieId": "movie202",
      "title": "Naruto",
      "genres": ["Action", "Shounen", "Adventure"]
    },
    {
      "movieId": "movie203",
      "title": "Fairy Tail",
      "genres": ["Action", "Fantasy", "Adventure"]
    }
  ],
  "showSuggestions": true
}
```

**Verify:**
- ✅ Suggestions cùng genre với phim gốc
- ✅ Answer giải thích điểm tương đồng

---

#### **Test Case 2.3: Đề xuất theo mood**

**Request:**
```json
{
  "message": "cho mình phim vui vui",
  "conversationId": "conv-002"
}
```

**Expected Response:**
```json
{
  "answer": "Nếu muốn cười sảng khoái, thử:\n1. Gintama - Hài hước bá đạo...\n2. Kaguya-sama - Hài rom-com...",
  "suggestions": [
    {
      "movieId": "movie304",
      "title": "Gintama",
      "genres": ["Comedy", "Action"]
    },
    {
      "movieId": "movie305",
      "title": "Kaguya-sama: Love is War",
      "genres": ["Comedy", "Romance"]
    }
  ],
  "showSuggestions": true
}
```

**Verify:**
- ✅ Hiểu "vui vui" = comedy genre
- ✅ Suggestions đúng mood

---

### **SCENARIO 3: LỌC PHIM THEO THỂ LOẠI** 🎭

#### **Test Case 3.1: Lọc anime/hoạt hình (FAST-PATH)**

**Request:**
```json
{
  "message": "cho mình xem anime",
  "conversationId": "conv-003"
}
```

**Expected Response:**
```json
{
  "answer": "Đây là các anime hot nhất hiện nay:",
  "suggestions": [
    {"movieId": "movie401", "title": "Attack on Titan", "genres": ["Action", "Drama"]},
    {"movieId": "movie402", "title": "My Hero Academia", "genres": ["Action", "Shounen"]},
    {"movieId": "movie403", "title": "Jujutsu Kaisen", "genres": ["Action", "Supernatural"]},
    // ... up to 8 movies
  ],
  "showSuggestions": true
}
```

**Verify:**
- ✅ Response time < 500ms (FAST-PATH!)
- ✅ Không gọi OpenAI LLM (check logs)
- ✅ Trả về 8 phim

---

#### **Test Case 3.2: Lọc hành động**

**Request:**
```json
{
  "message": "phim hành động",
  "conversationId": "conv-003"
}
```

**Expected Response:**
```json
{
  "answer": "Các phim hành động hay nhất:",
  "suggestions": [
    {"title": "Attack on Titan", "genres": ["Action", "Drama"]},
    {"title": "Demon Slayer", "genres": ["Action", "Shounen"]},
    // ... more action movies
  ],
  "showSuggestions": true
}
```

**Verify:**
- ✅ Fast response (< 500ms)
- ✅ Tất cả phim có genre "Action"

---

#### **Test Case 3.3: Lọc nhiều thể loại**

**Request:**
```json
{
  "message": "phim hành động tình cảm",
  "conversationId": "conv-003"
}
```

**Expected Response:**
```json
{
  "answer": "Phim kết hợp hành động và tình cảm:",
  "suggestions": [
    {"title": "Sword Art Online", "genres": ["Action", "Romance", "Fantasy"]},
    {"title": "Inuyasha", "genres": ["Action", "Romance", "Adventure"]},
    // ...
  ],
  "showSuggestions": true
}
```

**Verify:**
- ✅ Suggestions có CẢ HAI genres: Action + Romance

---

### **SCENARIO 4: HỎI KHUYẾN MÃI** 🎁

#### **Test Case 4.1: Hỏi khuyến mãi**

**Request:**
```json
{
  "message": "có khuyến mãi gì không?",
  "conversationId": "conv-004"
}
```

**Expected Response:**
```json
{
  "answer": "Hiện tại có các khuyến mãi hấp dẫn:\n1. Giảm 30% gói Premium - Mã: PREMIUM30...\n2. Giảm 20% gói Basic - Mã: BASIC20...",
  "suggestions": [],
  "showSuggestions": false,
  "showPromos": true,
  "promos": [
    {
      "promotionId": "promo001",
      "title": "Giảm 30% Premium",
      "voucherCode": "PREMIUM30",
      "discountPercent": 30,
      "validFrom": "2025-11-01",
      "validTo": "2025-11-30"
    },
    {
      "promotionId": "promo002",
      "title": "Giảm 20% Basic",
      "voucherCode": "BASIC20",
      "discountPercent": 20,
      "validFrom": "2025-11-01",
      "validTo": "2025-11-30"
    }
  ]
}
```

**Verify:**
- ✅ showPromos = true
- ✅ promos array có đầy đủ thông tin
- ✅ Answer liệt kê khuyến mãi với mã code

---

#### **Test Case 4.2: Hỏi voucher**

**Request:**
```json
{
  "message": "có voucher không?",
  "conversationId": "conv-004"
}
```

**Expected Response:**
```json
{
  "answer": "Có các voucher sau:\n- PREMIUM30: Giảm 30% gói Premium\n- BASIC20: Giảm 20% gói Basic",
  "showPromos": true,
  "promos": [...]
}
```

**Verify:**
- ✅ Hiểu "voucher" = khuyến mãi
- ✅ Trả về promos

---

### **SCENARIO 5: OFF-TOPIC DETECTION** 🚫

#### **Test Case 5.1: Hỏi thời tiết**

**Request:**
```json
{
  "message": "hôm nay thời tiết thế nào?",
  "conversationId": "conv-005"
}
```

**Expected Response:**
```json
{
  "answer": "Xin lỗi, mình chỉ có thể giúp bạn về phim hoạt hình thôi. Bạn muốn tìm phim gì không?",
  "suggestions": [],
  "showSuggestions": false
}
```

**Verify:**
- ✅ Từ chối lịch sự
- ✅ Redirect về phim
- ✅ Fast response (< 300ms, không gọi LLM)

---

#### **Test Case 5.2: Hỏi toán học**

**Request:**
```json
{
  "message": "2+2 bằng mấy?",
  "conversationId": "conv-005"
}
```

**Expected Response:**
```json
{
  "answer": "Mình là trợ lý phim hoạt hình, không giỏi toán lắm 😅. Thử hỏi mình về phim nhé!",
  "suggestions": [],
  "showSuggestions": false
}
```

**Verify:**
- ✅ Từ chối nhưng friendly
- ✅ Fast response

---

### **SCENARIO 6: CONTEXT & MEMORY** 🧠

#### **Test Case 6.1: Follow-up question**

**Conversation Flow:**

**Message 1:**
```json
{
  "message": "gợi ý phim hành động",
  "conversationId": "conv-006"
}
```

**Response 1:**
```json
{
  "answer": "Mình gợi ý Attack on Titan, Demon Slayer...",
  "suggestions": [...]
}
```

**Message 2 (Follow-up):**
```json
{
  "message": "phim đầu tiên có bao nhiêu tập?",
  "conversationId": "conv-006"  // ← Same conversationId
}
```

**Response 2:**
```json
{
  "answer": "Attack on Titan có 4 season với tổng cộng 87 tập...",
  "suggestions": []
}
```

**Verify:**
- ✅ Chatbot nhớ "phim đầu tiên" = "Attack on Titan"
- ✅ Context from previous message

---

#### **Test Case 6.2: Clarification**

**Message 1:**
```json
{
  "message": "phim này có hay không?",
  "conversationId": "conv-006"
}
```

**Response 1:**
```json
{
  "answer": "Bạn đang hỏi về phim nào vậy? Hiện tại mình chưa biết bạn đang xem phim gì.",
  "suggestions": []
}
```

**Verify:**
- ✅ Yêu cầu clarification khi thiếu context

---

### **SCENARIO 7: EDGE CASES** ⚠️

#### **Test Case 7.1: Empty message**

**Request:**
```json
{
  "message": "",
  "conversationId": "conv-007"
}
```

**Expected Response:**
```json
{
  "answer": "Bạn muốn hỏi gì về phim nhỉ? Mình có thể giúp bạn tìm phim hay, thông tin phim, hoặc khuyến mãi đấy!",
  "suggestions": []
}
```

---

#### **Test Case 7.2: Very long message**

**Request:**
```json
{
  "message": "tôi muốn xem phim hành động có nội dung về võ thuật và phép thuật với nhân vật chính là nam giới trẻ tuổi có sức mạnh đặc biệt được thừa hưởng từ gia đình và phải chiến đấu với các thế lực tà ác để bảo vệ thế giới...",
  "conversationId": "conv-007"
}
```

**Expected Response:**
```json
{
  "answer": "Dựa trên mô tả của bạn, mình gợi ý:\n1. Jujutsu Kaisen - Có phép thuật và chiến đấu...\n2. Black Clover - Về ma pháp và sức mạnh...",
  "suggestions": [...]
}
```

**Verify:**
- ✅ Handle long input
- ✅ Extract key requirements

---

#### **Test Case 7.3: Vietnamese with typos**

**Request:**
```json
{
  "message": "cho minh xem phim hannh dong",  // typos: minh, hannh
  "conversationId": "conv-007"
}
```

**Expected Response:**
```json
{
  "answer": "Các phim hành động hay nhất:",
  "suggestions": [...]
}
```

**Verify:**
- ✅ Vẫn hiểu được ý (spelling tolerance)

---

## 🎨 FRONTEND IMPLEMENTATION

### **React Example:**

```javascript
import { useState } from 'react';

function Chatbot() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [conversationId, setConversationId] = useState(null);
  const [currentMovieId, setCurrentMovieId] = useState(null);

  const sendMessage = async () => {
    if (!input.trim()) return;

    // Add user message to UI
    const userMsg = { role: 'user', content: input };
    setMessages([...messages, userMsg]);

    try {
      const response = await fetch('http://localhost:8080/api/ai/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          message: input,
          currentMovieId: currentMovieId,
          conversationId: conversationId || `conv-${Date.now()}`
        })
      });

      const data = await response.json();

      // Save conversationId for follow-up
      if (!conversationId) {
        setConversationId(`conv-${Date.now()}`);
      }

      // Add assistant message to UI
      const assistantMsg = { 
        role: 'assistant', 
        content: data.answer,
        suggestions: data.suggestions,
        promos: data.promos
      };
      setMessages([...messages, userMsg, assistantMsg]);

      setInput('');
    } catch (error) {
      console.error('Chat error:', error);
    }
  };

  return (
    <div className="chatbot">
      <div className="messages">
        {messages.map((msg, idx) => (
          <div key={idx} className={`message ${msg.role}`}>
            <p>{msg.content}</p>
            
            {/* Render suggestions */}
            {msg.suggestions && msg.suggestions.length > 0 && (
              <div className="suggestions">
                {msg.suggestions.map(movie => (
                  <MovieCard 
                    key={movie.movieId} 
                    movie={movie}
                    onClick={() => {
                      setCurrentMovieId(movie.movieId);
                      navigate(`/movie/${movie.movieId}`);
                    }}
                  />
                ))}
              </div>
            )}

            {/* Render promotions */}
            {msg.promos && msg.promos.length > 0 && (
              <div className="promos">
                {msg.promos.map(promo => (
                  <PromoCard key={promo.promotionId} promo={promo} />
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="input-area">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="Hỏi về phim, khuyến mãi..."
        />
        <button onClick={sendMessage}>Gửi</button>
      </div>
    </div>
  );
}
```

---

## 📊 PERFORMANCE BENCHMARKS

| Scenario | Expected Response Time | Notes |
|----------|------------------------|-------|
| Fast-path (filter) | < 500ms | Không gọi LLM |
| Off-topic | < 300ms | Pre-filter |
| Ask info (with movie) | < 2s | Có context |
| Recommendation | < 3s | Cần query DB |
| Promo query | < 1s | Direct data return |

---

## ✅ TEST CHECKLIST

### **Functional Tests:**
- [ ] Hỏi thông tin phim đang xem
- [ ] Hỏi thông tin phim cụ thể
- [ ] Đề xuất phim chung
- [ ] Đề xuất phim tương tự
- [ ] Lọc theo thể loại (anime, hành động, etc.)
- [ ] Hỏi khuyến mãi
- [ ] Off-topic detection
- [ ] Follow-up questions
- [ ] Context memory

### **Performance Tests:**
- [ ] Fast-path < 500ms
- [ ] Normal query < 3s
- [ ] No timeout errors

### **Edge Cases:**
- [ ] Empty message
- [ ] Very long message
- [ ] Typos tolerance
- [ ] Special characters
- [ ] Multiple genres

### **UI/UX:**
- [ ] Messages display correctly
- [ ] Suggestions render as movie cards
- [ ] Promos render as promo cards
- [ ] Loading state
- [ ] Error handling

---

## 🔍 DEBUGGING TIPS

### **1. Check Backend Logs:**
```bash
# Look for:
⏱️ Intent parsed | isPureFilter=true    ← Fast-path triggered
⏱️ Fast-path completed | latency=300ms   ← Performance good
🎯 Proactive suggestion: promo_push      ← Proactive working
✅ Promo response built | promos_count=2 ← Promo query working
```

### **2. Network Tab:**
```
POST /api/ai/chat
Status: 200 OK
Time: 1.2s  ← Should be < 3s
Response size: 5KB
```

### **3. Common Issues:**

**Issue: "Conversation not found"**
→ Solution: Make sure conversationId is consistent across messages

**Issue: "No suggestions returned"**
→ Check: Is it an info query? (shouldn't have suggestions)

**Issue: "Timeout"**
→ Check: Backend logs for LLM timeout, reduce context size

---

## 📈 SUCCESS METRICS

Track these metrics:
- **Response time:** < 3s for 95% queries
- **Accuracy:** User clicks on suggestions > 15%
- **Engagement:** 3+ messages per conversation
- **Satisfaction:** Users upvote answers > 70%

---

## 🎯 NEXT STEPS

1. **Run all test cases** in this guide
2. **Document any issues** found
3. **Test edge cases** thoroughly
4. **Collect user feedback** in beta
5. **Iterate based on data**

---

**Happy Testing! 🚀**

For questions, refer to:
- API documentation: `docs/ML_ROADMAP_ANALYSIS.md`
- Architecture: `docs/VISUAL_DIAGRAMS.md`
- Code: `controllers/AiController.java`

