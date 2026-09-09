package com.footballmanager.domain.scheduling;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.TournamentRegistration;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ScheduleInput {
    private ScheduleInput() {
    }

    static List<TournamentRegistration> validateAndSort(
            long tournamentId,
            List<TournamentRegistration> registrations
    ) {
        if (tournamentId <= 0) {
            throw new ValidationException("Tournament id must be positive");
        }
        if (registrations == null) {
            throw new ValidationException("Tournament registrations are required");
        }
        Set<Long> teamIds = new HashSet<>();
        Set<Integer> seeds = new HashSet<>();
        for (TournamentRegistration registration : registrations) {
            if (registration == null || registration.tournamentId() != tournamentId) {
                throw new ValidationException("Every registration must belong to the tournament");
            }
            if (!teamIds.add(registration.teamId())) {
                throw new BusinessRuleException("Registered teams must be unique");
            }
            if (!seeds.add(registration.seedNumber())) {
                throw new BusinessRuleException("Registered seeds must be unique");
            }
        }
        return registrations.stream()
                .sorted(Comparator.comparingInt(TournamentRegistration::seedNumber))
                .toList();
    }
}
