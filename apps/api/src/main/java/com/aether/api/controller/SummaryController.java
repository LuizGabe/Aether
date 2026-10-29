package com.aether.api.controller;

import com.aether.api.service.FinanceQueryService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummaryController {
    private final FinanceQueryService financeQueryService;

    public SummaryController(FinanceQueryService financeQueryService) {
        this.financeQueryService = financeQueryService;
    }

    @GetMapping("/api/summary")
    public Map<String, Integer> summary() {
        return financeQueryService.summary();
    }
}
