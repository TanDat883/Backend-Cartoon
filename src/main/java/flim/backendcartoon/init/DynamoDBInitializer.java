package flim.backendcartoon.init;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
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
        createTableIfNotExists(SubscriptionPackage.class, "SubscriptionPackage");
        createTableIfNotExists(VipSubscription.class, "VipSubscription");
        createTableIfNotExists(Order.class, "Order");
        createTableIfNotExists(PaymentOrder.class, "PaymentOrder");
        createTableIfNotExists(Promotion.class, "Promotion");
        createTableIfNotExists(Feedback.class, "Feedback");
        createTableIfNotExists(Wishlist.class, "Wishlist");
        createTableIfNotExists(Author.class, "Author");
        createTableIfNotExists(MovieRating.class, "MovieRating");
        createTableIfNotExists(Season.class, "Season");


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