package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.services.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authors")
public class AuthorController {
    @Autowired
    private AuthorService authorService;


    @PostMapping("/create")
    public ResponseEntity<String> createAuthor(@RequestBody Author author) {
        try {
            authorService.saveAuthor(author);
            return ResponseEntity.ok("Author saved successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save author: " + e.getMessage());
        }
    }


    @GetMapping("/all")
    public ResponseEntity<List<Author>> getAllAuthors() {
        try {
            List<Author> authors = authorService.findAllAuthors();
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 3. Thêm Movie vào Author (nếu Author đã tồn tại)
    @PostMapping("/add-movie")
    public ResponseEntity<String> addMovieToAuthor(
            @RequestParam String authorId,
            @RequestParam String movieId) {
        try {
            authorService.addMovieToAuthor(authorId, movieId);
            return ResponseEntity.ok("✅ Movie added to author successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Failed to add movie to author: " + e.getMessage());
        }
    }
}