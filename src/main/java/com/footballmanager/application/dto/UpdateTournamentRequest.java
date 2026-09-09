package com.footballmanager.application.dto;

import com.footballmanager.domain.model.TournamentFormat;

import java.time.LocalDate;

public record UpdateTournamentRequest(String name, TournamentFormat format, LocalDate startDate) {
}
