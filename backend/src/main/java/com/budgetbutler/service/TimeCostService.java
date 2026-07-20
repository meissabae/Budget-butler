package com.budgetbutler.service;

import com.budgetbutler.model.User;
import com.budgetbutler.model.UserSettings;
import com.budgetbutler.repository.UserSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Turns money into something more visceral than a number: "how many hours of YOUR life
 * did this cost?" hourlyRate = monthlySalary / monthlyWorkingHours, and timeCost = amount / hourlyRate.
 *
 * If the user hasn't filled in their salary/hours yet, every method here safely returns
 * null/zero instead of crashing - the rest of the app treats "no time cost data" as
 * "just don't show this feature yet".
 */
@Service
public class TimeCostService {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    /** Returns null if the user hasn't set up salary + working hours yet. */
    public Double getHourlyRate(User user) {
        Optional<UserSettings> settingsOpt = userSettingsRepository.findByOwner(user);
        if (settingsOpt.isEmpty()) {
            return null;
        }
        UserSettings settings = settingsOpt.get();
        if (settings.getMonthlySalary() == null || settings.getMonthlyWorkingHours() == null
                || settings.getMonthlyWorkingHours() <= 0) {
            return null;
        }
        return settings.getMonthlySalary() / settings.getMonthlyWorkingHours();
    }

    /** How many hours of work a given amount represents. Returns null if hourlyRate is unknown. */
    public Double calculateTimeCostHours(Double amount, Double hourlyRate) {
        if (hourlyRate == null || hourlyRate <= 0) {
            return null;
        }
        return amount / hourlyRate;
    }

    /**
     * Turns a raw number of hours into a human, slightly dramatic sentence.
     * This is the piece that gives Feature 2 its emotional impact.
     */
    public String buildTimeCostMessage(Double hours) {
        if (hours == null) {
            return null;
        }
        if (hours >= 8) {
            double days = hours / 8.0;
            return String.format("This cost you about %.1f full working day(s).", days);
        } else if (hours >= 1) {
            return String.format("This cost you %.1f hour(s) of work.", hours);
        } else {
            int minutes = (int) Math.round(hours * 60);
            return String.format("This cost you about %d minute(s) of work.", minutes);
        }
    }
}
