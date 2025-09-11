package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.entities.Episode;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AuthorRepository {
    private final DynamoDbTable<Author> table;

    public AuthorRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Author", TableSchema.fromBean(Author.class));
    }

    public void save(Author author) { table.putItem(author); } // upsert
    public List<Author> findAll() { return table.scan().items().stream().collect(Collectors.toList()); }
    public Author findById(String authorId) { return table.getItem(r -> r.key(k -> k.partitionValue(authorId))); }

    public void deleteById(String authorId) {
        Author key = new Author();
        key.setAuthorId(authorId);
        table.deleteItem(key);
    }
}
