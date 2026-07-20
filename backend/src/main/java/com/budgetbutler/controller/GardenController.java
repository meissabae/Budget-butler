package com.budgetbutler.controller;

import com.budgetbutler.dto.GardenResponse;
import com.budgetbutler.security.CurrentUserProvider;
import com.budgetbutler.service.GardenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/garden")
public class GardenController {

    @Autowired
    private GardenService gardenService;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @GetMapping
    public GardenResponse getGarden() {
        return gardenService.getGardenStatus(currentUserProvider.getCurrentUser());
    }
}
