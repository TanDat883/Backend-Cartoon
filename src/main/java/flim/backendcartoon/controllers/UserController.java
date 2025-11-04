package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.User;
import flim.backendcartoon.entities.VipSubscription;
import flim.backendcartoon.repositories.PaymentDetailRepository;
import flim.backendcartoon.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final VipSubscriptionService vipSubscriptionService;
    private final PaymentService paymentService;
    private final S3Service s3Service;

    @Autowired
    public UserController(UserService userService, VipSubscriptionService vipSubscriptionService,
                          PaymentService paymentService, PaymentDetailRepository paymentDetailRepository
                            , S3Service s3Service) {
        this.s3Service = s3Service;
        this.userService = userService;
        this.vipSubscriptionService = vipSubscriptionService;
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createUser(@RequestBody User user) {
        if (user.getUserName() == null || user.getPhoneNumber() == null ) {
            return ResponseEntity.badRequest().body("Thông tin người dùng không hợp lệ");
        }

        user.setUserId(UUID.randomUUID().toString());

        userService.createUser(user);
        return ResponseEntity.ok("User created successfully!");
    }

    @PostMapping("/findByPhoneNumber")
    public ResponseEntity<?> findUserByPhoneNumber(@RequestBody Map<String, String> payload) {
        String phoneNumber = payload.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Số điện thoại không được để trống");
        }

        User user = userService.findUserByPhoneNumber(phoneNumber);
        if (user == null) {
            System.out.println("Không tìm thấy người dùng với số điện thoại: " + phoneNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy người dùng với số điện thoại: " + phoneNumber);
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Page<User> users = userService.findAllUsers(page, size, keyword);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(users.getTotalElements()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(users.getContent());
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        User user = userService.findUserById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy người dùng với ID: " + id);
        }
        return ResponseEntity.ok(user);
    }

    //update user
    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateUser(@PathVariable String id,
                                        @RequestPart("user") User user,
                                        @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (user.getUserName() == null) {
            return ResponseEntity.badRequest().body("Thông tin người dùng không hợp lệ");
        }

        User existingUser = userService.findUserById(id);
        if (existingUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy người dùng với ID: " + id);
        }
        // Nếu có ảnh thì upload và set avatarUrl
        if (file != null && !file.isEmpty()) {
            String avatarUrl = s3Service.uploadAvatarUrl(file);
            existingUser.setAvatarUrl(avatarUrl);
        }
        // Cập nhật thông tin người dùng
        existingUser.setUserName(user.getUserName());
        existingUser.setEmail(user.getEmail());
        existingUser.setGender(user.getGender());
        existingUser.setDob(user.getDob());

        userService.updateUser(existingUser);
        return ResponseEntity.ok("User updated successfully!");
    }

    @GetMapping("/{userId}/subscription-packages")
    public ResponseEntity<?> getUserSubscriptionPackages(@PathVariable String userId) {
        List<VipSubscription> packages = vipSubscriptionService.UserVipSubscriptions(userId);
        return ResponseEntity.ok(packages);
    }

    /**
     * Check VIP subscription status for watch party eligibility
     * Required: COMBO_PREMIUM_MEGA_PLUS package with ACTIVE status
     */
    @GetMapping("/{userId}/vip-status")
    public ResponseEntity<?> getVipStatus(@PathVariable String userId) {
        try {
            User user = userService.findUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Không tìm thấy người dùng"));
            }

            // Tìm subscription COMBO_PREMIUM_MEGA_PLUS đang active
            VipSubscription vipSub = vipSubscriptionService
                    .findActiveVipByUserIdAndPackageType(
                            userId,
                            flim.backendcartoon.entities.PackageType.COMBO_PREMIUM_MEGA_PLUS
                    );

            if (vipSub == null) {
                // Trả về thông tin user không có gói COMBO
                return ResponseEntity.ok(Map.of(
                        "userId", userId,
                        "packageType", "",
                        "status", "NONE",
                        "endDate", "",
                        "message", "Chưa có gói COMBO PREMIUM"
                ));
            }

            // Trả về thông tin subscription
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "vipId", vipSub.getVipId() != null ? vipSub.getVipId() : "",
                    "packageId", vipSub.getPackageId() != null ? vipSub.getPackageId() : "",
                    "packageType", vipSub.getPackageType().name(),
                    "status", vipSub.getStatus() != null ? vipSub.getStatus() : "",
                    "startDate", vipSub.getStartDate() != null ? vipSub.getStartDate() : "",
                    "endDate", vipSub.getEndDate() != null ? vipSub.getEndDate() : "",
                    "orderCode", vipSub.getOrderCode() != null ? vipSub.getOrderCode() : ""
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
