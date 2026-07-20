package com.budgetbutler.controller;

import com.budgetbutler.dto.StreakResponse;
import com.budgetbutler.security.CurrentUserProvider;
import com.budgetbutler.service.StreakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/streak")
public class StreakController {

    @Autowired
    private StreakService streakService;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @GetMapping
    public StreakResponse getStreak() {
        return streakService.getStreakStatus(currentUserProvider.getCurrentUser());
    }
}
