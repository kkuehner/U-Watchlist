package com.example.Investment.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Investment.entity.Category;
import com.example.Investment.entity.WatchlistEntry;
import com.example.Investment.exception.CategoryNotFoundException;
import com.example.Investment.exception.InsufficientDataException;
import com.example.Investment.exception.StockNotFoundException;
import com.example.Investment.repository.CategoryRepository;
import com.example.Investment.repository.WatchlistRepository;
import com.example.Investment.service.ValuationService;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistRepository repository;
    private final CategoryRepository categoryRepository;
    private final ValuationService valuationService;

    public WatchlistController(WatchlistRepository repository, CategoryRepository categoryRepository, ValuationService valuationService){
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.valuationService = valuationService;
    }

    @GetMapping
    public List<WatchlistEntry> getWatchlist() {
        return repository.findAll();
    }

    @PostMapping("/{symbol}")
    public ResponseEntity<WatchlistEntry> addToWatchlist(@PathVariable String symbol, @RequestParam Long categoryId) {
        if (repository.existsBySymbol(symbol)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        WatchlistEntry entry = new WatchlistEntry();
        entry.setSymbol(symbol);
        entry.setCategory(category);
        WatchlistEntry saved = repository.save(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{symbol}/category")
    public WatchlistEntry updateCategory(@PathVariable String symbol, @RequestParam Long categoryId) {
        WatchlistEntry entry = repository.findBySymbol(symbol)
            .orElseThrow(() -> new StockNotFoundException(symbol));
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        entry.setCategory(category);
        return repository.save(entry);
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable String symbol) {
        WatchlistEntry entry = repository.findBySymbol(symbol)
            .orElseThrow(() -> new StockNotFoundException(symbol));
        repository.delete(entry);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/valuations")
    public List<Map<String, Object>> getWatchlistValuations() {
        // parallelStream fans these out across threads instead of fetching one
        // stock at a time - the slow part is waiting on the sidecar/network, not CPU,
        // so running several of those waits at once is a big win for a 15+ stock list.
        return repository.findAll().parallelStream()
            .map(this::getKpisOrError)
            .toList();
    }

    // One bad symbol (sidecar down, delisted, etc.) shouldn't take down
    // the whole watchlist view - surface it as an error entry instead of failing the request.
    private Map<String, Object> getKpisOrError(WatchlistEntry entry) {
        String symbol = entry.getSymbol();
        try {
            Map<String, Object> kpis = new HashMap<>(valuationService.getKpisFor(symbol));
            kpis.put("category", entry.getCategory());
            return kpis;
        } catch (StockNotFoundException | InsufficientDataException ex) {
            return Map.of("symbol", symbol, "category", entry.getCategory(), "error", ex.getMessage());
        } catch (ResourceAccessException ex) {
            return Map.of("symbol", symbol, "category", entry.getCategory(), "error", "Can't reach the yfinance sidecar - make sure yfinance-sidecar/app.py is running.");
        } catch (RestClientException ex) {
            return Map.of("symbol", symbol, "category", entry.getCategory(), "error", "Data provider error - this symbol may not be available.");
        }
    }

}
