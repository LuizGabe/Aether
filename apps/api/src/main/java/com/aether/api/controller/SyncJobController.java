package com.aether.api.controller;

import com.aether.api.dto.SyncJobDto;
import com.aether.api.service.FinanceQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync-jobs")
public class SyncJobController {

    private final FinanceQueryService financeQueryService;

    public SyncJobController(FinanceQueryService financeQueryService) {
        this.financeQueryService = financeQueryService;
    }

    @GetMapping
    public List<SyncJobDto> list() {
        return financeQueryService.syncJobs();
    }
}
