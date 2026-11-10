package com.example.Investment.exception;

public class InsufficientDataException extends RuntimeException {
    public InsufficientDataException(String symbol) {
        super("Not enough historical data for " + symbol + " to calculate growth KPIs (need at least 2 fiscal years)");
    }
}
