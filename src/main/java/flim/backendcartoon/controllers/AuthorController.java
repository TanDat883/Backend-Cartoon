package flim.backendcartoon.controllers;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.entities.AuthorRole;
import flim.backendcartoon.exception.AuthorException;
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
            return ResponseEntity.ok("Author created with ID: " + author.getAuthorId());
        } catch (IllegalStateException dup) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate author");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed: " + e.getMessage());
        }
    }
    @GetMapping("/all")
    public ResponseEntity<List<Author>> getAllAuthors() {
        try { return ResponseEntity.ok(authorService.findAllAuthors()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
    }

    @PostMapping("/add-movie")
    public ResponseEntity<String> addMovieToAuthor(@RequestParam List<String> authorIds, @RequestParam String movieId) {
        try { authorService.addMovieToAuthor(authorIds, movieId); return ResponseEntity.ok("OK"); }
        catch (Exception e) { return ResponseEntity.badRequest().body("Failed: " + e.getMessage()); }
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<Author>> getAuthorsByMovieId(@PathVariable String movieId) {
        try { return ResponseEntity.ok(authorService.findAuthorsByMovieId(movieId)); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); }
    }

    // NEW: search trùng name+role
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String name, @RequestParam AuthorRole role) {
        Author a = authorService.findByNameAndRole(name, role);
        if (a == null) return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.ok(a);
    }

    // NEW: update
    @PutMapping("/{authorId}")
    public ResponseEntity<?> update(@PathVariable String authorId,
                                    @RequestParam(required = false) String name,
                                    @RequestParam(required = false) AuthorRole role) {
        try {
            Author updated = authorService.updateAuthor(authorId, name, role);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException dup) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate author");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed: " + e.getMessage());
        }
    }

    // NEW: delete one
    @DeleteMapping("/{authorId}")
    public ResponseEntity<?> delete(@PathVariable String authorId) {
        authorService.deleteAuthor(authorId);
        return ResponseEntity.ok("Deleted");
    }

    // NEW: bulk delete
    @PostMapping("/delete")
    public ResponseEntity<?> bulkDelete(@RequestBody List<String> ids) {
        authorService.deleteAuthors(ids);
        return ResponseEntity.ok("Deleted");
    }
}