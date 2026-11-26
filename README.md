# U-Watchlist

A stock watchlist built around your own investment strategy. Sort stocks into your own categories, dig into each company's financial statements, and judge its price with valuation tools such as the Graham Number and an interactive DCF whose assumptions you set with sliders.

## Features

- **Watchlist** with custom categories and current valuations
- **Analysis page** per stock: income statement, balance sheet, cash flow and earnings estimates
- **Decision page**: Graham Number, leverage-vs-earnings checks and a DCF with adjustable growth and discount rates
- **Currency conversion** of prices and financials

## Tech stack

- **Backend:** Java 25, Spring Boot 4, Spring Data JPA, H2 file database
- **Market data:** Python/Flask sidecar using yfinance, plus the Financial Modeling Prep API
- **Frontend:** static HTML and JavaScript served by Spring Boot

## Getting started

Requirements: Java 25, Python 3.11 or later, and a free API key from [Financial Modeling Prep](https://site.financialmodelingprep.com/).

1. Install the sidecar's dependencies:
   ```
   pip install -r yfinance-sidecar/requirements.txt
   ```
2. Create `Investment/src/main/resources/application-local.properties` with your key (this file is git-ignored):
   ```
   fmp.api.key=YOUR_KEY
   ```
3. Start both services from the repository root (Windows PowerShell):
   ```
   ./start.ps1
   ```
   This starts the sidecar on port 5000 and the app on port 8080.
4. Open http://localhost:8080

## Status

Personal project in an early stage. The code was developed locally before being put under version control, so the history starts with a single commit.