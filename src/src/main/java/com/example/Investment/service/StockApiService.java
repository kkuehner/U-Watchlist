package com.example.Investment.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.Investment.exception.StockNotFoundException;
import com.example.Investment.model.BalanceSheet;
import com.example.Investment.model.CashFlowStatement;
import com.example.Investment.model.EarningsEstimate;
import com.example.Investment.model.IncomeStatement;
import com.example.Investment.model.SearchResult;
import com.example.Investment.model.StockQuote;

@Service
public class StockApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    // FMP returns an empty array (not an error) for an unknown symbol - this
    // turns that into a clear exception instead of a raw ArrayIndexOutOfBoundsException.
    private <T> T[] requireNonEmpty(T[] results, String symbol){
        if (results == null || results.length == 0) {
            throw new StockNotFoundException(symbol);
        }
        return results;
    }

    @Value("${fmp.api.key}")
    private String apiKey;

    @Value("${yfinance.sidecar.url}")
    private String sidecarUrl;

    public String getStockData(String symbol){
        String url = "https://financialmodelingprep.com/stable/search-symbol?query=" + symbol + "&apikey=" + apiKey;
        return restTemplate.getForObject(url, String.class);
    }

    public StockQuote getStockQuote(String symbol){
        StockQuote[] quote = restTemplate.getForObject(sidecarUrl + "/quote/" + symbol, StockQuote[].class);
        return requireNonEmpty(quote, symbol)[0];
    }

    // Returns every fiscal year the sidecar has (newest first, usually ~4) - callers that
    // only need current + prior year (like ValuationService) just read index 0 and 1.
    public IncomeStatement[] getIncomeStatements(String symbol){
        IncomeStatement[] statements = restTemplate.getForObject(sidecarUrl + "/income-statement/" + symbol, IncomeStatement[].class);
        return requireNonEmpty(statements, symbol);
    }

    // Returns up to 2 entries: current fiscal year estimate, next fiscal year estimate.
    public EarningsEstimate[] getEarningsEstimates(String symbol){
        EarningsEstimate[] estimates = restTemplate.getForObject(sidecarUrl + "/estimates/" + symbol, EarningsEstimate[].class);
        return requireNonEmpty(estimates, symbol);
    }

    public BalanceSheet getBalanceSheet(String symbol){
        BalanceSheet[] balanceSheets = restTemplate.getForObject(sidecarUrl + "/balance-sheet/" + symbol, BalanceSheet[].class);
        return requireNonEmpty(balanceSheets, symbol)[0];
    }

    public CashFlowStatement getCashFlowStatement(String symbol){
        CashFlowStatement[] cashFlows = restTemplate.getForObject(sidecarUrl + "/cash-flow/" + symbol, CashFlowStatement[].class);
        return requireNonEmpty(cashFlows, symbol)[0];
    }

    // Used for the add-symbol autocomplete - query may contain spaces, so it needs URL-encoding
    // (a raw ticker symbol never does, which is why the other methods above skip this step).
    public SearchResult[] searchSymbols(String query){
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        SearchResult[] results = restTemplate.getForObject(sidecarUrl + "/search/" + encoded, SearchResult[].class);
        return results == null ? new SearchResult[0] : results;
    }

}
