package com.footballmanager.ui.navigation;

public enum ScreenId {
    DASHBOARD("Dashboard", "Tournament overview", "No tournament activity yet", "Create teams and a tournament to begin."),
    TEAMS("Teams", "Club registration", "No teams to display", "Team management is not available in the current foundation release."),
    PLAYERS("Players", "Squad registration", "No players to display", "Player management is not available in the current foundation release."),
    TOURNAMENTS("Tournaments", "Setup and lifecycle", "No tournament workflow yet", "Tournament setup will be added after the domain services are ready."),
    FIXTURES_RESULTS("Fixtures & Results", "Scheduling and scores", "No fixtures have been generated", "Fixture and result workflows will be added during later development."),
    STANDINGS_REPORTS("Standings & Reports", "Tables and summaries", "No standings are available", "Standings and reports require completed match features.");

    private final String title;
    private final String subtitle;
    private final String emptyTitle;
    private final String emptyMessage;

    ScreenId(String title, String subtitle, String emptyTitle, String emptyMessage) {
        this.title = title;
        this.subtitle = subtitle;
        this.emptyTitle = emptyTitle;
        this.emptyMessage = emptyMessage;
    }

    public String title() {
        return title;
    }

    public String subtitle() {
        return subtitle;
    }

    public String emptyTitle() {
        return emptyTitle;
    }

    public String emptyMessage() {
        return emptyMessage;
    }
}
