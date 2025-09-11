package flim.backendcartoon.services;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.entities.AuthorRole;
import flim.backendcartoon.exception.AuthorException;

import java.util.List;

public interface AuthorService {
    void saveAuthor(Author author);

    List<Author> findAllAuthors();

    void addMovieToAuthor(List<String> authorIds, String movieId);

    List<Author> findAuthorsByMovieId(String movieId) throws AuthorException;

    // NEW
    Author updateAuthor(String authorId, String name, AuthorRole role);

    void deleteAuthor(String authorId);

    void deleteAuthors(List<String> ids);

    Author findByNameAndRole(String name, AuthorRole role);
}
