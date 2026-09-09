package com.footballmanager.application.command;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchSnapshot;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.util.Objects;

public final class RecordMatchResultCommand {
    private final long matchId;
    private final int homeScore;
    private final int awayScore;
    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private MatchSnapshot snapshot;
    private boolean executed;

    public RecordMatchResultCommand(
            long matchId,
            int homeScore,
            int awayScore,
            MatchRepository matchRepository,
            TournamentRepository tournamentRepository
    ) {
        if (matchId <= 0) {
            throw new ValidationException("Match id must be a positive number");
        }
        if (homeScore < 0 || awayScore < 0) {
            throw new ValidationException("Scores cannot be negative");
        }
        this.matchId = matchId;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
    }

    public void execute() {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match " + matchId + " was not found"));
        if (match.status() == MatchStatus.PENDING) {
            throw new BusinessRuleException("A pending match cannot have a result recorded");
        }
        TournamentFormat format = tournamentRepository.findById(match.tournamentId())
                .orElseThrow(() -> new EntityNotFoundException("Tournament " + match.tournamentId() + " was not found"))
                .format();
        if (format == TournamentFormat.KNOCKOUT && homeScore == awayScore) {
            throw new BusinessRuleException("Knockout matches cannot end in a draw");
        }

        Long progressedTeamId = null;
        MatchStatus nextMatchPreviousStatus = null;
        Long nextMatchPreviousHomeTeamId = null;
        Long nextMatchPreviousAwayTeamId = null;
        if (match.nextMatchId() != null) {
            Match nextMatch = matchRepository.findById(match.nextMatchId())
                    .orElseThrow(() -> new EntityNotFoundException("Next match " + match.nextMatchId() + " was not found"));
            if (nextMatch.status() == MatchStatus.COMPLETED) {
                throw new BusinessRuleException("This result cannot be changed because the next knockout match is already completed");
            }
            nextMatchPreviousStatus = nextMatch.status();
            nextMatchPreviousHomeTeamId = nextMatch.homeTeamId();
            nextMatchPreviousAwayTeamId = nextMatch.awayTeamId();
        }

        long winnerId = homeScore > awayScore ? match.homeTeamId() : match.awayTeamId();
        Long teamToProgress = match.nextMatchId() != null ? winnerId : null;

        snapshot = new MatchSnapshot(
                match.id(),
                match.homeScore(),
                match.awayScore(),
                match.status(),
                match.nextMatchId(),
                match.nextMatchSlot(),
                teamToProgress,
                nextMatchPreviousStatus,
                nextMatchPreviousHomeTeamId,
                nextMatchPreviousAwayTeamId
        );

        try {
            matchRepository.recordResult(match.id(), homeScore, awayScore, match.nextMatchId(), match.nextMatchSlot(), teamToProgress);
            executed = true;
        } catch (RuntimeException exception) {
            snapshot = null;
            throw exception;
        }
    }

    public void undo() {
        if (!executed || snapshot == null) {
            throw new BusinessRuleException("There is no executed result to undo");
        }
        matchRepository.undoResult(snapshot);
        executed = false;
    }

    public long matchId() {
        return matchId;
    }

    public int homeScore() {
        return homeScore;
    }

    public int awayScore() {
        return awayScore;
    }

    public boolean isExecuted() {
        return executed;
    }
}
