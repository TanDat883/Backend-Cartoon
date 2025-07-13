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

@RestController
@RequestMapping("/episodes")
public class EpisodeController {

    @Autowired
    private EpisodeService episodeService;
    @Autowired
    private S3Service s3Service;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadEpisode(
            @RequestParam("movieId") String movieId,
            @RequestParam("title") String title,
            @RequestParam("episodeNumber") Integer episodeNumber,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "videoLink",required = false) String videoLink
    ) {
        try {
            String videoUrl;
            if(video!=null && !video.isEmpty()) {
                videoUrl = s3Service.uploadVideo(video);
            } else if (videoLink!=null && !videoLink.isBlank()) {
                videoUrl = videoLink;
            }else {
                    return ResponseEntity.badRequest().body("Video file or video link is required");
            }

            Episode episode = new Episode();
            episode.setEpisodeId(UUID.randomUUID().toString());
            episode.setMovieId(movieId);
            episode.setTitle(title);
            episode.setEpisodeNumber(episodeNumber);
            episode.setVideoUrl(videoUrl);
            episode.setCreatedAt(Instant.now().toString());

            episodeService.saveEpisode(episode);

            return ResponseEntity.ok(episode);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload episode: " + e.getMessage());
        }
    }


    @GetMapping("/movie/{movieId}")
    public ResponseEntity<?> getEpisodesByMovie(@PathVariable String movieId) {
        List<Episode> episodes = episodeService.findEpisodesByMovieId(movieId);
        return ResponseEntity.ok(episodes);
    }

    //countEpisodesByMovieId
    @GetMapping("/count/{movieId}")
    public ResponseEntity<?> countEpisodesByMovieId(@PathVariable String movieId) {
        int count = episodeService.countEpisodesByMovieId(movieId);
        return ResponseEntity.ok(Map.of("count", count)); // ✅ bọc trong object JSON
    }
}
