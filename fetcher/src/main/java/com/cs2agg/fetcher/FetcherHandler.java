package com.cs2agg.fetcher;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.cs2agg.fetcher.model.Match;
import com.cs2agg.fetcher.model.LiveMatch;
import com.cs2agg.fetcher.model.TournamentResult;
import com.cs2agg.fetcher.model.Odds;
import com.cs2agg.fetcher.model.MatchOdds;
import com.cs2agg.fetcher.model.OddsFixture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

public class FetcherHandler implements RequestHandler<ScheduledEvent, Void> {
    private final PandaScoreClient client;
    private final SqsPublisher publisher;

    public FetcherHandler() {
        this.client = new PandaScoreClient();
        this.publisher = new SqsPublisher();
    }

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        System.out.println("Fetcher Lambda invoked. Event ID: " + event.getId() + ", Time: " + event.getTime());
        try {
            try {
                System.out.println("Fetching live matches from PandaScore...");
                List<LiveMatch> liveMatches = client.fetchLiveMatches();
                System.out.println("Fetched " + liveMatches.size() + " live matches from PandaScore.");
                publisher.publishLiveMatches(liveMatches);
            } catch (Exception e) {
                System.err.println("Failed to fetch/publish live matches: " + e.getMessage());
            }

            List<Match> matches = client.fetchUpcomingMatches();
            System.out.println("Fetched " + matches.size() + " matches from PandaScore.");

            // Busca odds apenas nas invocações das horas cheias (00, 06, 12, 18 UTC)
            // ou se a variável de ambiente FORCE_FETCH_ODDS estiver configurada como true para testes
            int currentHour = ZonedDateTime.now(ZoneOffset.UTC).getHour();
            boolean shouldFetchOdds = (currentHour % 6 == 0) || "true".equalsIgnoreCase(System.getenv("FORCE_FETCH_ODDS"));

            List<Match> processedMatches = matches;
            if (shouldFetchOdds) {
                try {
                    System.out.println("Fetching CS2 fixtures from OddsPapi (Hour: " + currentHour + " UTC)...");
                    OddsPapiClient oddsClient = new OddsPapiClient();
                    List<OddsFixture> fixtures = oddsClient.fetchCS2Fixtures();
                    System.out.println("Fetched " + fixtures.size() + " fixtures from OddsPapi.");

                    if (!fixtures.isEmpty()) {
                        List<MatchOdds> allOdds = new ArrayList<>();
                        // Para cada partida da PandaScore, procura se há um fixture correspondente no OddsPapi
                        for (Match m : matches) {
                            Optional<OddsFixture> matchingFixtureOpt = findMatchingFixture(m, fixtures);
                            if (matchingFixtureOpt.isPresent()) {
                                OddsFixture mf = matchingFixtureOpt.get();
                                if (mf.hasOdds()) {
                                    System.out.println("Fetching odds for matched fixture: " + mf.fixtureId() + " (" + mf.participant1Name() + " vs " + mf.participant2Name() + ")");
                                    MatchOdds mo = oddsClient.fetchOddsForFixture(mf);
                                    if (mo != null) {
                                        allOdds.add(mo);
                                    }
                                    try {
                                        Thread.sleep(250);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            }
                        }

                        if (!allOdds.isEmpty()) {
                            processedMatches = matches.stream()
                                    .map(m -> enrichMatchOdds(m, allOdds))
                                    .toList();
                            long enrichedCount = processedMatches.stream()
                                    .filter(m -> m.odds() != null && !m.odds().isEmpty())
                                    .count();
                            System.out.println("Enriched " + enrichedCount + " matches with real odds.");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch/process odds from OddsPapi: " + e.getMessage());
                }
            } else {
                System.out.println("Skipping OddsPapi fetch (Hour: " + currentHour + " UTC, not divisible by 6).");
            }
            
            publisher.publish(processedMatches);
            System.out.println("Published matches successfully to SQS.");

            // Passo 1: Extrair torneios ativos e coletar brackets
            List<String> tournamentIds = processedMatches.stream()
                    .map(Match::tournamentId)
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .distinct()
                    .toList();

            System.out.println("Found " + tournamentIds.size() + " unique active tournaments. Fetching brackets...");
            for (String tournamentId : tournamentIds) {
                try {
                    List<com.cs2agg.fetcher.model.Bracket> brackets = client.fetchBrackets(tournamentId);
                    if (!brackets.isEmpty()) {
                        publisher.publishBracket(tournamentId, brackets);
                    } else {
                        System.out.println("No brackets found for tournament: " + tournamentId);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch/publish brackets for tournament ID " + tournamentId + ": " + e.getMessage());
                }
            }

            try {
                System.out.println("Fetching recent tournament results for ranking calculation...");
                List<TournamentResult> recentTournaments = client.fetchRecentTournamentResults();
                System.out.println("Fetched " + recentTournaments.size() + " tournament results. Publishing to SQS...");
                publisher.publishTournamentResults(recentTournaments);
            } catch (Exception e) {
                System.err.println("Failed to fetch/publish recent tournament results: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error in FetcherHandler: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return null;
    }

    private Match enrichMatchOdds(Match pandaMatch, List<MatchOdds> allOdds) {
        Optional<MatchOdds> matchingOddsOpt = findMatchingOdds(pandaMatch, allOdds);
        if (matchingOddsOpt.isEmpty()) {
            return pandaMatch;
        }
        
        MatchOdds matchingOdds = matchingOddsOpt.get();
        boolean isReverse = false;
        
        if (pandaMatch.team1() != null && pandaMatch.team1().name() != null &&
            pandaMatch.team2() != null && pandaMatch.team2().name() != null &&
            matchingOdds.team1Name() != null && matchingOdds.team2Name() != null) {
            
            String pTeam1 = normalize(pandaMatch.team1().name());
            String oTeam2 = normalize(matchingOdds.team2Name());
            
            if (oTeam2.contains(pTeam1) || pTeam1.contains(oTeam2)) {
                isReverse = true;
            }
        }
        
        final boolean reverse = isReverse;
        List<Odds> oddsList = matchingOdds.bookmakers().stream()
            .map(bo -> {
                double t1Win = reverse ? bo.team2Win() : bo.team1Win();
                double t2Win = reverse ? bo.team1Win() : bo.team2Win();
                return new Odds(bo.bookmaker(), t1Win, t2Win);
            })
            .toList();
            
        return new Match(
            pandaMatch.id(),
            pandaMatch.name(),
            pandaMatch.beginAt(),
            pandaMatch.tournamentName(),
            pandaMatch.serieName(),
            pandaMatch.team1(),
            pandaMatch.team2(),
            oddsList,
            pandaMatch.tournamentId(),
            pandaMatch.tournamentImageUrl(),
            pandaMatch.tournamentTier()
        );
    }

    private Optional<OddsFixture> findMatchingFixture(Match pandaMatch, List<OddsFixture> allFixtures) {
        if (pandaMatch.team1() == null || pandaMatch.team1().name() == null ||
            pandaMatch.team2() == null || pandaMatch.team2().name() == null) {
            return Optional.empty();
        }
        String pTeam1 = normalize(pandaMatch.team1().name());
        String pTeam2 = normalize(pandaMatch.team2().name());
        
        if (pTeam1.isEmpty() || pTeam2.isEmpty()) {
            return Optional.empty();
        }

        return allFixtures.stream()
            .filter(fixture -> {
                if (fixture.participant1Name() == null || fixture.participant2Name() == null) {
                    return false;
                }
                String oTeam1 = normalize(fixture.participant1Name());
                String oTeam2 = normalize(fixture.participant2Name());

                boolean directTeam1Match = oTeam1.contains(pTeam1) || pTeam1.contains(oTeam1);
                boolean directTeam2Match = oTeam2.contains(pTeam2) || pTeam2.contains(oTeam2);
                boolean directMatch = directTeam1Match && directTeam2Match;

                boolean reverseTeam1Match = oTeam1.contains(pTeam2) || pTeam2.contains(oTeam1);
                boolean reverseTeam2Match = oTeam2.contains(pTeam1) || pTeam1.contains(oTeam2);
                boolean reverseMatch = reverseTeam1Match && reverseTeam2Match;

                boolean timeMatch = Math.abs(
                    parseEpoch(fixture.startTime()) - parseEpoch(pandaMatch.beginAt())
                ) < 30 * 60; // 30 minutos em segundos

                return (directMatch || reverseMatch) && timeMatch;
            })
            .findFirst();
    }

    private Optional<MatchOdds> findMatchingOdds(Match pandaMatch, List<MatchOdds> allOdds) {
        if (pandaMatch.team1() == null || pandaMatch.team1().name() == null ||
            pandaMatch.team2() == null || pandaMatch.team2().name() == null) {
            return Optional.empty();
        }
        String pTeam1 = normalize(pandaMatch.team1().name());
        String pTeam2 = normalize(pandaMatch.team2().name());
        
        if (pTeam1.isEmpty() || pTeam2.isEmpty()) {
            return Optional.empty();
        }

        return allOdds.stream()
            .filter(odds -> {
                if (odds.team1Name() == null || odds.team2Name() == null) {
                    return false;
                }
                String oTeam1 = normalize(odds.team1Name());
                String oTeam2 = normalize(odds.team2Name());

                boolean directTeam1Match = oTeam1.contains(pTeam1) || pTeam1.contains(oTeam1);
                boolean directTeam2Match = oTeam2.contains(pTeam2) || pTeam2.contains(oTeam2);
                boolean directMatch = directTeam1Match && directTeam2Match;

                boolean reverseTeam1Match = oTeam1.contains(pTeam2) || pTeam2.contains(oTeam1);
                boolean reverseTeam2Match = oTeam2.contains(pTeam1) || pTeam1.contains(oTeam2);
                boolean reverseMatch = reverseTeam1Match && reverseTeam2Match;

                boolean timeMatch = Math.abs(
                    parseEpoch(odds.commenceTime()) - parseEpoch(pandaMatch.beginAt())
                ) < 30 * 60; // 30 minutos em segundos

                return (directMatch || reverseMatch) && timeMatch;
            })
            .findFirst();
    }

    private String normalize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private long parseEpoch(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return 0L;
        }
        try {
            return java.time.Instant.parse(timeStr).getEpochSecond();
        } catch (Exception e) {
            try {
                return java.time.ZonedDateTime.parse(timeStr).toEpochSecond();
            } catch (Exception e2) {
                try {
                    return java.time.OffsetDateTime.parse(timeStr).toEpochSecond();
                } catch (Exception e3) {
                    System.err.println("Failed to parse time string: " + timeStr + ". Error: " + e3.getMessage());
                    return 0L;
                }
            }
        }
    }
}
