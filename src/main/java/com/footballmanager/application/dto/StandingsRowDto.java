package com.footballmanager.application.dto;

public record StandingsRowDto(
        long teamId,
        String teamName,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points
) {}
