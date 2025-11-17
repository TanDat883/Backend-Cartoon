package flim.backendcartoon.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.CloudFrontClientBuilder;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;


@Configuration
public class AwsConfig {
//    private final String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
//    private final String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
//    private final String region = System.getenv("AWS_REGION");

    private final Dotenv dotenv = Dotenv.load();

    private final String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
    private final String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
    private final String region = dotenv.get("AWS_REGION");

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

//    @Bean
//    public SnsClient snsClient() {
//        return SnsClient.create();
//    }
    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }


    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(CloudFrontClient.class)
    public CloudFrontClient cloudFrontClient() {
        CloudFrontClientBuilder builder = CloudFrontClient.builder()
                .region(Region.AWS_GLOBAL); // hoặc Region.US_EAST_1

        if (accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            builder = builder.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder = builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}