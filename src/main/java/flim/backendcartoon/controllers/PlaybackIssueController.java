package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.DTO.request.PlaybackIssueCreateRequest;
import flim.backendcartoon.entities.PlaybackIssueStatus;
import flim.backendcartoon.entities.PlaybackIssueType;
import flim.backendcartoon.services.PlaybackIssueService;
import flim.backendcartoon.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "http://localhost:3000")
@Validated
public class PlaybackIssueController {

    private final PlaybackIssueService svc;
    private final UserService userService;

    public PlaybackIssueController(PlaybackIssueService svc, UserService userService) {
        this.svc = svc;
        this.userService = userService;
    }

    @PostMapping("/playback")
    public ResponseEntity<?> create(
            @RequestHeader("userId") String userId,
            @Valid @RequestBody PlaybackIssueCreateRequest req) {

        if (userId == null || userId.isBlank() || userService.findUserById(userId) == null) {
            return ResponseEntity.status(401).body("Bạn phải đăng nhập để báo lỗi.");
        }

        PlaybackIssueType type;
        try {
            type = PlaybackIssueType.valueOf(req.getType().trim().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("type phải là VIDEO/AUDIO/SUBTITLE/OTHER");
        }

        var created = svc.createOrBump(
                userId,
                req.getMovieId(),
                req.getSeasonId(),
                req.getEpisodeNumber(),
                type,
                req.getDetail()
        );
        // 201 Created là hợp lý khi tạo/bump xong
        return ResponseEntity.status(201).body(created);
    }

    // Admin xem report theo 1 phim/tập

    @GetMapping("/playback")
    public ResponseEntity<?> listByTarget(
            @RequestParam String movieId,
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false) Integer episodeNumber) {
        return ResponseEntity.ok(svc.listByTarget(movieId, seasonId, episodeNumber));
    }

    // Admin đổi trạng thái
    @PatchMapping("/playback/{issueId}/status")
    public ResponseEntity<?> setStatus(
            @PathVariable String issueId,

            @RequestParam String movieId,
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false) Integer episodeNumber,
            @RequestParam String status,
            @RequestParam(defaultValue = "true") boolean enableTtl) {

        PlaybackIssueStatus st;
        try {
            st = PlaybackIssueStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("status phải là OPEN/IN_PROGRESS/RESOLVED/INVALID");
        }

        return ResponseEntity.ok(
                svc.updateStatus(movieId, seasonId, episodeNumber, issueId, st, enableTtl)
        );
    }



    @GetMapping("/playback/movie/{movieId}")
    public ResponseEntity<?> listByMovie(@PathVariable String movieId) {

        return ResponseEntity.ok(svc.listByMovie(movieId));
    }

    // (tuỳ chọn) handler chung cho IllegalArgumentException -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
