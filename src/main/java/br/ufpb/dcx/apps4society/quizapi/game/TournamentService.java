package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateRoomRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateTournamentRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.GameConfig;
import br.ufpb.dcx.apps4society.quizapi.game.dto.RoomStateResponse;
import br.ufpb.dcx.apps4society.quizapi.game.dto.TournamentStateResponse;
import br.ufpb.dcx.apps4society.quizapi.game.model.*;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Torneios eliminatórios em memória — mesmo padrão do {@link GameRoomService}
 * (nada persistido). Cada confronto vira uma sala de multiplayer própria do
 * torneio: TODOS os presentes jogam e pontuam (config.hostPlays), o avanço das
 * questões é por tempo (AdvanceMode.AUTO, sem intermediário) e a partida começa
 * sozinha assim que os dois adversários se conectam (auto-início).
 *
 * <p>Fluxo: LOBBY (entra/sai livre) → o organizador trava as chaves quando há
 * 4/8/16 jogadores (CONFIGURING) e escolhe o quiz de cada rodada → inicia
 * (IN_PROGRESS) → FINISHED. O serviço observa (polling) cada sala e avança o
 * vencedor para a próxima rodada.
 */
@Service
public class TournamentService {
    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long IDLE_TIMEOUT_MINUTES = 60;
    /** Tamanhos de chaveamento válidos (potências de 2 sem "bye"). */
    private static final Set<Integer> VALID_BRACKET_SIZES = Set.of(4, 8, 16);

    private final Map<String, Tournament> tournaments = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final GameRoomService gameRoomService;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TournamentService(GameRoomService gameRoomService, ThemeRepository themeRepository,
                             UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.gameRoomService = gameRoomService;
        this.themeRepository = themeRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.scheduler.scheduleAtFixedRate(this::pollActiveMatches, 3, 3, TimeUnit.SECONDS);
        this.scheduler.scheduleAtFixedRate(this::sweepIdleTournaments, 10, 10, TimeUnit.MINUTES);
    }

    private String topic(String code) {
        return "/topic/tournament/" + code;
    }

    /** Empurra o estado atual do torneio para os inscritos via STOMP (substitui o polling do cliente). */
    private void broadcast(Tournament tournament) {
        messagingTemplate.convertAndSend(topic(tournament.getCode()),
                Map.of("type", "STATE", "data", toState(tournament)));
    }

    /** Sinaliza aos inscritos que o torneio foi encerrado/removido. */
    private void broadcastClosed(String code) {
        messagingTemplate.convertAndSend(topic(code), Map.of("type", "CLOSED"));
    }

    /**
     * Ao finalizar, remove o torneio da memória após 60s. Evita que salas de
     * campeões deixadas abertas no navegador fiquem ocupando o servidor até a
     * varredura de ociosidade. O cliente exibe a contagem regressiva equivalente.
     */
    private void scheduleClose(String code) {
        scheduler.schedule(() -> {
            synchronized (this) {
                if (tournaments.remove(code) != null) {
                    broadcastClosed(code);
                }
            }
        }, 60, TimeUnit.SECONDS);
    }

    public synchronized TournamentStateResponse createTournament(CreateTournamentRequest request) {
        String code = generateUniqueCode();
        String hostId = request.hostId() != null ? request.hostId() : UUID.randomUUID().toString();

        // Config aplicada a cada sala de confronto: todos jogam (hostPlays) e o
        // avanço é por tempo (AUTO) — sem líder-apresentador nem controle manual.
        GameConfig matchConfig = new GameConfig(
                RoomMode.INDIVIDUAL, ScoringMode.SPEED, AdvanceMode.AUTO,
                request.questionTimeSeconds(), request.questionCount(), null, GameStyle.NORMAL, 2, true
        ).withDefaults();

        // Torneio não tem mais quiz único: o quiz é escolhido por rodada na fase
        // de configuração. themeId no matchConfig fica null aqui.
        Tournament tournament = new Tournament(code, hostId, safeName(request.name(), "Torneio"), null, matchConfig);
        int max = request.maxPlayers() != null ? Math.max(4, Math.min(16, request.maxPlayers())) : 8;
        tournament.setMaxPlayers(max);

        TournamentPlayer hostPlayer = new TournamentPlayer(hostId, safeName(request.hostName(), "Host"), true);
        applyCosmetics(hostPlayer, request.hostUuid());
        tournament.getPlayers().put(hostId, hostPlayer);
        tournaments.put(code, tournament);
        return toState(tournament);
    }

