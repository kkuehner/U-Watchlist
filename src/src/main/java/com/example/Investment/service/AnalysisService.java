package com.example.Investment.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.Investment.exception.StockNotFoundException;
import com.example.Investment.model.CashFlowStatement;
import com.example.Investment.model.EarningsEstimate;
import com.example.Investment.model.IncomeStatement;
import com.example.Investment.model.StockQuote;

// The "Genaue Analyse" (detailed analysis) page's math: multi-year growth (CAGR)
// instead of the single-year ratios ValuationService computes for the watchlist.
@Service
public class AnalysisService {

    private final StockApiService stockApiService;
    private final ValuationService valuationService;

    public AnalysisService(StockApiService stockApiService, ValuationService valuationService){
        this.stockApiService = stockApiService;
        this.valuationService = valuationService;
    }

    public Map<String, Object> getDetailedAnalysisFor(String symbol){
        StockQuote quote = stockApiService.getStockQuote(symbol);
        // Newest fiscal year first - e.g. [2025, 2024, 2023, 2022] for a company with 4 years on file.
        IncomeStatement[] history = stockApiService.getIncomeStatements(symbol);
        // Smaller/less-followed stocks often have no analyst coverage at all - that's a
        // real, common case (not an error), so it degrades to "no forward estimate"
        // instead of failing the whole page the way a genuinely missing symbol should.
        EarningsEstimate[] estimates;
        try {
            estimates = stockApiService.getEarningsEstimates(symbol);
        } catch (StockNotFoundException ex) {
            estimates = new EarningsEstimate[0];
        }
        CashFlowStatement cashFlow = stockApiService.getCashFlowStatement(symbol);

        double marketCap = quote.getMarketCap();
        IncomeStatement current = history[0];
        IncomeStatement previous = history.length > 1 ? history[1] : null;

        double pe = valuationService.calcPE(marketCap, current.getNetIncome());
        double rule40 = previous != null
            ? valuationService.calcRule40(current.getRevenue(), previous.getRevenue(), cashFlow.getFreeCashFlow())
            : -1.;

        double[] netIncomes = Arrays.stream(history).mapToDouble(IncomeStatement::getNetIncome).toArray();
        double[] revenues = Arrays.stream(history).mapToDouble(IncomeStatement::getRevenue).toArray();

        // Historical growth: based on net income, same basis as the rest of the app.
        double cagr = calcCagr(netIncomes);
        double revenueCagr = calcCagr(revenues);
        double avgProfitMargin = calcAvgProfitMargin(firstN(netIncomes, 3), firstN(revenues, 3));
        double pegCagr = calcPegFromCagr(pe, cagr);

        // Forward growth: based on Yahoo's own EPS growth rates (see EarningsEstimate) -
        // a different basis than the historical CAGR above, deliberately, to stay
        // correct for ADRs. Both are clearly labeled so they're never confused.
        double[] epsGrowthRates = Arrays.stream(estimates).mapToDouble(EarningsEstimate::getEpsGrowth).toArray();
        double cagrEstimate = epsGrowthRates.length > 0 ? calcCagrFromGrowthRates(epsGrowthRates) : -1.;
        double pegEstimate = calcPegFromCagr(pe, cagrEstimate);

        Map<String, Object> result = new HashMap<>();
        result.put("symbol", quote.getSymbol());
        result.put("name", quote.getName());
        result.put("marketCap", marketCap);
        result.put("pe", pe);
        result.put("rule40", rule40);
        result.put("cagr", cagr);
        result.put("revenueCagr", revenueCagr);
        result.put("avgProfitMargin", avgProfitMargin);
        result.put("pegCagr", pegCagr);
        result.put("cagrEstimate", cagrEstimate);
        result.put("pegEstimate", pegEstimate);
        result.put("history", history);       // full multi-year data, for the page to show as columns
        result.put("estimates", estimates);
        return result;
    }

