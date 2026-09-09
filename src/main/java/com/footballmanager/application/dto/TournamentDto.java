package com.footballmanager.application.dto;

import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TournamentDto(
        long id,
        String name,
        TournamentFormat format,
        TournamentStatus status,
        LocalDate startDate,
        LocalDateTime createdAt
) {
}
