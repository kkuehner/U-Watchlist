package com.example.Investment.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Investment.model.SearchResult;
import com.example.Investment.model.StockQuote;
import com.example.Investment.service.StockApiService;
import com.example.Investment.service.ValuationService;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/stock")
public class TestController {

    private final StockApiService service;
    private final ValuationService valuationService;

    public TestController(StockApiService service, ValuationService valuationService){
        this.service = service;
        this.valuationService = valuationService;
    }

    @GetMapping("/{symbol}")
    public String getStock(@PathVariable String symbol) {
        return service.getStockData(symbol);
    }

    @GetMapping("/search")
    public SearchResult[] search(@RequestParam String q) {
        return service.searchSymbols(q);
    }

    @GetMapping("/{symbol}/quote")
    public StockQuote getQuote(@PathVariable String symbol) {
        return service.getStockQuote(symbol);
    }
    
    @GetMapping("/{symbol}/valuation")
    public Map<String, Object> getKPIs(@PathVariable String symbol) {
        return valuationService.getKpisFor(symbol);
    }

}