    private double[] firstN(double[] values, int n){
        return Arrays.copyOf(values, Math.min(n, values.length));
    }

    // Compound Annual Growth Rate, using the longest run of consecutive positive years
    // counting back from the newest one (matching IncomeStatement[]'s newest-first order).
    // A loss anywhere further back than that just shortens the usable span instead of
    // wiping the CAGR out entirely - CAGR only needs its two endpoints to be positive,
    // it doesn't care what happened in between. Example: [2025: +, 2024: +, 2023: -, 2022: +]
    // stops at 2023 and computes CAGR from 2024 to 2025 only, ignoring 2022 even though
    // it was profitable too, because it's on the far side of a gap.
    public double calcCagr(double[] valuesNewestFirst){
        int span = countCagrSpan(valuesNewestFirst);
        if (span < 2) return -1.; // need at least 2 points to measure growth between them

        double newest = valuesNewestFirst[0];
        double oldest = valuesNewestFirst[span - 1];
        int years = span - 1;
        return Math.pow(newest / oldest, 1.0 / years) - 1.0;
    }

    // How many consecutive positive years calcCagr actually used, counting back from the
    // newest. Exposed separately so callers can tell a CAGR based on a long, stable run
    // apart from one based on just a single year-over-year comparison (span 2) - the latter
    // is mathematically correct but easy to distort with a "base effect" (e.g. profit jumping
    // 5x off a tiny prior-year base), worth flagging rather than silently trusting for
    // multi-year projections. See DecisionService's use of this for the DCF/years-to-double.
    public int countCagrSpan(double[] valuesNewestFirst){
        int n = valuesNewestFirst.length;
        if (n == 0 || valuesNewestFirst[0] <= 0) return 0;
        int span = 1; // valuesNewestFirst[0] is already known positive
        while (span < n && valuesNewestFirst[span] > 0){
            span++;
        }
        return span;
    }

    // Same idea as calcCagr, but built by compounding individual year-over-year growth
    // rates instead of dividing raw start/end values - what we use for the forward
    // estimate, since only the growth *rate* is safe to trust across currencies/ADRs.
    //
    // A growth rate below -100% (g < -1) means analysts expect that year's EPS to swing
    // to a loss - "1 + g" goes negative, and compounding through a negative multiplier
    // makes the running product negative too. Math.pow() of a negative number to a
    // fractional power (e.g. the square root, for 2 years of estimates) has no real
    // result and silently returns NaN, which Jackson then ships to the browser as the
    // *string* "NaN" - exactly the same failure shape as the earlier P/S "Infinity" bug.
    // Guarded here instead of relying on the frontend's safeFixed() backstop to catch it.
    public double calcCagrFromGrowthRates(double[] yearOverYearGrowthRates){
        double compounded = 1.0;
        for (double g : yearOverYearGrowthRates){
            compounded *= (1.0 + g);
        }
        if (compounded <= 0) return -1.; // swings to a loss somewhere in the estimate window
        return Math.pow(compounded, 1.0 / yearOverYearGrowthRates.length) - 1.0;
    }

    // Same PEG formula as ValuationService.calcPEG (P/E divided by growth expressed in
    // percentage points), just fed a CAGR instead of a single year's growth rate.
    public double calcPegFromCagr(double pe, double cagr){
        if (pe <= 0 || cagr <= 0) return -1.;
        return pe / (cagr * 100);
    }

    // Average net margin (net income / revenue) over the given years, skipping any
    // year with zero-or-negative revenue (margin is undefined there, not just bad).
    public double calcAvgProfitMargin(double[] netIncomes, double[] revenues){
        int n = Math.min(netIncomes.length, revenues.length);
        double sum = 0;
        int counted = 0;
        for (int i = 0; i < n; i++){
            if (revenues[i] > 0){
                sum += netIncomes[i] / revenues[i];
                counted++;
            }
        }
        return counted == 0 ? -1. : sum / counted;
    }
}
