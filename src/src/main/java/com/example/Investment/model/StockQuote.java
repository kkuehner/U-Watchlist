package com.example.Investment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockQuote {
    private String symbol;
    private String name;
    private double price;
    private double changePercentage;
    private long marketCap;
    private String exchange;

    public StockQuote(){};

    // ---- Getter & Setter ----

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getChangePercentage() { return changePercentage; }
    public void setChangePercentage(double changePercentage) { this.changePercentage = changePercentage; }

    public long getMarketCap() { return marketCap; }
    public void setMarketCap(long marketCap) { this.marketCap = marketCap; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

}
