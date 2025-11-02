# 🚀 QUICK TEST GUIDE - FRONTEND

**5 phút để test chatbot AI hoàn chỉnh!**

---

## 📝 CHUẨN BỊ

### **1. Lấy JWT Token**
```bash
# Login để lấy token
POST http://localhost:8080/api/auth/login
{
  "phoneNumber": "0123456789",
  "password": "your_password"
}

# Lưu token vào biến
TOKEN="eyJhbGciOiJIUzI1NiIs..."
```

### **2. Test endpoint**
```bash
curl http://localhost:8080/api/ai/chat
# Should return 401 (needs auth)
```

---

## ⚡ QUICK TESTS (Copy & Run)

### **TEST 1: Đề xuất phim** 🎬
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "gợi ý phim hay",
    "conversationId": "test-001"
  }'
```

**✅ Expected:**
- Response trong 2-3 giây
- `suggestions` array có 3-8 phim
- `answer` giải thích lý do chọn

---

### **TEST 2: Lọc anime (Fast-path)** ⚡
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "cho mình xem anime",
    "conversationId": "test-002"
  }'
```

**✅ Expected:**
- Response < 500ms (SUPER FAST!)
- 8 phim anime
- Backend log: `⏱️ Fast-path completed`

---

### **TEST 3: Hỏi thông tin phim** 📝
```bash
# Thay MOVIE_ID bằng movieId thực tế trong DB
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "phim này nói về gì?",
    "currentMovieId": "MOVIE_ID",
    "conversationId": "test-003"
  }'
```

**✅ Expected:**
- Answer chứa thông tin phim
- Không có suggestions

---

### **TEST 4: Hỏi khuyến mãi** 🎁
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "có khuyến mãi gì không?",
    "conversationId": "test-004"
  }'
```

**✅ Expected:**
- `showPromos: true`
- `promos` array có voucher codes
- Response < 1 giây

---

### **TEST 5: Off-topic** 🚫
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "hôm nay thời tiết thế nào?",
    "conversationId": "test-005"
  }'
```

**✅ Expected:**
- Answer từ chối lịch sự
- Response < 300ms (không gọi LLM)

---

### **TEST 6: Follow-up question** 🧠
```bash
# Message 1
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "gợi ý phim hành động",
    "conversationId": "test-006"
  }'

# Message 2 (same conversationId!)
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "phim đầu tiên có bao nhiêu tập?",
    "conversationId": "test-006"
  }'
```

**✅ Expected:**
- Message 2 hiểu "phim đầu tiên" từ message 1
- Context memory working

---

## 🎨 INTEGRATION VỚI REACT

### **Component đơn giản:**
```jsx
import { useState } from 'react';

function ChatbotQuickTest() {
  const [response, setResponse] = useState(null);

  const testChat = async (message) => {
    const res = await fetch('http://localhost:8080/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        message,
        conversationId: 'test-' + Date.now()
      })
    });
    
    const data = await res.json();
    setResponse(data);
  };

  return (
    <div>
      <h2>Quick Test Chatbot</h2>
      
      <button onClick={() => testChat('gợi ý phim hay')}>
        Test 1: Đề xuất phim
      </button>
      
      <button onClick={() => testChat('cho mình xem anime')}>
        Test 2: Lọc anime
      </button>
      
      <button onClick={() => testChat('có khuyến mãi gì không?')}>
        Test 3: Hỏi khuyến mãi
      </button>
      
      {response && (
        <div>
          <h3>Response:</h3>
          <p>{response.answer}</p>
          
          {response.suggestions?.length > 0 && (
            <div>
              <h4>Suggestions:</h4>
              {response.suggestions.map(m => (
                <div key={m.movieId}>{m.title}</div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
```

---

## 📊 CHECK RESULTS

### **✅ SUCCESS Indicators:**
| Test | Success Criteria |
|------|-----------------|
| Test 1 | Has suggestions, answer explains |
| Test 2 | Response < 500ms, 8 movies |
| Test 3 | Answer has movie info |
| Test 4 | showPromos = true, has vouchers |
| Test 5 | Polite rejection, < 300ms |
| Test 6 | Understands context |

### **❌ FAILURE Indicators:**
- Timeout (> 3s for normal, > 500ms for fast-path)
- No suggestions when should have
- Wrong suggestions (not matching query)
- 500 error
- Empty response

---

## 🔍 DEBUGGING

### **Check Backend Logs:**
```bash
# Trong terminal backend, look for:
⏱️ Fast-path completed | latency=300ms
✅ Promo response built | promos_count=2
🎯 Intent parsed | isPureFilter=true
```

### **Common Issues:**

**1. 401 Unauthorized**
```bash
# Fix: Check token
echo $TOKEN
# Should be long string starting with "eyJ..."
```

**2. Empty suggestions**
```bash
# Check DB has movies
curl http://localhost:8080/api/movies
```

**3. Slow response**
```bash
# Check backend is running
curl http://localhost:8080/actuator/health
```

---

## 🎯 PERFORMANCE TARGETS

| Query Type | Target | Good | Bad |
|------------|--------|------|-----|
| Fast-path | < 500ms | < 300ms | > 1s |
| Normal | < 3s | < 2s | > 5s |
| Promo | < 1s | < 500ms | > 2s |

---

## 📝 TEST CHECKLIST

- [ ] Test 1: Đề xuất phim ✓
- [ ] Test 2: Lọc anime (fast) ✓
- [ ] Test 3: Hỏi info phim ✓
- [ ] Test 4: Hỏi khuyến mãi ✓
- [ ] Test 5: Off-topic ✓
- [ ] Test 6: Follow-up ✓

**All pass?** ✅ Chatbot sẵn sàng production!

---

## 🚀 NEXT STEPS

1. **Import Postman Collection:**
   - File: `CartoonToo_Chatbot_Tests.postman_collection.json`
   - Import vào Postman
   - Update `token` variable
   - Run all tests

2. **Read Full Guide:**
   - `docs/CHATBOT_TESTING_GUIDE.md`
   - Có 30+ test cases chi tiết

3. **Integrate to FE:**
   - Copy React component example
   - Add to your chat UI
   - Style & customize

---

**Happy Testing! 🎉**

**Questions?** Check:
- Full guide: `CHATBOT_TESTING_GUIDE.md`
- Postman: `CartoonToo_Chatbot_Tests.postman_collection.json`
- API docs: `ML_ROADMAP_ANALYSIS.md`

