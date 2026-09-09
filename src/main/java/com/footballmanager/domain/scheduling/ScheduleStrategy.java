package com.footballmanager.domain.scheduling;

import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentRegistration;

import java.util.List;

public interface ScheduleStrategy {
    TournamentFormat format();

    List<FixtureDraft> generate(long tournamentId, List<TournamentRegistration> registrations);
}
