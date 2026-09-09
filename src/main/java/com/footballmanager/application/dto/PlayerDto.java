package com.footballmanager.application.dto;

public record PlayerDto(
        long id,
        long teamId,
        String teamName,
        String fullName,
        int shirtNumber,
        String position
) {
}
