package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Author;
import flim.backendcartoon.entities.Episode;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class AuthorRepository {
    private final DynamoDbTable<Author> table;

    public AuthorRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Author", TableSchema.fromBean(Author.class));
    }

    // Save an author to the database
    public void save(Author author) {
        System.out.println("Saving author to DynamoDB: " + author);
        table.putItem(author);
    }
    //find all author
    public Iterable<Author> findAll() {
        System.out.println("Finding all authors in DynamoDB");
        return table.scan().items();
    }

    public Author findById(String authorId) {
        System.out.println("Finding author by ID: " + authorId);
        return table.getItem(r -> r.key(k -> k.partitionValue(authorId)));
    }
}
