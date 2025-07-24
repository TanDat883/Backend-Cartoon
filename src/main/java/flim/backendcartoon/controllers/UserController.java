package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.User;
import flim.backendcartoon.repositories.PaymentOrderRepository;
import flim.backendcartoon.services.PaymentService;
import flim.backendcartoon.services.PackageVipService;
import flim.backendcartoon.services.S3Service;
import flim.backendcartoon.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final PackageVipService packageVipService;
    private final PaymentService paymentService;
    private final S3Service s3Service;

    @Autowired
    public UserController(UserService userService, PackageVipService packageVipService,
                          PaymentService paymentService, PaymentOrderRepository paymentOrderRepository
                            , S3Service s3Service) {
        this.s3Service = s3Service;
        this.userService = userService;
        this.packageVipService = packageVipService;
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

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        if (users.isEmpty()) {
            return ResponseEntity.noContent().build(); // HTTP 204 nếu danh sách rỗng
        }
        return ResponseEntity.ok(users); // HTTP 200 và trả về danh sách người dùng
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
        existingUser.setDob(user.getDob());

        userService.updateUser(existingUser);
        return ResponseEntity.ok("User updated successfully!");
    }

}
