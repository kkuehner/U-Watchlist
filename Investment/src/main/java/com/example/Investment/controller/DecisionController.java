package com.example.Investment.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Investment.service.DecisionService;

@RestController
@RequestMapping("/decision")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService){
        this.decisionService = decisionService;
    }

    @GetMapping("/{symbol}")
    public Map<String, Object> getDecisionMetrics(@PathVariable String symbol) {
        return decisionService.getDecisionMetricsFor(symbol);
    }

}
