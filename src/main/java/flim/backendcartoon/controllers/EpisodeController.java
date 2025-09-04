package flim.backendcartoon.controllers;


import flim.backendcartoon.entities.Episode;
import flim.backendcartoon.services.EpisodeService;
import flim.backendcartoon.services.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/episodes")
public class EpisodeController {

    @Autowired
    private EpisodeService episodeService;
    @Autowired
    private S3Service s3Service;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadEpisode(
            @RequestParam("seasonId") String seasonId,
            @RequestParam("movieId") String movieId,
            @RequestParam("title") String title,
            @RequestParam("episodeNumber") Integer episodeNumber,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "videoLink",required = false) String videoLink
    ) {
        try {
            String videoUrl;
            if(video!=null && !video.isEmpty()) {
                videoUrl = s3Service.convertAndUploadToHLS(video);
            } else if (videoLink!=null && !videoLink.isBlank()) {
                videoUrl = videoLink;
            }else {
                    return ResponseEntity.badRequest().body("Video file or video link is required");
            }

            Episode ep = new Episode();
            ep.setEpisodeId(UUID.randomUUID().toString());
            ep.setSeasonId(seasonId);
            ep.setMovieId(movieId);
            ep.setTitle(title);
            ep.setEpisodeNumber(episodeNumber);
            ep.setVideoUrl(videoUrl);
            ep.setCreatedAt(Instant.now());
            ep.setUpdatedAt(Instant.now());

            episodeService.saveEpisode(ep);

            return ResponseEntity.ok(ep);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload episode: " + e.getMessage());
        }
    }

    //find by episodeId
    @GetMapping("/season/{seasonId}")
    public ResponseEntity<?> getEpisodesBySeason(@PathVariable String seasonId) {
        List<Episode> episodes = episodeService.findEpisodesBySeasonId(seasonId);
        return ResponseEntity.ok(episodes);
    }

    // Đếm tập theo season
    @GetMapping("/season/{seasonId}/count")
    public ResponseEntity<?> countBySeason(@PathVariable String seasonId) {
        int count = episodeService.countBySeasonId(seasonId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Lấy 1 tập theo composite key
    @GetMapping("/season/{seasonId}/ep/{episodeNumber}")
    public ResponseEntity<?> getEpisode(
            @PathVariable String seasonId,
            @PathVariable Integer episodeNumber) {
        Episode ep = episodeService.findOne(seasonId, episodeNumber);
        return ResponseEntity.ok(ep);
    }

    // UPDATE title / video (tuỳ chọn)
    @PutMapping(value = "/season/{seasonId}/ep/{episodeNumber}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateEpisode(
            @PathVariable String seasonId,
            @PathVariable Integer episodeNumber,
            @RequestParam(value = "title", required = false) String title,
            @RequestPart(value = "video", required = false) MultipartFile video
    ) {
        try {
            Episode existing = episodeService.findOne(seasonId, episodeNumber);

            if (title != null) existing.setTitle(title);

            if (video != null && !video.isEmpty()) {
                // nếu bạn muốn giữ lại file cũ thì bỏ dòng xoá này
                s3Service.deleteByMediaUrl(existing.getVideoUrl());
                String newUrl = s3Service.convertAndUploadToHLS(video);
                existing.setVideoUrl(newUrl);
            }
            existing.setUpdatedAt(Instant.now());
            episodeService.update(existing);
            return ResponseEntity.ok(existing);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Failed to update episode: " + e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/season/{seasonId}/ep/{episodeNumber}")
    public ResponseEntity<?> deleteEpisode(
            @PathVariable String seasonId,
            @PathVariable Integer episodeNumber
    ) {
        try {
            Episode existing = episodeService.findOne(seasonId, episodeNumber);
            if (existing != null) {
                // Xoá HLS/folder nếu cần
                s3Service.deleteByMediaUrl(existing.getVideoUrl());
            }
            episodeService.delete(seasonId, episodeNumber);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Failed to delete episode: " + e.getMessage());
        }
    }

    @GetMapping("/{episodeId}")
    public ResponseEntity<?> getEpisodeById(@PathVariable String episodeId) {
        Episode ep = episodeService.findById(episodeId);
        if (ep == null) return ResponseEntity.status(404).body("Episode not found");
        return ResponseEntity.ok(ep);
    }



}