    public synchronized TournamentStateResponse join(String code, String playerId, String name, String userUuid) {
        Tournament tournament = requireTournament(code);
        if (tournament.getStatus() != TournamentStatus.LOBBY) {
            throw new IllegalStateException("O torneio já foi travado pelo organizador — não é possível entrar agora.");
        }
        boolean isNew = !tournament.getPlayers().containsKey(playerId);
        if (isNew && tournament.getPlayers().size() >= tournament.getMaxPlayers()) {
            throw new IllegalStateException("O torneio está cheio.");
        }
        TournamentPlayer player = tournament.getPlayers().computeIfAbsent(playerId,
                id -> new TournamentPlayer(id, safeName(name, "Jogador"), false));
        applyCosmetics(player, userUuid);
        broadcast(tournament);
        return toState(tournament);
    }

    /** Organizador remove um jogador (apenas no lobby). */
    public synchronized TournamentStateResponse kick(String code, String hostId, String targetId) {
        Tournament tournament = requireTournament(code);
        if (!hostId.equals(tournament.getHostId())) {
            throw new IllegalStateException("Apenas o organizador pode remover jogadores.");
        }
        if (tournament.getStatus() != TournamentStatus.LOBBY) {
            throw new IllegalStateException("Só é possível remover jogadores no lobby.");
        }
        if (targetId.equals(tournament.getHostId())) {
            throw new IllegalStateException("O organizador não pode se remover.");
        }
        tournament.getPlayers().remove(targetId);
        broadcast(tournament);
        return toState(tournament);
    }

    /** Jogador sai do torneio (lobby). Se o organizador sair, o torneio é encerrado. */
    public synchronized TournamentStateResponse leave(String code, String playerId) {
        Tournament tournament = tournaments.get(code);
        if (tournament == null) throw new NoSuchElementException("Torneio não encontrado: " + code);
        if (playerId.equals(tournament.getHostId())) {
            tournaments.remove(code);
            broadcastClosed(code);
            return toState(tournament);
        }
        if (tournament.getStatus() == TournamentStatus.LOBBY) {
            tournament.getPlayers().remove(playerId);
        }
        broadcast(tournament);
        return toState(tournament);
    }

    /**
     * Trava as chaves: exige 4/8/16 jogadores. A partir daqui ninguém entra e o
     * organizador escolhe o quiz de cada rodada antes de iniciar.
     */
    public synchronized TournamentStateResponse configure(String code, String hostId) {
        Tournament tournament = requireTournament(code);
        if (!hostId.equals(tournament.getHostId())) {
            throw new IllegalStateException("Apenas o organizador pode travar as chaves.");
        }
        if (tournament.getStatus() != TournamentStatus.LOBBY) {
            throw new IllegalStateException("As chaves já foram travadas.");
        }
        int count = tournament.getPlayers().size();
        if (!VALID_BRACKET_SIZES.contains(count)) {
            throw new IllegalStateException(
                    "O torneio precisa ter exatamente 4, 8 ou 16 jogadores para travar as chaves (atual: " + count + ").");
        }

        tournament.setBracketSize(count);
        int rounds = Integer.numberOfTrailingZeros(count); // log2(count)
        tournament.getRoundThemeIds().clear();
        tournament.getRoundThemeNames().clear();
        for (int i = 0; i < rounds; i++) {
            tournament.getRoundThemeIds().add(null);
            tournament.getRoundThemeNames().add(null);
        }
        tournament.setStatus(TournamentStatus.CONFIGURING);
        broadcast(tournament);
        return toState(tournament);
    }

