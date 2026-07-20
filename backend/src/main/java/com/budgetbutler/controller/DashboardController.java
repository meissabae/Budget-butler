package com.budgetbutler.controller;

import com.budgetbutler.dto.DashboardResponse;
import com.budgetbutler.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is the ONE endpoint the Angular dashboard page calls when it loads:
 * GET http://localhost:8080/api/dashboard
 * It returns everything: nickname, twin comparison, pace analysis, dream protector,
 * memory lane, and (if applicable) a weekly/monthly report - all in one JSON object.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.buildDashboard();
    }
}
