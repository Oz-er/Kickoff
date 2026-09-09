package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.event.ApplicationEventPublisher;
import com.footballmanager.application.event.MatchResultChangedEvent;
import com.footballmanager.application.service.ReportService;
import com.footballmanager.application.service.ResultService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.domain.model.MatchStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class FixturesViewModel {
    private final TournamentService tournamentService;
    private final ReportService reportService;
    private final ResultService resultService;
    private final ApplicationEventPublisher eventPublisher;

    private Long selectedTournamentId;
    private Integer selectedRound;
    private MatchStatus selectedStatus;

    public FixturesViewModel(
            TournamentService tournamentService,
            ReportService reportService,
            ResultService resultService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.tournamentService = Objects.requireNonNull(tournamentService);
        this.reportService = Objects.requireNonNull(reportService);
        this.resultService = Objects.requireNonNull(resultService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    public List<TournamentDto> loadTournaments() {
        return tournamentService.findAll();
    }

    public void setFilters(Long tournamentId, Integer round, MatchStatus status) {
        this.selectedTournamentId = tournamentId;
        this.selectedRound = round;
        this.selectedStatus = status;
    }

    public List<MatchDto> loadMatches() {
        if (selectedTournamentId == null) {
            return List.of();
        }

        List<MatchDto> matches = reportService.getTournamentMatches(selectedTournamentId);

        return matches.stream()
                .filter(m -> selectedRound == null || Objects.equals(m.roundNumber(), selectedRound))
                .filter(m -> selectedStatus == null || m.status() == selectedStatus)
                .collect(Collectors.toList());
    }

    public List<Integer> getAvailableRounds() {
        if (selectedTournamentId == null) {
            return List.of();
        }
        return reportService.getTournamentMatches(selectedTournamentId).stream()
                .map(MatchDto::roundNumber)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public void recordResult(long matchId, int homeScore, int awayScore) {
        MatchDto updated = resultService.recordResult(matchId, homeScore, awayScore);
        eventPublisher.publish(new MatchResultChangedEvent(updated.id(), updated.tournamentId()));
    }

    public void correctResult(long matchId, int homeScore, int awayScore) {
        MatchDto updated = resultService.correctResult(matchId, homeScore, awayScore);
        eventPublisher.publish(new MatchResultChangedEvent(updated.id(), updated.tournamentId()));
    }

    public void undoResult() {
        if (resultService.canUndo()) {
            MatchDto undone = resultService.undoLastResult();
            eventPublisher.publish(new MatchResultChangedEvent(undone.id(), undone.tournamentId()));
        }
    }

    public boolean canUndo() {
        return resultService.canUndo();
    }
}
