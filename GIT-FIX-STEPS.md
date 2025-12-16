# 🔧 Git Push Fix - Step by Step

## ⚠️ VẤN ĐỀ:
SSH key của bạn (trongtinIUH) không có quyền push vào repo TanDat883/Backend-Cartoon

## ✅ GIẢI PHÁP (3 BƯỚC):

### Bước 1: Đổi lại HTTPS
```bash
git remote set-url origin https://github.com/TanDat883/Backend-Cartoon.git
```

### Bước 2: Xóa Windows Credentials cũ

**Cách 1 (GUI):**
1. Nhấn `Windows + R`
2. Gõ: `control /name Microsoft.CredentialManager`
3. Click **Windows Credentials**
4. Tìm tất cả entries có `git:https://github.com`
5. Click từng cái → **Remove**

**Cách 2 (Command):**
```bash
# List credentials
cmdkey /list | findstr github

# Delete credential (thay <target> bằng tên từ list)
cmdkey /delete:git:https://github.com
```

### Bước 3: Push với credentials đúng
```bash
git push
```

**Khi hỏi credentials:**
- Username: `TanDat883`
- Password: `<PERSONAL_ACCESS_TOKEN>`

---

## 🔑 TẠO PERSONAL ACCESS TOKEN:

### Nếu chưa có token (dành cho TanDat883):

1. Vào: https://github.com/settings/tokens
2. Click: **Generate new token (classic)**
3. Note: `Backend-Cartoon push`
4. Expiration: `90 days` (hoặc `No expiration`)
5. Select scopes:
   - ✅ **repo** (full control of private repositories)
6. Click: **Generate token**
7. **COPY TOKEN NGAY** (chỉ hiện 1 lần!)
8. Lưu token vào file text an toàn

### Sử dụng token:
```bash
git push

# Username: TanDat883
# Password: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

## 🚨 LƯU Ý QUAN TRỌNG:

1. **KHÔNG** dùng password GitHub thường → Sẽ fail!
2. **PHẢI** dùng Personal Access Token
3. Token chỉ hiện 1 lần → Save ngay!
4. Token hết hạn → Tạo token mới

---

## 🎯 SAU KHI FIX:

```bash
# Verify remote đúng
git remote -v
# Should show:
# origin  https://github.com/TanDat883/Backend-Cartoon.git (fetch)
# origin  https://github.com/TanDat883/Backend-Cartoon.git (push)

# Check status
git status

# Push
git push origin tin-gpt
```

---

## 🔄 ALTERNATIVE: Nếu bạn KHÔNG phải TanDat883

### Option A: Xin quyền collaborator
1. Nhờ TanDat883 add bạn làm collaborator
2. Accept invite
3. Push bình thường

### Option B: Fork & Pull Request
1. Fork repo về account trongtinIUH
2. Push code lên fork của bạn
3. Tạo Pull Request từ fork → repo gốc

---

## ✅ CHECKLIST:

- [ ] Đổi remote sang HTTPS
- [ ] Xóa Windows Credentials cũ
- [ ] Có Personal Access Token của TanDat883
- [ ] Push thành công
- [ ] Verify code đã lên GitHub

---

**Good luck!** 🚀

