package flim.backendcartoon.services;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.exception.AuthorException;

import java.util.List;

public interface AuthorService {
    //lưu author
    void saveAuthor(Author author);
    //find all
    List<Author> findAllAuthors();

    //trường hợp đã có authorId thì sẽ update
    void addMovieToAuthor(List<String> authorIds, String movieId);

    //tìm author trong bộ phim
    List<Author> findAuthorsByMovieId(String movieId) throws AuthorException;
}
