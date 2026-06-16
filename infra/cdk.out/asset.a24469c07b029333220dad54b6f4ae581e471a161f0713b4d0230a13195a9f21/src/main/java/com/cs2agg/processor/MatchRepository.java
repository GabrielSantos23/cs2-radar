package com.cs2agg.processor;

import com.cs2agg.processor.model.MatchEntity;
import com.cs2agg.processor.model.MatchEvent;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class MatchRepository {
    private final DynamoDbTable<MatchEntity> table;

    public MatchRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("cs2-matches", TableSchema.fromBean(MatchEntity.class));
    }

    public void save(MatchEvent event) {
        MatchEntity entity = new MatchEntity(event);
        table.putItem(entity);
    }

    public List<MatchEvent> findUpcoming() {
        return table.scan().items().stream()
                .map(MatchEntity::toEvent)
                .collect(Collectors.toList());
    }
}
