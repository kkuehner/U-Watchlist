package com.example.Investment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Investment.entity.Category;
import com.example.Investment.entity.WatchlistEntry;
import com.example.Investment.exception.CategoryNotFoundException;
import com.example.Investment.repository.CategoryRepository;
import com.example.Investment.repository.WatchlistRepository;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository repository;
    private final WatchlistRepository watchlistRepository;

    public CategoryController(CategoryRepository repository, WatchlistRepository watchlistRepository){
        this.repository = repository;
        this.watchlistRepository = watchlistRepository;
    }

    @GetMapping
    public List<Category> getCategories() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        category.setId(null); // never trust a client-supplied id on create
        if (repository.findByName(category.getName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(category));
    }

    @PutMapping("/{id}")
    public Category updateCategory(@PathVariable Long id, @RequestBody Category update) {
        Category category = repository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        category.setName(update.getName());
        category.setKpiWeights(update.getKpiWeights());
        return repository.save(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        // Stocks in this category fall back to "uncategorized" rather than being deleted too.
        for (WatchlistEntry entry : watchlistRepository.findAll()) {
            if (entry.getCategory() != null && entry.getCategory().getId().equals(id)) {
                entry.setCategory(null);
                watchlistRepository.save(entry);
            }
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
