package com.cs2agg.fetcher;

import com.cs2agg.fetcher.model.Bracket;
import com.cs2agg.fetcher.model.BracketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PublishTournament {
    public static void main(String[] args) throws Exception {
        String tournamentId = args.length > 0 ? args[0] : "21245";
        System.out.println("Starting PublishTournament for tournament ID: " + tournamentId);
        
        PandaScoreClient client = new PandaScoreClient();
        List<Bracket> brackets = client.fetchBrackets(tournamentId);
        System.out.println("Fetched " + brackets.size() + " rounds from PandaScore.");
        
        if (brackets.isEmpty()) {
            System.out.println("No brackets found for tournament ID: " + tournamentId);
            return;
        }
        
        BracketMessage msg = new BracketMessage("bracket", tournamentId, brackets);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(msg);
        
        Files.writeString(Paths.get("D:\\projects\\cs2-aggregator\\infra\\bracket_" + tournamentId + ".json"), json);
        System.out.println("Successfully wrote tournament bracket JSON to file: bracket_" + tournamentId + ".json");
    }
}
