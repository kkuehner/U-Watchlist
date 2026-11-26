package com.example.Investment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CashFlowStatement {
    private String symbol;
    private String fiscalYear;
    private double freeCashFlow;

    public CashFlowStatement(){};

    // ---- Getter & Setter ----

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }

    public double getFreeCashFlow() { return freeCashFlow; }
    public void setFreeCashFlow(double freeCashFlow) { this.freeCashFlow = freeCashFlow; }
}
