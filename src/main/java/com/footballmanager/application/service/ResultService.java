package com.footballmanager.application.service;

import com.footballmanager.application.command.CommandHistory;
import com.footballmanager.application.command.RecordMatchResultCommand;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Match;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.util.Objects;

public final class ResultService {
    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final CommandHistory<RecordMatchResultCommand> commandHistory;

    public ResultService(
            MatchRepository matchRepository,
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository
    ) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
        this.teamRepository = Objects.requireNonNull(teamRepository);
        this.commandHistory = new CommandHistory<>();
    }

    public MatchDto recordResult(long matchId, int homeScore, int awayScore) {
        RecordMatchResultCommand command = new RecordMatchResultCommand(
                matchId, homeScore, awayScore, matchRepository, tournamentRepository
        );
        command.execute();
        commandHistory.push(command);
        return loadMatchDto(matchId);
    }

    public MatchDto correctResult(long matchId, int homeScore, int awayScore) {
        return recordResult(matchId, homeScore, awayScore);
    }

    public MatchDto undoLastResult() {
        RecordMatchResultCommand undone = commandHistory.undoLast();
        return loadMatchDto(undone.matchId());
    }

    public boolean canUndo() {
        return commandHistory.hasHistory();
    }

    public CommandHistory<RecordMatchResultCommand> commandHistory() {
        return commandHistory;
    }

    private MatchDto loadMatchDto(long matchId) {
        if (matchId <= 0) {
            throw new ValidationException("Match id must be a positive number");
        }
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " was not found"));
        return new MatchDto(
                match.id(),
                match.tournamentId(),
                match.roundNumber(),
                match.homeTeamId(),
                teamName(match.homeTeamId()),
                match.awayTeamId(),
                teamName(match.awayTeamId()),
                match.homeScore(),
                match.awayScore(),
                match.status(),
                match.scheduledAt(),
                match.nextMatchId(),
                match.nextMatchSlot()
        );
    }

    private String teamName(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " was not found"))
                .name();
    }
}
