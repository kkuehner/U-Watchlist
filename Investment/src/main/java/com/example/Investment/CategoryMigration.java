package com.example.Investment;

import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.Investment.entity.Category;
import com.example.Investment.entity.WatchlistEntry;
import com.example.Investment.repository.CategoryRepository;
import com.example.Investment.repository.WatchlistRepository;

// One-time startup migration: categories used to be a fixed GROWTH/BLUE_CHIP enum
// stored directly on watchlist_entry.category (plain text). Now categories are their
// own editable entity, referenced via the new watchlist_entry.category_id column.
// This runs once (guarded by categoryRepository.count() == 0) to recreate those two
// as real, editable Category rows and re-point existing watchlist entries at them,
// reading the old text column directly since it's no longer JPA-mapped.
@Component
public class CategoryMigration implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final WatchlistRepository watchlistRepository;
    private final JdbcTemplate jdbcTemplate;

    public CategoryMigration(CategoryRepository categoryRepository, WatchlistRepository watchlistRepository, JdbcTemplate jdbcTemplate){
        this.categoryRepository = categoryRepository;
        this.watchlistRepository = watchlistRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // already migrated
        }

        Category growth = new Category();
        growth.setName("Growth");
        growth.setKpiWeights(Map.of("peg", 5.0, "psg", 5.0, "Rule40", 5.0));
        growth = categoryRepository.save(growth);

        Category blueChip = new Category();
        blueChip.setName("Blue Chip");
        blueChip.setKpiWeights(Map.of("pe", 5.0, "ps", 5.0, "pb", 5.0, "ROE", 5.0, "debtRatio", 5.0));
        blueChip = categoryRepository.save(blueChip);

        List<Map<String, Object>> oldRows = jdbcTemplate.queryForList("SELECT id, category FROM watchlist_entry");
        for (Map<String, Object> row : oldRows) {
            Object oldCategory = row.get("category");
            if (oldCategory == null) continue;

            Long entryId = ((Number) row.get("id")).longValue();
            WatchlistEntry entry = watchlistRepository.findById(entryId).orElse(null);
            if (entry == null) continue;

            if ("GROWTH".equals(oldCategory)) {
                entry.setCategory(growth);
                watchlistRepository.save(entry);
            } else if ("BLUE_CHIP".equals(oldCategory)) {
                entry.setCategory(blueChip);
                watchlistRepository.save(entry);
            }
        }
    }
}
