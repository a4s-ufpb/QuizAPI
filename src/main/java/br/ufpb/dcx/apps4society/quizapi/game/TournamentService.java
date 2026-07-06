package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateRoomRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateTournamentRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.GameConfig;
import br.ufpb.dcx.apps4society.quizapi.game.dto.RoomStateResponse;
import br.ufpb.dcx.apps4society.quizapi.game.dto.TournamentStateResponse;
import br.ufpb.dcx.apps4society.quizapi.game.model.*;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Torneios eliminatórios em memória — mesmo padrão do {@link GameRoomService}
 * (nada persistido). Cada confronto vira uma sala normal de multiplayer
 * (2 jogadores); o torneio só monta o chaveamento e observa (polling) quando
 * cada sala termina, pra avançar o vencedor pra próxima rodada.
 */
@Service
public class TournamentService {
    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long IDLE_TIMEOUT_MINUTES = 60;

    private final Map<String, Tournament> tournaments = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final GameRoomService gameRoomService;
    private final ThemeRepository themeRepository;

    public TournamentService(GameRoomService gameRoomService, ThemeRepository themeRepository) {
        this.gameRoomService = gameRoomService;
        this.themeRepository = themeRepository;
        this.scheduler.scheduleAtFixedRate(this::pollActiveMatches, 3, 3, TimeUnit.SECONDS);
        this.scheduler.scheduleAtFixedRate(this::sweepIdleTournaments, 10, 10, TimeUnit.MINUTES);
    }

    public synchronized TournamentStateResponse createTournament(CreateTournamentRequest request) {
        String code = generateUniqueCode();
        String hostId = request.hostId() != null ? request.hostId() : UUID.randomUUID().toString();

        GameConfig matchConfig = new GameConfig(
                RoomMode.INDIVIDUAL, ScoringMode.SPEED, AdvanceMode.HOST,
                request.questionTimeSeconds(), request.questionCount(), null, GameStyle.NORMAL, 2
        ).withDefaults();

        Tournament tournament = new Tournament(code, hostId, safeName(request.name(), "Torneio"), request.themeId(), matchConfig);
        // Capacidade não conta o host (ele não pontua). Teto absoluto de 24;
        // o controller já restringe por papel (convidado 12, logado 12/24).
        int max = request.maxPlayers() != null ? Math.max(2, Math.min(24, request.maxPlayers())) : 12;
        tournament.setMaxPlayers(max);
        if (request.themeId() != null) {
            Theme theme = themeRepository.findById(request.themeId()).orElse(null);
            tournament.setThemeName(theme != null ? theme.getName() : null);
        }
        tournament.getPlayers().put(hostId, new TournamentPlayer(hostId, safeName(request.hostName(), "Host"), true));
        tournaments.put(code, tournament);
        return toState(tournament);
    }

    public synchronized TournamentStateResponse join(String code, String playerId, String name) {
        Tournament tournament = requireTournament(code);
        if (tournament.getStatus() != TournamentStatus.LOBBY) {
            throw new IllegalStateException("O torneio já começou.");
        }
        boolean isNew = !tournament.getPlayers().containsKey(playerId);
        long nonHost = tournament.getPlayers().values().stream().filter(p -> !p.isHost()).count();
        if (isNew && nonHost >= tournament.getMaxPlayers()) {
            throw new IllegalStateException("O torneio está cheio.");
        }
        tournament.getPlayers().computeIfAbsent(playerId,
                id -> new TournamentPlayer(id, safeName(name, "Jogador"), false));
        return toState(tournament);
    }

    public synchronized TournamentStateResponse getState(String code) {
        return toState(requireTournament(code));
    }

    public synchronized TournamentStateResponse start(String code, String hostId) {
        Tournament tournament = requireTournament(code);
        if (!hostId.equals(tournament.getHostId())) {
            throw new IllegalStateException("Apenas o organizador pode iniciar o torneio.");
        }
        if (tournament.getStatus() != TournamentStatus.LOBBY) {
            throw new IllegalStateException("O torneio já foi iniciado.");
        }
        if (tournament.getPlayers().size() < 2) {
            throw new IllegalStateException("São necessários ao menos 2 jogadores.");
        }
        if (tournament.getThemeId() == null) {
            throw new IllegalStateException("Selecione um quiz antes de iniciar.");
        }

        List<String> ids = new ArrayList<>(tournament.getPlayers().keySet());
        Collections.shuffle(ids, random);

        int bracketSize = Integer.highestOneBit(Math.max(1, ids.size() - 1)) * 2;
        while (bracketSize < ids.size()) bracketSize *= 2;
        List<String> padded = new ArrayList<>(ids);
        while (padded.size() < bracketSize) padded.add(null);

        List<Match> firstRound = new ArrayList<>();
        for (int i = 0; i < padded.size(); i += 2) {
            firstRound.add(new Match(UUID.randomUUID().toString(), padded.get(i), padded.get(i + 1)));
        }

        tournament.getRounds().add(firstRound);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        launchRoundMatches(tournament, firstRound);
        advanceRoundIfComplete(tournament, firstRound);

        return toState(tournament);
    }

