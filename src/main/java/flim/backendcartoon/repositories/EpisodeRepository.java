package flim.backendcartoon.repositories;

import flim.backendcartoon.entities.Episode;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class EpisodeRepository {
    private final DynamoDbTable<Episode> table;

    public EpisodeRepository(DynamoDbEnhancedClient client) {
        this.table = client.table("Episode", TableSchema.fromBean(Episode.class));
    }

    public void save(Episode episode) {
        table.putItem(episode);
    }

    public List<Episode> findByMovieId(String movieId) {
        return table.scan()
                .items()
                .stream()
                .filter(e -> e.getMovieId().equals(movieId))
                .sorted((a, b) -> a.getEpisodeNumber() - b.getEpisodeNumber())
                .collect(Collectors.toList());
    }


}
