package com.example.Investment.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.Investment.exception.StockNotFoundException;
import com.example.Investment.model.BalanceSheet;
import com.example.Investment.model.CashFlowStatement;
import com.example.Investment.model.EarningsEstimate;
import com.example.Investment.model.IncomeStatement;
import com.example.Investment.model.StockQuote;

// The "Entscheid" (decision) page's math: valuation estimates (Graham Number) and
// leverage-vs-earnings checks, on top of what AnalysisService already computes.
// DCF itself is deliberately NOT computed here - its assumptions (growth rate, discount
// rate, terminal growth) are meant to be adjustable sliders on the page, so the actual
// DCF formula lives in JS for instant feedback. This service only supplies the raw
// ingredients (free cash flow, share count, cash, debt) plus a sensible default growth rate.
@Service
public class DecisionService {

    private final StockApiService stockApiService;
    private final ValuationService valuationService;
    private final AnalysisService analysisService;

    public DecisionService(StockApiService stockApiService, ValuationService valuationService, AnalysisService analysisService){
        this.stockApiService = stockApiService;
        this.valuationService = valuationService;
        this.analysisService = analysisService;
    }

    public Map<String, Object> getDecisionMetricsFor(String symbol){
        StockQuote quote = stockApiService.getStockQuote(symbol);
        IncomeStatement[] history = stockApiService.getIncomeStatements(symbol);
        BalanceSheet balance = stockApiService.getBalanceSheet(symbol);
        CashFlowStatement cashFlow = stockApiService.getCashFlowStatement(symbol);

        double price = quote.getPrice();
        // See class javadoc: this is the ADR-safe stand-in for "diluted shares outstanding" -
        // marketCap and price already agree on currency and per-ADR/per-ordinary-share basis,
        // so dividing one by the other carries no unit-mismatch risk, unlike yfinance's raw
        // "Diluted Shares" field would for a stock like TSM.
        double impliedShares = quote.getMarketCap() / price;
        IncomeStatement current = history[0];

        double eps = current.getNetIncome() / impliedShares;
        double bookValuePerShare = balance.getTotalStockholdersEquity() / impliedShares;
        double grahamNumber = calcGrahamNumber(eps, bookValuePerShare);

        double debtToEarnings = calcDebtToEarnings(balance.getTotalLiabilities(), current.getNetIncome());
        double debtRatio = valuationService.calcDebtRatio(balance.getTotalLiabilities(), balance.getTotalStockholdersEquity());

        // Reuses AnalysisService's CAGR math (not its full getDetailedAnalysisFor, which
        // would re-fetch the same quote/history/estimates a second time) to pick a sensible
        // starting growth rate for the DCF slider: forward estimate if analysts cover this
        // stock, else fall back to trailing historical growth.
        double[] netIncomes = Arrays.stream(history).mapToDouble(IncomeStatement::getNetIncome).toArray();
        double cagr = analysisService.calcCagr(netIncomes);
        double cagrEstimate = getCagrEstimate(symbol);
        double suggestedGrowthRate = cagrEstimate > 0 ? cagrEstimate : cagr;

        double yearsToDouble = calcYearsToDouble(suggestedGrowthRate);

        Map<String, Object> result = new HashMap<>();
        result.put("symbol", quote.getSymbol());
        result.put("price", price);
        result.put("grahamNumber", grahamNumber);
        result.put("grahamVsPrice", grahamNumber > 0 ? (grahamNumber - price) / price : -1.);
        result.put("debtToEarnings", debtToEarnings);
        result.put("debtRatio", debtRatio);
        result.put("yearsToDouble", yearsToDouble);
        // Raw DCF ingredients - the formula itself runs client-side, see decision.html.
        result.put("freeCashFlow", cashFlow.getFreeCashFlow());
        result.put("impliedShares", impliedShares);
        result.put("cash", balance.getCash());
        result.put("totalDebt", balance.getTotalDebt());
        result.put("suggestedGrowthRate", suggestedGrowthRate);
        return result;
    }

    // Same "no analyst coverage isn't an error" handling as AnalysisService.
    private double getCagrEstimate(String symbol){
        try {
            EarningsEstimate[] estimates = stockApiService.getEarningsEstimates(symbol);
            double[] epsGrowthRates = Arrays.stream(estimates).mapToDouble(EarningsEstimate::getEpsGrowth).toArray();
            return epsGrowthRates.length > 0 ? analysisService.calcCagrFromGrowthRates(epsGrowthRates) : -1.;
        } catch (StockNotFoundException ex) {
            return -1.;
        }
    }

    // Benjamin Graham's simplified formula (the "Graham Number"): a conservative intrinsic
    // value estimate from just earnings and book value, no growth-rate or interest-rate
    // assumptions involved. Needs both inputs positive - a loss-making or negative-equity
    // company doesn't have a meaningful Graham Number.
    public double calcGrahamNumber(double eps, double bookValuePerShare){
        if (eps <= 0 || bookValuePerShare <= 0) return -1.;
        return Math.sqrt(22.5 * eps * bookValuePerShare);
    }

    // "FK/Gewinn": how many years of current earnings it would take to pay off all
    // liabilities - a leverage check relative to earnings power, not just relative to equity
    // the way the existing debt ratio (calcDebtRatio) is.
    public double calcDebtToEarnings(double totalLiabilities, double netIncome){
        if (netIncome <= 0) return -1.;
        return totalLiabilities / netIncome;
    }

    // "Years to 2x": solves (1+g)^n = 2 for n - i.e. how long compounding at rate g takes
    // to double, the precise version of the "Rule of 72" shortcut.
    public double calcYearsToDouble(double growthRate){
        if (growthRate <= 0) return -1.;
        return Math.log(2) / Math.log(1 + growthRate);
    }
}
