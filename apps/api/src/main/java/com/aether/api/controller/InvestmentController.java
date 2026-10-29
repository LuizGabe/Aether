package com.aether.api.controller;

import com.aether.api.dto.InvestmentDto;
import com.aether.api.service.FinanceQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final FinanceQueryService financeQueryService;

    public InvestmentController(FinanceQueryService financeQueryService) {
        this.financeQueryService = financeQueryService;
    }

    @GetMapping
    public List<InvestmentDto> list() {
        return financeQueryService.investments();
    }
}
