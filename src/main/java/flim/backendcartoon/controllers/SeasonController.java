package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.Season;
import flim.backendcartoon.services.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/seasons")
public class SeasonController {

    @Autowired private SeasonService seasonService;

    @PostMapping("/create")
    public ResponseEntity<?> createSeason(
            @RequestParam("movieId") String movieId,
            @RequestParam("seasonNumber") Integer seasonNumber,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "releaseYear", required = false) Integer releaseYear,
            @RequestParam(value = "posterUrl", required = false) String posterUrl
    ) {
        try {
            if (seasonNumber == null || seasonNumber < 1) {
                return ResponseEntity.badRequest().body("seasonNumber phải >= 1");
            }
            Season s = new Season();
            s.setMovieId(movieId);
            s.setSeasonNumber(seasonNumber);
            s.setSeasonId(UUID.randomUUID().toString());
            s.setTitle(title != null ? title : ("Phần " + seasonNumber));
            s.setDescription(description);
            s.setReleaseYear(releaseYear);
            s.setPosterUrl(posterUrl);
            s.setCreatedAt(Instant.now());
            s.setLastUpdated(Instant.now());

            seasonService.save(s);
            return ResponseEntity.ok(s);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to create season: " + e.getMessage());
        }
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<?> getSeasonsByMovie(@PathVariable String movieId) {
        List<Season> list = seasonService.findByMovieId(movieId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/movie/{movieId}/number/{seasonNumber}")
    public ResponseEntity<?> getSeason(
            @PathVariable String movieId,
            @PathVariable int seasonNumber) {
        Season s = seasonService.findOne(movieId, seasonNumber);
        return ResponseEntity.ok(s);
    }

    @DeleteMapping("/movie/{movieId}/number/{seasonNumber}")
    public ResponseEntity<?> deleteSeason(
            @PathVariable String movieId,
            @PathVariable int seasonNumber) {
        seasonService.delete(movieId, seasonNumber);
        return ResponseEntity.ok("Deleted");
    }
}