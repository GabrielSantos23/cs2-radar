package com.cs2agg.api;

import com.cs2agg.api.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MatchService {
    private final DynamoDbReader dynamoDbReader;
    private final RedisReader redisReader;

    public MatchService() {
        this.dynamoDbReader = new DynamoDbReader();
        this.redisReader = new RedisReader();
    }

    public List<MatchResponse> getUpcoming() {
        List<MatchEntity> entities = dynamoDbReader.getUpcomingMatches();
        return entities.stream()
                .map(this::enrichAndMap)
                .collect(Collectors.toList());
    }

    public Optional<MatchResponse> getById(String matchId) {
        Optional<MatchEntity> entityOpt = dynamoDbReader.getMatchById(matchId);
        return entityOpt.map(this::enrichAndMap);
    }

    public List<String> getTournaments() {
        return dynamoDbReader.getTournaments();
    }

    public List<MatchResponse> getMatchesByTeam(String teamId) {
        List<MatchEntity> entities = dynamoDbReader.getMatchesByTeam(teamId);
        return entities.stream()
                .map(this::enrichAndMap)
                .collect(Collectors.toList());
    }

    private MatchResponse enrichAndMap(MatchEntity entity) {
        List<OddsResponse> odds = redisReader.getOdds(entity.getMatchId())
                .orElseGet(() -> {
                    if (entity.getOdds() == null) {
                        return new ArrayList<>();
                    }
                    return entity.getOdds().stream()
                            .map(o -> new OddsResponse(o.getBookmaker(), o.getTeam1Win(), o.getTeam2Win()))
                            .collect(Collectors.toList());
                });

        TeamResponse team1 = null;
        if (entity.getTeam1() != null) {
            team1 = new TeamResponse(
                    entity.getTeam1().getId(),
                    entity.getTeam1().getName(),
                    entity.getTeam1().getAcronym(),
                    entity.getTeam1().getImageUrl()
            );
        }

        TeamResponse team2 = null;
        if (entity.getTeam2() != null) {
            team2 = new TeamResponse(
                    entity.getTeam2().getId(),
                    entity.getTeam2().getName(),
                    entity.getTeam2().getAcronym(),
                    entity.getTeam2().getImageUrl()
            );
        }

        return new MatchResponse(
                entity.getMatchId(),
                entity.getName(),
                entity.getBeginAt(),
                entity.getTournamentId(),
                entity.getSerieName(),
                team1,
                team2,
                odds,
                entity.getPandascoreTournamentId()
        );
    }

    public void close() {
        redisReader.close();
    }
}
