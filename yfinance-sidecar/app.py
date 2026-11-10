from flask import Flask, jsonify
import yfinance as yf
import math
import time

app = Flask(__name__)

CACHE_TTL_SECONDS = 900  # fundamentals barely change within 15 minutes
_cache = {}


@app.route("/health")
def health():
    # No Yahoo call here on purpose - this only answers "is the sidecar process up",
    # not "is Yahoo reachable", so start.ps1 can check it quickly and cheaply.
    return jsonify({"status": "ok"})


def clean(value):
    """yfinance uses NaN for missing data; JSON has no NaN, so turn it into None."""
    if value is None or (isinstance(value, float) and math.isnan(value)):
        return None
    return value


def cached(key, fetch_fn):
    """Returns the cached value for `key` if it's younger than the TTL, else
    calls fetch_fn(), caches the result, and returns that instead."""
    entry = _cache.get(key)
    if entry is not None and time.time() - entry[0] < CACHE_TTL_SECONDS:
        return entry[1]
    value = fetch_fn()
    _cache[key] = (time.time(), value)
    return value


def get_quote_currency(symbol):
    """The currency the stock's price/marketCap are quoted in (e.g. USD for TSM's ADR,
    even though the underlying company reports financials in TWD)."""
    def fetch():
        return yf.Ticker(symbol).info.get("currency")
    return cached(("quote-currency", symbol), fetch)


def get_fx_rate(from_currency, to_currency):
    """Multiplier to convert an amount in `from_currency` into `to_currency`.
    Generic - works for financial-statement currency, or an estimate's own
    `currency` column, or anything else that turns up."""
    if not from_currency or not to_currency or from_currency == to_currency:
        return 1.0

    def fetch_rate():
        pair = yf.Ticker(f"{from_currency}{to_currency}=X").info
        rate = pair.get("regularMarketPrice") or pair.get("currentPrice")
        return rate if rate else 1.0
    return cached(("fx", from_currency, to_currency), fetch_rate)


def get_financial_fx_rate(symbol):
    """Financial statements for foreign companies (e.g. TSM's ADR) are often reported
    in the home country's currency (financialCurrency) while price/marketCap use a
    different one (currency, usually USD) - dividing one by the other without
    converting first gives a nonsense ratio (TSM's P/E came out as 1.2 instead of ~31).
    Returns the multiplier to convert a financial-statement amount into the quote currency."""
    def fetch_financial_currency():
        return yf.Ticker(symbol).info.get("financialCurrency")
    financial_currency = cached(("financial-currency", symbol), fetch_financial_currency)
    return get_fx_rate(financial_currency, get_quote_currency(symbol))


@app.route("/search/<query>")
def search(query):
    def fetch():
        results = yf.Search(query, max_results=5).quotes
        return [{
            "symbol": r.get("symbol"),
            "name": r.get("longname") or r.get("shortname"),
            "exchange": r.get("exchDisp"),
        } for r in results if r.get("quoteType") == "EQUITY"]
    return jsonify(cached(("search", query.lower()), fetch))


@app.route("/quote/<symbol>")
def quote(symbol):
    def fetch():
        info = yf.Ticker(symbol).info
        if not info or info.get("regularMarketPrice") is None:
            return []
        return [{
            "symbol": symbol,
            "name": info.get("longName") or info.get("shortName"),
            "price": clean(info.get("currentPrice") or info.get("regularMarketPrice")),
            "changePercentage": clean(info.get("regularMarketChangePercent")) or 0,
            "marketCap": clean(info.get("marketCap")),
            "exchange": info.get("exchange"),
        }]
    return jsonify(cached(("quote", symbol), fetch))


@app.route("/income-statement/<symbol>")
def income_statement(symbol):
    def fetch():
        fin = yf.Ticker(symbol).financials
        if fin.empty or "Total Revenue" not in fin.index:
            return []
        fx = get_financial_fx_rate(symbol)
        result = []
        for year in fin.columns:
            revenue = clean(fin.loc["Total Revenue", year])
            net_income = clean(fin.loc["Net Income", year]) if "Net Income" in fin.index else None
            if revenue is None or net_income is None:
                continue
            result.append({
                "symbol": symbol,
                "fiscalYear": str(year.year),
                "revenue": revenue * fx,
                "netIncome": net_income * fx,
            })
        return result  # newest first, however many years yfinance has (usually ~4)
    return jsonify(cached(("income-statement", symbol), fetch))


