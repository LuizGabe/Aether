package com.aether.api.controller;

import com.aether.api.dto.CategoryDto;
import com.aether.api.service.FinanceQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final FinanceQueryService financeQueryService;

    public CategoryController(FinanceQueryService financeQueryService) {
        this.financeQueryService = financeQueryService;
    }

    @GetMapping
    public List<CategoryDto> list() {
        return financeQueryService.categories();
    }
}
