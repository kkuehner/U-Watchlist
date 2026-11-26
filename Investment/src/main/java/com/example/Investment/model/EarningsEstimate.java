package com.example.Investment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Mirrors the /estimates/<symbol> sidecar endpoint. epsGrowth/revenueGrowth are what
// the CAGR calculation actually uses (see AnalysisService) - epsEstimate/revenueEstimate
// are kept for display only, since they're not always on a directly comparable basis
// across companies (currency, ADR share ratios, ...).
@JsonIgnoreProperties(ignoreUnknown = true)
public class EarningsEstimate {
    private String symbol;
    private String fiscalYear;
    private double epsEstimate;
    private double epsGrowth;
    private double revenueEstimate;
    private double revenueGrowth;
    private int analystCount;

    public EarningsEstimate(){};

    // ---- Getter & Setter ----

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }

    public double getEpsEstimate() { return epsEstimate; }
    public void setEpsEstimate(double epsEstimate) { this.epsEstimate = epsEstimate; }

    public double getEpsGrowth() { return epsGrowth; }
    public void setEpsGrowth(double epsGrowth) { this.epsGrowth = epsGrowth; }

    public double getRevenueEstimate() { return revenueEstimate; }
    public void setRevenueEstimate(double revenueEstimate) { this.revenueEstimate = revenueEstimate; }

    public double getRevenueGrowth() { return revenueGrowth; }
    public void setRevenueGrowth(double revenueGrowth) { this.revenueGrowth = revenueGrowth; }

    public int getAnalystCount() { return analystCount; }
    public void setAnalystCount(int analystCount) { this.analystCount = analystCount; }
}
