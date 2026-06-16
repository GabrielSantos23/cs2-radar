package com.cs2agg.fetcher;

public class PandaScoreException extends RuntimeException {
    public PandaScoreException(String message) {
        super(message);
    }

    public PandaScoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