    /** Reabre o lobby (volta de CONFIGURING para LOBBY, descartando os temas escolhidos). */
    public synchronized TournamentStateResponse reopen(String code, String hostId) {
        Tournament tournament = requireTournament(code);
        if (!hostId.equals(tournament.getHostId())) {
            throw new IllegalStateException("Apenas o organizador pode reabrir o lobby.");
        }
        if (tournament.getStatus() != TournamentStatus.CONFIGURING) {
            throw new IllegalStateException("O lobby só pode ser reaberto durante a configuração.");
        }
        tournament.setBracketSize(0);
        tournament.getRoundThemeIds().clear();
        tournament.getRoundThemeNames().clear();
        tournament.setStatus(TournamentStatus.LOBBY);
        broadcast(tournament);
        return toState(tournament);
    }

    /** Define o quiz de uma rodada do chaveamento (fase de configuração). */
    public synchronized TournamentStateResponse setRoundTheme(String code, String hostId, int roundIndex, Long themeId) {
        Tournament tournament = requireTournament(code);
        if (!hostId.equals(tournament.getHostId())) {
            throw new IllegalStateException("Apenas o organizador pode escolher os quizzes.");
        }
        if (tournament.getStatus() != TournamentStatus.CONFIGURING) {
            throw new IllegalStateException("Só é possível escolher os quizzes durante a configuração.");
        }
        if (roundIndex < 0 || roundIndex >= tournament.getRoundThemeIds().size()) {
            throw new IllegalStateException("Rodada inválida.");
        }
        Theme theme = themeId != null ? themeRepository.findById(themeId).orElse(null) : null;
        tournament.getRoundThemeIds().set(roundIndex, theme != null ? themeId : null);
        tournament.getRoundThemeNames().set(roundIndex, theme != null ? theme.getName() : null);
        broadcast(tournament);
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
        if (tournament.getStatus() != TournamentStatus.CONFIGURING) {
            throw new IllegalStateException("Trave as chaves e escolha os quizzes antes de iniciar.");
        }
        if (tournament.getRoundThemeIds().stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("Escolha o quiz de todas as rodadas antes de iniciar.");
        }

        List<String> ids = new ArrayList<>(tournament.getPlayers().keySet());
        Collections.shuffle(ids, random);

        List<Match> firstRound = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += 2) {
            firstRound.add(new Match(UUID.randomUUID().toString(), ids.get(i), ids.get(i + 1)));
        }

        tournament.getRounds().add(firstRound);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        launchRoundMatches(tournament, firstRound, tournament.getRoundThemeIds().get(0));

