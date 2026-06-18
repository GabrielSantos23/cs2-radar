package com.cs2agg.processor;

import com.cs2agg.processor.model.LiveMatchEntity;
import com.cs2agg.processor.model.LiveMatchEvent;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class LiveMatchRepository {
    private final DynamoDbTable<LiveMatchEntity> table;

    public LiveMatchRepository(DynamoDbEnhancedClient enhancedClient) {
        String tableName = System.getenv("DYNAMODB_TABLE");
        if (tableName == null || tableName.trim().isEmpty()) {
            tableName = "cs2-matches";
        }
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(LiveMatchEntity.class));
    }

    public void save(LiveMatchEvent event) {
        LiveMatchEntity entity = new LiveMatchEntity(event);
        table.putItem(entity);
    }
}
