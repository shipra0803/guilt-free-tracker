package com.guiltfree.tracker.controller;

import com.guiltfree.tracker.dto.HistoryResponse;
import com.guiltfree.tracker.model.HistoryRange;
import com.guiltfree.tracker.service.HistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Serves the spending-over-time chart and range totals for a selected preset date range.
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    // Injects the service that computes the history data.
    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    // Returns chart data + totals for the given range, defaulting to the current month.
    @GetMapping
    public HistoryResponse getHistory(@RequestParam(name = "range", defaultValue = "THIS_MONTH") HistoryRange range) {
        return historyService.getHistory(range);
    }
}