        broadcast(tournament);
        return toState(tournament);
    }

    // ---------- internos ----------

    /** Cria a sala (GameRoom) de cada confronto real da rodada com o quiz informado. */
    private void launchRoundMatches(Tournament tournament, List<Match> round, Long themeId) {
        for (Match match : round) {
            if (match.getStatus() != MatchStatus.PENDING) continue;

            String player1Name = tournament.getPlayers().get(match.getPlayer1Id()).getName();
            RoomStateResponse room = gameRoomService.createTournamentRoom(new CreateRoomRequest(
                    match.getPlayer1Id(), player1Name, themeId, tournament.getMatchConfig()
            ), 2);
            match.setRoomCode(room.code());
            match.setStatus(MatchStatus.WAITING_PLAYERS);
        }
    }

    private void pollActiveMatches() {
        synchronized (this) {
            for (Tournament tournament : tournaments.values()) {
                if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) continue;
                tournament.touch();
                int roundIndex = tournament.getRounds().size() - 1;
                List<Match> currentRound = tournament.getRounds().get(roundIndex);
                boolean changed = false;

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
                        // Vencedor = maior pontuação entre TODOS os participantes da sala
                        // (inclui o jogador que também é líder — hostPlays no torneio).
                        String winnerId = state.players().stream()
                                .max(Comparator.comparingInt(RoomStateResponse.PlayerView::score))
                                .map(RoomStateResponse.PlayerView::id)
                                .orElse(null);
                        match.setWinnerId(winnerId);
                        match.setStatus(MatchStatus.DONE);
                        changed = true;
                    } else if ("IN_QUESTION".equals(state.status()) || "BETWEEN".equals(state.status())) {
                        if (match.getStatus() != MatchStatus.IN_PROGRESS) changed = true;
                        match.setStatus(MatchStatus.IN_PROGRESS);
                    }
                }

                boolean advanced = advanceRoundIfComplete(tournament, currentRound, roundIndex);
                if (changed || advanced) {
                    broadcast(tournament);
                }
            }
        }
    }

    /** @return true se a rodada foi encerrada (nova rodada criada ou torneio finalizado). */
    private boolean advanceRoundIfComplete(Tournament tournament, List<Match> round, int roundIndex) {
        boolean allDone = round.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.DONE || m.getStatus() == MatchStatus.BYE);
        if (!allDone) return false;

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
            scheduleClose(tournament.getCode());
            return true;
        }

        List<Match> nextRound = new ArrayList<>();
        for (int i = 0; i < winners.size(); i += 2) {
            nextRound.add(new Match(UUID.randomUUID().toString(), winners.get(i), winners.get(i + 1)));
        }
        tournament.getRounds().add(nextRound);
        int nextRoundIndex = roundIndex + 1;
        Long nextTheme = nextRoundIndex < tournament.getRoundThemeIds().size()
                ? tournament.getRoundThemeIds().get(nextRoundIndex) : null;
        launchRoundMatches(tournament, nextRound, nextTheme);
        return true;
    }

    private void sweepIdleTournaments() {
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(IDLE_TIMEOUT_MINUTES);
        synchronized (this) {
            tournaments.entrySet().removeIf(entry -> {
                boolean idle = entry.getValue().getLastActivityMillis() < cutoff;
                if (idle) broadcastClosed(entry.getKey());
                return idle;
            });
        }
    }

    /** Copia os cosméticos equipados da conta real (só visual). Convidado -> nada. */
    private void applyCosmetics(TournamentPlayer player, String userUuid) {
        if (userUuid == null || userUuid.isBlank()) return;
        try {
            User user = userRepository.findById(java.util.UUID.fromString(userUuid)).orElse(null);
            if (user == null) return;
            player.setTitle(user.getEquippedTitle());
            player.setFrame(user.getEquippedFrame());
            player.setBanner(user.getEquippedBanner());
            player.setFont(user.getEquippedFont());
            player.setNameStyle(user.getEquippedNameStyle());
            player.setNameEffect(user.getEquippedNameEffect());
        } catch (IllegalArgumentException ignored) {
            // userUuid inválido (ex.: convidado) — sem cosméticos.
        }
    }

    private TournamentStateResponse toState(Tournament tournament) {
        List<TournamentStateResponse.PlayerView> players = tournament.getPlayers().values().stream()
                .map(p -> new TournamentStateResponse.PlayerView(p.getId(), p.getName(), p.isHost(), p.isEliminated(),
                        p.getTitle(), p.getFrame(), p.getBanner(), p.getFont(), p.getNameStyle(), p.getNameEffect()))
                .toList();

        List<List<TournamentStateResponse.MatchView>> rounds = tournament.getRounds().stream()
                .map(round -> round.stream()
                        .map(m -> new TournamentStateResponse.MatchView(
                                m.getId(), m.getPlayer1Id(), m.getPlayer2Id(), m.getWinnerId(),
                                m.getRoomCode(), m.getStatus().name()))
                        .toList())
                .toList();

        int totalRounds = tournament.getRoundThemeIds().size();
        List<TournamentStateResponse.RoundThemeView> roundThemes = new ArrayList<>();
        for (int i = 0; i < totalRounds; i++) {
            roundThemes.add(new TournamentStateResponse.RoundThemeView(
                    i, roundLabel(i, totalRounds),
                    tournament.getRoundThemeIds().get(i), tournament.getRoundThemeNames().get(i)));
        }

        return new TournamentStateResponse(
                tournament.getCode(), tournament.getHostId(), tournament.getName(),
                tournament.getThemeId(), tournament.getThemeName(), tournament.getStatus().name(),
                players, rounds, tournament.getChampionId(),
                tournament.getMaxPlayers(), tournament.getBracketSize(), roundThemes
        );
    }

    private String roundLabel(int index, int totalRounds) {
        int fromEnd = totalRounds - index;
        if (fromEnd == 1) return "Final";
        if (fromEnd == 2) return "Semifinal";
        if (fromEnd == 3) return "Quartas de final";
        if (fromEnd == 4) return "Oitavas de final";
        return "Rodada " + (index + 1);
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
