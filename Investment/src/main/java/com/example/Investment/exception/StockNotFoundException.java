package com.example.Investment.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(String symbol) {
        super("No data found for symbol: " + symbol);
    }
}
