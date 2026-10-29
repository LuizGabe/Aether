package com.aether.api.controller;

import com.aether.api.dto.AccountDto;
import com.aether.api.service.FinanceQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final FinanceQueryService financeQueryService;

    public AccountController(FinanceQueryService financeQueryService) {
        this.financeQueryService = financeQueryService;
    }

    @GetMapping
    public List<AccountDto> list() {
        return financeQueryService.accounts();
    }
}
