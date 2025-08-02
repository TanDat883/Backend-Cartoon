package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.repositories.AuthorRepository;
import flim.backendcartoon.services.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthorServiceImpl implements AuthorService {
    private final AuthorRepository authorRepository;

    @Autowired
    public AuthorServiceImpl(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }


    @Override
    public void saveAuthor(Author author) {
        // Tạo ID nếu chưa có
        if (author.getAuthorId() == null || author.getAuthorId().isEmpty()) {
            author.setAuthorId(UUID.randomUUID().toString());
        }

        // Validate dữ liệu nâng cao (ví dụ: name, role)
        if (author.getName() == null || author.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Author name is required");
        }
        if (author.getAuthorRole() == null) {
            throw new IllegalArgumentException("Author role is required");
        }

        // Gọi repository để lưu vào DynamoDB
        authorRepository.save(author);
    }

    @Override
    public List<Author> findAllAuthors() {
        // Gọi repository để lấy danh sách tất cả tác giả
        return (List<Author>) authorRepository. findAll();
    }

    @Override
    public void addMovieToAuthor(String authorId, String movieId) {
        Author author = authorRepository.findById(authorId);
        if (author == null) throw new RuntimeException("Author not found");

        List<String> movieIds = author.getMovieId();
        if (!movieIds.contains(movieId)) {
            movieIds.add(movieId);
            author.setMovieId(movieIds);
            authorRepository.save(author);
        }
    }


}
