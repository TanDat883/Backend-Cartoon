package flim.backendcartoon.init;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import flim.backendcartoon.entities.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;

@Component
public class DynamoDBInitializer implements CommandLineRunner {
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;

    public DynamoDBInitializer(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
    }
    @Override
    public void run(String... args) {

        createTableIfNotExists(User.class, "User");
        createTableIfNotExists(Movie.class, "Movie");
        createTableIfNotExists(Episode.class, "Episode");
        createTableIfNotExists(Price.class, "Price");
        createTableIfNotExists(PaymentOrder.class, "PaymentOrder");
    }

    private <T> void createTableIfNotExists(Class<T> clazz, String tableName) {
        boolean tableExists = dynamoDbClient
                .listTables(ListTablesRequest.builder().build())
                .tableNames()
                .contains(tableName);

        if (tableExists) {
            System.out.println("✅ Table '" + tableName + "' already exists.");
            return;
        }

        try {
            enhancedClient.table(tableName, TableSchema.fromBean(clazz))
                    .createTable(CreateTableEnhancedRequest.builder().build());
            System.out.println("✅ Table '" + tableName + "' created.");
        } catch (Exception e) {
            System.err.println("❌ Failed to create table '" + tableName + "': " + e.getMessage());
        }
    }
}