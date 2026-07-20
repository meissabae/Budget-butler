package com.budgetbutler.controller;

import com.budgetbutler.model.Dream;
import com.budgetbutler.repository.DreamRepository;
import com.budgetbutler.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dreams")
public class DreamController {

    @Autowired
    private DreamRepository dreamRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<Dream> getAllDreams() {
        return dreamRepository.findByOwner(currentUserProvider.getCurrentUser());
    }

    @PostMapping
    public Dream createDream(@RequestBody Dream dream) {
        dream.setSavedAmount(0.0); // every new dream starts at 0 progress
        dream.setOwner(currentUserProvider.getCurrentUser());
        return dreamRepository.save(dream);
    }

    @PutMapping("/{id}")
    public Dream updateDream(@PathVariable Long id, @RequestBody Dream updatedDream) {
        Dream dream = dreamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dream not found"));

        if (!dream.getOwner().getId().equals(currentUserProvider.getCurrentUser().getId())) {
            throw new RuntimeException("You don't have permission to edit this dream.");
        }

        dream.setName(updatedDream.getName());
        dream.setTargetAmount(updatedDream.getTargetAmount());
        // savedAmount is intentionally NOT editable here - it's meant to only grow through
        // the Dream Protector's monthly savings logic, not be typed in directly.
        return dreamRepository.save(dream);
    }

    @DeleteMapping("/{id}")
    public void deleteDream(@PathVariable Long id) {
        Dream dream = dreamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dream not found"));

        if (!dream.getOwner().getId().equals(currentUserProvider.getCurrentUser().getId())) {
            throw new RuntimeException("You don't have permission to delete this dream.");
        }

        dreamRepository.deleteById(id);
    }
}
