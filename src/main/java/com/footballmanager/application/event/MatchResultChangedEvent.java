package com.footballmanager.application.event;

public record MatchResultChangedEvent(long matchId, long tournamentId) {}
