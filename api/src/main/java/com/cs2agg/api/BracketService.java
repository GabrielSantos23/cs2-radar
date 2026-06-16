package com.cs2agg.api;

import com.cs2agg.api.model.BracketResponse;
import com.cs2agg.api.model.BracketEntity;
import com.cs2agg.api.model.TeamResponse;

import java.util.*;
import java.util.stream.Collectors;

public class BracketService {
    private final DynamoDbReader reader;

    public BracketService(DynamoDbReader reader) {
        this.reader = reader;
    }

    public Optional<BracketResponse> getBracketByTournament(String tournamentId) {
        List<BracketEntity> entities = reader.getBracketsByTournament(tournamentId);
        if (entities == null || entities.isEmpty()) {
            return Optional.empty();
        }

        Map<Integer, String> roundNames = new HashMap<>();
        Map<Integer, List<BracketEntity>> grouped = new TreeMap<>();
        for (BracketEntity entity : entities) {
            int rn = entity.getRoundNumber();
            roundNames.put(rn, entity.getRoundName());
            grouped.computeIfAbsent(rn, k -> new ArrayList<>()).add(entity);
        }

        List<BracketResponse.RoundResponse> rounds = new ArrayList<>();
        for (Map.Entry<Integer, List<BracketEntity>> entry : grouped.entrySet()) {
            int roundNumber = entry.getKey();
            String roundName = roundNames.get(roundNumber);
            List<BracketEntity> roundEntities = entry.getValue();

            roundEntities.sort(Comparator.comparingInt(BracketEntity::getMatchIndex));

            List<BracketResponse.MatchResponse> matches = roundEntities.stream()
                    .map(e -> {
                        TeamResponse team1 = null;
                        if (e.getTeam1() != null) {
                            team1 = new TeamResponse(
                                    e.getTeam1().getId(),
                                    e.getTeam1().getName(),
                                    e.getTeam1().getAcronym(),
                                    e.getTeam1().getImageUrl()
                            );
                        }
                        TeamResponse team2 = null;
                        if (e.getTeam2() != null) {
                            team2 = new TeamResponse(
                                    e.getTeam2().getId(),
                                    e.getTeam2().getName(),
                                    e.getTeam2().getAcronym(),
                                    e.getTeam2().getImageUrl()
                            );
                        }
                        return new BracketResponse.MatchResponse(
                                e.getMatchId(),
                                team1,
                                team2,
                                e.getScore1(),
                                e.getScore2(),
                                e.getStatus(),
                                e.getScheduledAt()
                        );
                    })
                    .collect(Collectors.toList());

            rounds.add(new BracketResponse.RoundResponse(roundName, roundNumber, matches));
        }

        return Optional.of(new BracketResponse(tournamentId, rounds));
    }
}
