package com.cs2agg.processor;

import com.cs2agg.processor.model.Bracket;
import com.cs2agg.processor.model.BracketMatch;
import com.cs2agg.processor.model.BracketEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class BracketRepository {
    private final DynamoDbTable<BracketEntity> table;

    public BracketRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("cs2-brackets", TableSchema.fromBean(BracketEntity.class));
    }

    public void save(String tournamentId, Bracket bracket) {
        int roundNumber = bracket.roundNumber();
        String roundName = bracket.round();
        List<BracketMatch> matches = bracket.matches();
        if (matches == null) {
            return;
        }

        for (int i = 0; i < matches.size(); i++) {
            BracketMatch match = matches.get(i);
            String roundKey = roundNumber + "#" + i;
            
            BracketEntity entity = new BracketEntity();
            entity.setTournamentId(tournamentId);
            entity.setRoundKey(roundKey);
            entity.setRoundName(roundName);
            entity.setRoundNumber(roundNumber);
            entity.setMatchIndex(i);
            entity.setMatchId(match.id());
            entity.setTeam1(match.team1());
            entity.setTeam2(match.team2());
            entity.setScore1(match.score1());
            entity.setScore2(match.score2());
            entity.setStatus(match.status());
            entity.setScheduledAt(match.scheduledAt());
            
            table.putItem(entity);
        }
    }

    public List<Bracket> findByTournament(String tournamentId) {
        List<BracketEntity> entities = table.query(QueryConditional.keyEqualTo(
                k -> k.partitionValue(tournamentId)
        )).items().stream().collect(Collectors.toList());

        Map<Integer, String> roundNames = new HashMap<>();
        Map<Integer, List<BracketEntity>> grouped = new TreeMap<>();
        for (BracketEntity entity : entities) {
            int rn = entity.getRoundNumber();
            roundNames.put(rn, entity.getRoundName());
            grouped.computeIfAbsent(rn, k -> new ArrayList<>()).add(entity);
        }

        List<Bracket> result = new ArrayList<>();
        for (Map.Entry<Integer, List<BracketEntity>> entry : grouped.entrySet()) {
            int roundNumber = entry.getKey();
            String roundName = roundNames.get(roundNumber);
            List<BracketEntity> roundEntities = entry.getValue();
            
            roundEntities.sort(Comparator.comparingInt(BracketEntity::getMatchIndex));
            
            List<BracketMatch> bracketMatches = roundEntities.stream()
                    .map(e -> new BracketMatch(
                            e.getMatchId(),
                            e.getTeam1(),
                            e.getTeam2(),
                            e.getScore1(),
                            e.getScore2(),
                            e.getStatus(),
                            e.getScheduledAt()
                    ))
                    .collect(Collectors.toList());
            
            result.add(new Bracket(roundName, roundNumber, bracketMatches));
        }
        
        return result;
    }
}
