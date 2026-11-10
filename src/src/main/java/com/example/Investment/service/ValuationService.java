package com.example.Investment.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.Investment.exception.InsufficientDataException;
import com.example.Investment.model.BalanceSheet;
import com.example.Investment.model.CashFlowStatement;
import com.example.Investment.model.IncomeStatement;
import com.example.Investment.model.StockQuote;

@Service
public class ValuationService {

    private final StockApiService stockApiService;

    public ValuationService(StockApiService stockApiService){
        this.stockApiService = stockApiService;
    }

    // Fetches everything needed for one symbol and returns the full KPI set.
    // Shared by both TestController (single symbol) and WatchlistController (whole list).
    public Map<String, Object> getKpisFor(String symbol){
        StockQuote quote = stockApiService.getStockQuote(symbol);
        IncomeStatement[] income = stockApiService.getIncomeStatements(symbol);
        if (income.length < 2) {
            throw new InsufficientDataException(symbol);
        }
        IncomeStatement current = income[0];
        IncomeStatement previous = income[1];
        BalanceSheet balance = stockApiService.getBalanceSheet(symbol);
        CashFlowStatement cashFlow = stockApiService.getCashFlowStatement(symbol);

        double marketCap = quote.getMarketCap();
        double pe = calcPE(marketCap, current.getNetIncome());
        double ps = calcPS(marketCap, current.getRevenue());
        double peg = calcPEG(marketCap, current.getNetIncome(), previous.getNetIncome());
        double psg = calcPSG(marketCap, current.getRevenue(), previous.getRevenue());
        double pb = calcPB(marketCap, balance.getTotalStockholdersEquity());
        double roe = calcROE(current.getNetIncome(), balance.getTotalStockholdersEquity());
        double debtRatio = calcDebtRatio(balance.getTotalLiabilities(), balance.getTotalStockholdersEquity());
        double rule40 = calcRule40(current.getRevenue(), previous.getRevenue(), cashFlow.getFreeCashFlow());

        return Map.ofEntries(
            Map.entry("symbol", quote.getSymbol()),
            Map.entry("name", quote.getName()),
            Map.entry("price", quote.getPrice()),
            Map.entry("marketCap", marketCap),
            Map.entry("fiscalYear", current.getFiscalYear()),
            Map.entry("revenue", current.getRevenue()),
            Map.entry("netIncome", current.getNetIncome()),
            Map.entry("pe", pe),
            Map.entry("ps", ps),
            Map.entry("peg", peg),
            Map.entry("psg", psg),
            Map.entry("pb", pb),
            Map.entry("ROE", roe),
            Map.entry("debtRatio", debtRatio),
            Map.entry("Rule40", rule40)
        );
    }

    public double calcPE(double marketCap, double net_profit){
        if(net_profit < 0){
            return -1.; // loss - same sentinel PEG/PSG already use, P/E is meaningless here
        }
        return marketCap/net_profit;
    }

    public double calcPS(double marketCap, double revenue){
        if(revenue == 0){
            return -1.; // no revenue yet (e.g. pre-revenue biotech) - ratio is undefined
        }
        return marketCap/revenue;
    }

    //Growth KPIs

    public double calcPEG(double marketCap, double net_profit, double net_profit_lastYear){
        if(net_profit < 0){
            return -1.; // loss
        }else if(net_profit_lastYear > net_profit){
            return -2.; //negative
        }else if(net_profit_lastYear <= 0 && net_profit > 0){
            return -3.; //0.0 - growing from a zero or negative base, no meaningful growth rate
        }
        return calcPE(marketCap, net_profit) / ((net_profit / net_profit_lastYear - 1.) * 100);
    }

    public double calcPSG(double marketCap, double rev, double rev_lastYear){
        if(rev_lastYear > rev){
            return -2.; //negative
        }else if(rev_lastYear == 0){
            return -3.; // growing from zero revenue base, no meaningful growth rate
        }
        return calcPS(marketCap, rev) / ((rev / rev_lastYear - 1.) * 100);
    }

    public double calcRule40(double rev, double rev_lastYear, double freeCashFlow){
        if(rev == 0 || rev_lastYear == 0){
            return -1.; // needs revenue in both years to mean anything
        }
        return (freeCashFlow/rev + rev/rev_lastYear - 1)*100;
    }


    //Blue Chip KPIs

    public double calcPB(double marketCap, double totEquity){
        if(totEquity == 0){
            return -1.; // division by zero - ratio is undefined, not just "bad"
        }
        return marketCap/totEquity;
    }

    public double calcROE(double net_profit, double totEquity){
        if(totEquity == 0){
            return -1.;
        }
        return net_profit/totEquity;
    }

    public double calcDebtRatio(double totLiabilities, double totEquity){
        if(totEquity < 0){
            return -1.; // negative equity (e.g. heavy buybacks) makes this ratio meaningless
        }
        return totLiabilities/totEquity;
    }
}
