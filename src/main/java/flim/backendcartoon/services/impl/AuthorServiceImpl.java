package flim.backendcartoon.services.impl;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.entities.AuthorRole;
import flim.backendcartoon.exception.AuthorException;
import flim.backendcartoon.exception.ResourceNotFoundException;
import flim.backendcartoon.repositories.AuthorRepository;
import flim.backendcartoon.repositories.MovieRepository;
import flim.backendcartoon.services.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthorServiceImpl implements AuthorService {
    private final AuthorRepository authorRepository;
    private final MovieRepository movieRepository;

    @Autowired
    public AuthorServiceImpl(AuthorRepository authorRepository, MovieRepository movieRepository) {
        this.authorRepository = authorRepository;
        this.movieRepository = movieRepository;
    }

    //check trùng
    private static String normalize(String s) {
        if (s == null) return "";
        String noDiac = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noDiac.toLowerCase().trim().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ");
    }
    private Author findDuplicate(String name, AuthorRole role) {
        String key = normalize(name);
        return authorRepository.findAll().stream()
                .filter(a -> role == a.getAuthorRole())
                .filter(a -> key.equals(a.getNormalizedName()))
                .findFirst().orElse(null);
    }
    @Override
    public void saveAuthor(Author author) {
        if (author.getName() == null || author.getName().isBlank()) throw new IllegalArgumentException("Author name is required");
        if (author.getAuthorRole() == null) throw new IllegalArgumentException("Author role is required");

        // set id nếu thiếu
        if (author.getAuthorId() == null || author.getAuthorId().isBlank()) {
            author.setAuthorId(java.util.UUID.randomUUID().toString());
        }
        author.setNormalizedName(normalize(author.getName()));

        // check trùng name+role
        Author dup = findDuplicate(author.getName(), author.getAuthorRole());
        if (dup != null) {
            // thay vì 409, ta trả về “đối tượng đã tồn tại” để FE dễ dùng
            // => caller có thể dùng dup.getAuthorId()
            // Nếu muốn 409 thì throw IllegalStateException("DUPLICATE")
            author.setAuthorId(dup.getAuthorId());
            author.setName(dup.getName());
            author.setAuthorRole(dup.getAuthorRole());
            author.setMovieId(dup.getMovieId());
            author.setNormalizedName(dup.getNormalizedName());
            return; // coi như idempotent
        }
        if (author.getMovieId() == null) author.setMovieId(new java.util.ArrayList<>());
        authorRepository.save(author);
    }

    @Override
    public List<Author> findAllAuthors() { return authorRepository.findAll(); }

    @Override
    public void addMovieToAuthor(List<String> authorIds, String movieId) {
        if (authorIds == null || authorIds.isEmpty() || movieId == null || movieId.isBlank()) return;
        for (String authorId : new java.util.HashSet<>(authorIds)) { // unique
            Author author = authorRepository.findById(authorId);
            if (author == null) continue;
            List<String> movieIds = author.getMovieId();
            if (movieIds == null) movieIds = new java.util.ArrayList<>();
            if (!movieIds.contains(movieId)) {
                movieIds.add(movieId);
                author.setMovieId(movieIds);
                authorRepository.save(author);
            }
        }
    }

    @Override
    public List<Author> findAuthorsByMovieId(String movieId) throws AuthorException {
        if (movieId == null || movieId.isBlank()) throw new AuthorException("Movie ID is required");
        return authorRepository.findAll().stream()
                .filter(a -> a.getMovieId() != null && a.getMovieId().contains(movieId))
                .collect(Collectors.toList());
    }

    // NEW
    @Override
    public Author updateAuthor(String authorId, String name, AuthorRole role) {
        Author a = authorRepository.findById(authorId);
        if (a == null) throw new IllegalArgumentException("Author not found");
        if (name != null && !name.isBlank()) a.setName(name);
        if (role != null) a.setAuthorRole(role);

        // re-check duplicate nếu đổi name/role
        Author dup = findDuplicate(a.getName(), a.getAuthorRole());
        if (dup != null && !dup.getAuthorId().equals(a.getAuthorId())) {
            throw new IllegalStateException("DUPLICATE_NAME_ROLE");
        }
        a.setNormalizedName(normalize(a.getName()));
        authorRepository.save(a);
        return a;
    }

    @Override
    public void deleteAuthor(String authorId) {
        Author a = authorRepository.findById(authorId);
        if (a != null && a.getMovieId() != null) {
            var movies = movieRepository.findAllMovies();
            for (var m : movies) {
                var ids = m.getAuthorIds();
                if (ids != null && ids.remove(authorId)) {
                    m.setAuthorIds(ids);
                    movieRepository.update(m);
                }
            }
        }
        authorRepository.deleteById(authorId);
    }

    @Override
    public void deleteAuthors(List<String> ids) {
        if (ids == null) return;
        for (String id : ids)  deleteAuthor(id);
    }

    @Override
    public Author findByNameAndRole(String name, AuthorRole role) {
        return findDuplicate(name, role);
    }


    @Override
    public void setAuthorsForMovie(String movieId, List<String> authorIds) {
        var all = authorRepository.findAll();
        var want = new java.util.HashSet<>(authorIds == null ? List.<String>of() : authorIds);

        for (Author a : all) {
            var list = a.getMovieId();
            if (list == null) list = new java.util.ArrayList<>();
            boolean has = list.contains(movieId);
            boolean should = want.contains(a.getAuthorId());

            if (should && !has) {
                list.add(movieId);
                a.setMovieId(list);
                authorRepository.save(a);
            } else if (!should && has) {
                list.remove(movieId);
                a.setMovieId(list);
                authorRepository.save(a);
            }
        }
    }

}