    // ---------- internos ----------

    /** Cria a sala (GameRoom) de cada confronto real (bye já tem vencedor, não precisa de sala). */
    private void launchRoundMatches(Tournament tournament, List<Match> round) {
        for (Match match : round) {
            if (match.getStatus() != MatchStatus.PENDING) continue;

            String player1Name = tournament.getPlayers().get(match.getPlayer1Id()).getName();
            RoomStateResponse room = gameRoomService.createRoom(new CreateRoomRequest(
                    match.getPlayer1Id(), player1Name, tournament.getThemeId(), tournament.getMatchConfig()
            ));
            match.setRoomCode(room.code());
            match.setStatus(MatchStatus.WAITING_PLAYERS);
        }
    }

    private void pollActiveMatches() {
        synchronized (this) {
            for (Tournament tournament : tournaments.values()) {
                if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) continue;
                tournament.touch();
                List<Match> currentRound = tournament.getRounds().get(tournament.getRounds().size() - 1);

                for (Match match : currentRound) {
                    if (match.getStatus() != MatchStatus.WAITING_PLAYERS && match.getStatus() != MatchStatus.IN_PROGRESS) {
                        continue;
                    }
                    RoomStateResponse state;
                    try {
                        state = gameRoomService.getState(match.getRoomCode());
                    } catch (NoSuchElementException e) {
                        // Sala sumiu (ex.: abandono) sem terminar — nada a fazer, fica travado
                        // nesse confronto (visível no bracket) até o organizador criar outro torneio.
                        continue;
                    }
                    if ("FINISHED".equals(state.status())) {
                        String winnerId = state.players().stream()
                                .max(Comparator.comparingInt(RoomStateResponse.PlayerView::score))
                                .map(RoomStateResponse.PlayerView::id)
                                .orElse(null);
                        match.setWinnerId(winnerId);
                        match.setStatus(MatchStatus.DONE);
                    } else if ("IN_QUESTION".equals(state.status()) || "BETWEEN".equals(state.status())) {
                        match.setStatus(MatchStatus.IN_PROGRESS);
                    }
                }

                advanceRoundIfComplete(tournament, currentRound);
            }
        }
    }

    private void advanceRoundIfComplete(Tournament tournament, List<Match> round) {
        boolean allDone = round.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.DONE || m.getStatus() == MatchStatus.BYE);
        if (!allDone) return;

        List<String> winners = round.stream().map(Match::getWinnerId).filter(Objects::nonNull).toList();

        // Marca perdedores como eliminados (quem não é vencedor e participou de fato).
        for (Match m : round) {
            String loser = null;
            if (m.getPlayer1Id() != null && !m.getPlayer1Id().equals(m.getWinnerId())) loser = m.getPlayer1Id();
            if (m.getPlayer2Id() != null && !m.getPlayer2Id().equals(m.getWinnerId())) loser = m.getPlayer2Id();
            if (loser != null) {
                TournamentPlayer player = tournament.getPlayers().get(loser);
                if (player != null) player.setEliminated(true);
            }
        }

        if (winners.size() <= 1) {
            tournament.setStatus(TournamentStatus.FINISHED);
            tournament.setChampionId(winners.isEmpty() ? null : winners.get(0));
            return;
        }

        List<Match> nextRound = new ArrayList<>();
        for (int i = 0; i < winners.size(); i += 2) {
            nextRound.add(new Match(UUID.randomUUID().toString(), winners.get(i), winners.get(i + 1)));
        }
        tournament.getRounds().add(nextRound);
        launchRoundMatches(tournament, nextRound);
    }

    private void sweepIdleTournaments() {
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(IDLE_TIMEOUT_MINUTES);
        synchronized (this) {
            tournaments.entrySet().removeIf(entry -> entry.getValue().getLastActivityMillis() < cutoff);
        }
    }

    private TournamentStateResponse toState(Tournament tournament) {
        List<TournamentStateResponse.PlayerView> players = tournament.getPlayers().values().stream()
                .map(p -> new TournamentStateResponse.PlayerView(p.getId(), p.getName(), p.isHost(), p.isEliminated()))
                .toList();

        List<List<TournamentStateResponse.MatchView>> rounds = tournament.getRounds().stream()
                .map(round -> round.stream()
                        .map(m -> new TournamentStateResponse.MatchView(
                                m.getId(), m.getPlayer1Id(), m.getPlayer2Id(), m.getWinnerId(),
                                m.getRoomCode(), m.getStatus().name()))
                        .toList())
                .toList();

        return new TournamentStateResponse(
                tournament.getCode(), tournament.getHostId(), tournament.getName(),
                tournament.getThemeId(), tournament.getThemeName(), tournament.getStatus().name(),
                players, rounds, tournament.getChampionId()
        );
    }

    private Tournament requireTournament(String code) {
        Tournament tournament = tournaments.get(code);
        if (tournament == null) throw new NoSuchElementException("Torneio não encontrado: " + code);
        tournament.touch();
        return tournament;
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (tournaments.containsKey(code));
        return code;
    }

    private String safeName(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        return name.length() > 30 ? name.substring(0, 30) : name.trim();
    }
}
