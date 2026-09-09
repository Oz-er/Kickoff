package com.footballmanager.application.dto;

public record SavePlayerRequest(long teamId, String fullName, int shirtNumber, String position) {
}