@app.route("/estimates/<symbol>")
def estimates(symbol):
    """Analyst consensus for the current and next fiscal year (Yahoo doesn't publish
    further out than that for free - see the earlier conversation about long-term
    growth estimates being empty even for mega-caps).

    epsEstimate/revenueEstimate are shown as-is for display, but the *growth* fields
    are what the CAGR/PEG(CAGR) calculation on the Java side actually uses - they're
    computed by Yahoo from same-currency, same-share-basis figures (this year's
    estimate vs last year's actual), so they stay correct even for ADRs like TSM
    where the raw EPS number alone would be off by both a currency and a share-ratio
    factor - see get_financial_fx_rate's docstring for the same issue on historicals.
    """
    def fetch():
        t = yf.Ticker(symbol)
        fin = t.financials
        eps_est = t.earnings_estimate
        rev_est = t.revenue_estimate
        if fin.empty or eps_est.empty or rev_est.empty:
            return []

        last_reported_year = fin.columns[0].year  # most recent fiscal year already closed
        target_currency = get_quote_currency(symbol)
        result = []
        # "0y" = the fiscal year currently in progress (not yet reported), "+1y" = the one after.
        for period, years_ahead in [("0y", 1), ("+1y", 2)]:
            if period not in eps_est.index or period not in rev_est.index:
                continue
            eps = clean(eps_est.loc[period, "avg"])
            eps_growth = clean(eps_est.loc[period, "growth"])
            revenue = clean(rev_est.loc[period, "avg"])
            revenue_growth = clean(rev_est.loc[period, "growth"])
            if eps is None or revenue is None:
                continue
            revenue_fx = get_fx_rate(rev_est.loc[period, "currency"], target_currency)
            result.append({
                "symbol": symbol,
                "fiscalYear": str(last_reported_year + years_ahead),
                "epsEstimate": eps,
                "epsGrowth": eps_growth,
                "revenueEstimate": revenue * revenue_fx,
                "revenueGrowth": revenue_growth,
                # int() because this column is numpy.int64, which - unlike numpy.float64,
                # which quietly IS a subclass of Python's float - isn't a subclass of
                # Python's int and so isn't JSON-serializable as-is.
                "analystCount": int(eps_est.loc[period, "numberOfAnalysts"]),
            })
        return result
    return jsonify(cached(("estimates", symbol), fetch))


@app.route("/balance-sheet/<symbol>")
def balance_sheet(symbol):
    def fetch():
        bs = yf.Ticker(symbol).balance_sheet
        if bs.empty or "Stockholders Equity" not in bs.index:
            return []
        year = bs.columns[0]
        equity = clean(bs.loc["Stockholders Equity", year])
        liabilities = clean(bs.loc["Total Liabilities Net Minority Interest", year]) if "Total Liabilities Net Minority Interest" in bs.index else None
        if equity is None or liabilities is None:
            return []
        # cash/totalDebt are for the DCF's enterprise-value -> equity-value step (see
        # AnalysisService) - less fundamental than equity/liabilities, so a company
        # missing one of these (rare, but e.g. some financials don't report "Total Debt"
        # the usual way) still gets a balance sheet back, just with 0 there instead of
        # failing the whole lookup.
        cash = clean(bs.loc["Cash And Cash Equivalents", year]) if "Cash And Cash Equivalents" in bs.index else None
        total_debt = clean(bs.loc["Total Debt", year]) if "Total Debt" in bs.index else None
        fx = get_financial_fx_rate(symbol)
        return [{
            "symbol": symbol,
            "fiscalYear": str(year.year),
            "totalStockholdersEquity": equity * fx,
            "totalLiabilities": liabilities * fx,
            "cash": (cash or 0) * fx,
            "totalDebt": (total_debt or 0) * fx,
        }]
    return jsonify(cached(("balance-sheet", symbol), fetch))


@app.route("/cash-flow/<symbol>")
def cash_flow(symbol):
    def fetch():
        cf = yf.Ticker(symbol).cashflow
        if cf.empty or "Free Cash Flow" not in cf.index:
            return []
        year = cf.columns[0]
        fcf = clean(cf.loc["Free Cash Flow", year])
        if fcf is None:
            return []
        fx = get_financial_fx_rate(symbol)
        return [{
            "symbol": symbol,
            "fiscalYear": str(year.year),
            "freeCashFlow": fcf * fx,
        }]
    return jsonify(cached(("cash-flow", symbol), fetch))


if __name__ == "__main__":
    # threaded=True lets Flask handle several symbols' requests concurrently
    # instead of queuing them one at a time - needed for the Java side's
    # parallel fetching to actually run in parallel.
    app.run(port=5000, threaded=True)
