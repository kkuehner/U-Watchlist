package com.example.Investment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceSheet {
    private String symbol;
    private String fiscalYear;
    private double totalStockholdersEquity;
    private double totalLiabilities;
    private double cash;
    private double totalDebt;

    public BalanceSheet(){};

    // ---- Getter & Setter ----

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }

    public double getTotalStockholdersEquity() { return totalStockholdersEquity; }
    public void setTotalStockholdersEquity(double totalStockholdersEquity) { this.totalStockholdersEquity = totalStockholdersEquity; }

    public double getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(double totalLiabilities) { this.totalLiabilities = totalLiabilities; }

    public double getCash() { return cash; }
    public void setCash(double cash) { this.cash = cash; }

    public double getTotalDebt() { return totalDebt; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }
}
