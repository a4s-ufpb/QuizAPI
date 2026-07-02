# Modo Multiplayer (Kahoot-style) — Backend

Motor de quiz multiplayer em tempo real, **totalmente em memória**. Jogadores são
**convidados (sem login)** e **nada de gameplay é persistido** no banco (nem
salas, nem respostas, nem pontuação). Convive com o sistema de sala legado
(`entity/Room` + `RoomController` + `RoomWebSocketController`), que permanece
intacto; o modo novo vive no pacote isolado `game/`.

## Componentes (pacote `game/`)

```
game/
├── model/            RoomMode, ScoringMode, AdvanceMode, RoomStatus,
│                     GamePlayer, Team, GameRoom       (estado em memória)
├── dto/              GameConfig, CreateRoomRequest, RoomStateResponse,
│                     QuestionView, QuestionResultView, ChatMessage, GameEvent
├── dto/ws/           GameMessages (payloads STOMP aninhados)
├── GameRoomService   motor: mapa de salas, timers, pontuação, broadcast
├── GameRoomController REST público (/v1/game)
└── GameWebSocketController  STOMP (/app/game/*)
```

`GameRoomService` guarda `Map<String,GameRoom>` e usa um
`ScheduledExecutorService` para o timer de fim de questão / avanço automático.
Todo método público é `synchronized` (monitor = instância do service);
tarefas agendadas revalidam o estado antes de agir.

## Identidade e persistência

- `playerId` é gerado no **cliente** (UUID) e enviado em cada mensagem.
- O host recebe `hostId = playerId` de quem criou a sala.
- Questões são carregadas do banco **uma vez** no início (snapshot destacado via
  `QuestionRepository.findByThemeId`), embaralhadas e limitadas a
  `questionCount`. Depois disso não há mais acesso ao banco durante a partida.

## Transporte

Reutiliza a infra STOMP existente (`WebSocketConfig`): endpoint `/ws` (SockJS),
app prefix `/app`, broker `/topic`.

### REST (`/v1/game`, público em `SecurityConfig`)

| Método | Rota                | Corpo / retorno                         |
| ------ | ------------------- | --------------------------------------- |
| POST   | `/v1/game/room`     | `CreateRoomRequest` → `RoomStateResponse` (201) |
| GET    | `/v1/game/room/{code}` | → `RoomStateResponse` (200) ou 404   |

### STOMP — envio: `/app/game/{ação}`

| Destino                | Payload (`GameMessages.*`)                  | Quem |
| ---------------------- | ------------------------------------------- | ---- |
| `/app/game/join`       | `Join(code, playerId, name)`                | todos |
| `/app/game/leave`      | `PlayerRef(code, playerId)`                 | todos |
| `/app/game/ready`      | `Ready(code, playerId, ready)`              | jogador |
| `/app/game/team`       | `TeamPick(code, playerId, teamId)`          | jogador |
| `/app/game/kick`       | `Kick(code, hostId, targetId)`              | líder |
| `/app/game/change-quiz`| `ChangeQuiz(code, hostId, themeId)`         | líder |
| `/app/game/config`     | `ConfigUpdate(code, hostId, config)`        | líder |
| `/app/game/chat`       | `Chat(code, playerId, content)`             | todos |
| `/app/game/start`      | `HostAction(code, hostId)`                  | líder |
| `/app/game/answer`     | `Answer(code, playerId, alternativeId)`     | jogador |
| `/app/game/next`       | `HostAction(code, hostId)`                  | líder |

### STOMP — recepção: `/topic/room/{code}`

Toda mensagem é um envelope **`GameEvent { type, data }`**:

| `type`        | `data`                | Quando |
| ------------- | --------------------- | ------ |
| `STATE`       | `RoomStateResponse`   | qualquer mudança de sala |
| `QUESTION`    | `QuestionView`        | início de cada questão (SEM gabarito) |
| `RESULT`      | `QuestionResultView`  | fim da questão (gabarito + placar) |
| `CHAT`        | `ChatMessage`         | nova mensagem |
| `KICKED`      | `String playerId`     | jogador expulso (cliente compara com o seu id) |
| `ROOM_CLOSED` | `String code`         | host saiu / sala encerrada |
| `ERROR`       | `String mensagem`     | validação falhou |

## Máquina de estados (`RoomStatus`)

```
LOBBY ──start──▶ IN_QUESTION ──(tempo esgotou | todos responderam)──▶ BETWEEN
                     ▲                                                    │
                     └──────────── next / auto ◀───(não é a última)──────┘
                                                     │
                                                (última) ▼
                                                     FINISHED
```

- **LOBBY**: entra/sai, pronto, escolhe equipe; líder troca quiz, muda regras,
  expulsa e inicia. `start` exige quiz selecionado e **todos prontos** (o host
  conta como sempre pronto).
- **IN_QUESTION**: broadcast de `QUESTION`. Fecha quando o tempo esgota (timer) ou
  quando todos responderam. Cada jogador responde uma vez.
- **BETWEEN**: broadcast de `RESULT`. `AdvanceMode.HOST` aguarda `next`;
  `AUTO` agenda o avanço após 5s.
- **FINISHED**: `STATE` final; o placar sai do próprio estado.

## Configuração da partida (`GameConfig`)

| Campo                | Valores / default                         |
| -------------------- | ----------------------------------------- |
| `roomMode`           | `INDIVIDUAL` (default) \| `TEAM`          |
| `scoringMode`        | `SPEED` (default) \| `FIXED`              |
| `advanceMode`        | `HOST` (default) \| `AUTO`                |
| `questionTimeSeconds`| 5–120, default 20                         |
| `questionCount`      | 1–30, default 10                          |

No modo `TEAM` são criadas duas equipes padrão (`Equipe 1`, `Equipe 2`) e cada
jogador escolhe a sua; a pontuação da equipe é a soma dos membros.

## Pontuação

- **SPEED**: acerto vale de 1000 (imediato) a 500 (no fim do tempo), linear pela
  fração de tempo decorrido; erro = 0.
- **FIXED**: acerto = 1000; erro = 0.

## Segurança

`/v1/game/**` e `/ws/**` são públicos (`SecurityConfig`). As mensagens STOMP não
passam pelo filtro JWT — a autorização de líder é feita comparando `hostId` com o
`hostId` da sala dentro do service.

## Limitações conhecidas / próximos passos

- Salas vivem só na memória da instância → **não escala horizontalmente** (uma
  instância). Para múltiplas instâncias, trocar o broker simples por um broker
  STOMP externo (RabbitMQ/Redis) e um store de sala compartilhado.
- Sem limpeza automática de salas ociosas/finalizadas (adicionar TTL/varredura).
- Reconexão do cliente reassina o tópico, mas questões perdidas não são
  reenviadas (o próximo `STATE`/`QUESTION` ressincroniza).
