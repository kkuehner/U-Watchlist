package com.example.Investment.entity;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;

@Entity
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    // e.g. { "peg": 5, "psg": 5, "Rule40": 8 } - which KPIs this category cares
    // about and how heavily each counts towards the ranking score.
    @ElementCollection
    @CollectionTable(name = "category_kpi_weight", joinColumns = @JoinColumn(name = "category_id"))
    @MapKeyColumn(name = "kpi")
    @Column(name = "weight")
    private Map<String, Double> kpiWeights = new HashMap<>();

    public Category(){};

    // ---- Getter & Setter ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, Double> getKpiWeights() { return kpiWeights; }
    public void setKpiWeights(Map<String, Double> kpiWeights) { this.kpiWeights = kpiWeights; }
}
