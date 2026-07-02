# QuizAPI — Contexto do Projeto

Backend do **Quiz A4S**, API REST + WebSocket do projeto de extensão
**Apps4Society** (UFPB Campus IV — Rio Tinto). Serve o frontend `QuizA4S-Front`
(React/TS). Ver o contexto do frontend em `../QuizA4S-Front/docs/CONTEXT.md`.

## Stack

| Item          | Tecnologia                                             |
| ------------- | ------------------------------------------------------ |
| Linguagem     | Java 21                                                |
| Framework     | Spring Boot 3.2.3                                       |
| Build         | Maven (`mvnw`), artefato final `QuizAPI.jar`           |
| Persistência  | Spring Data JPA / Hibernate                            |
| Banco         | PostgreSQL (dev/prod), H2 (test)                        |
| Segurança     | Spring Security + JWT (`com.auth0:java-jwt`)           |
| Tempo real    | Spring WebSocket + STOMP + SockJS                      |
| Docs          | springdoc-openapi (Swagger UI)                         |
| Testes        | spring-boot-starter-test, spring-security-test, REST-assured |

Group `com.ronyelison`, artifact `quiz`. Deploy via Docker (`Dockerfile`,
`docker-compose*.yaml`).

## Como rodar

```
./mvnw spring-boot:run            # sobe a API (perfil ativo = APP_PROFILE, default 'test' = H2)
./mvnw test                       # testes
./mvnw clean package              # gera target/QuizAPI.jar
```

Perfis (`src/main/resources/`): `application.yaml` (base, seleciona perfil via
`APP_PROFILE`), `application-dev.yaml` (Postgres via `DEV_*`), `application-prod.yaml`,
`application-test.yaml` (H2). Variáveis chave: `APP_SECRET` (assinatura JWT),
`APP_VERSION`, `DEV_USER/DEV_HOST/DEV_DB/DEV_PASSWORD`.

## Estrutura de pastas

Pacote base: `br.ufpb.dcx.apps4society.quizapi`

```
src/main/java/.../quizapi/
├── QuizApplication.java          # main (@SpringBootApplication)
├── config/
│   ├── SecurityConfig.java       # filter chain, CORS, stateless, rotas públicas
│   ├── WebSocketConfig.java      # STOMP: broker /topic, prefixo /app, endpoint /ws (SockJS)
│   ├── WebConfig.java            # CORS MVC (allowedOrigins *)
│   └── SwaggerConfig.java
├── controller/                   # @RestController REST + @Controller WebSocket
│   ├── UserController, ThemeController, QuestionController, AlternativeController
│   ├── ResponseController, ScoreController, StatisticController
│   ├── RoomController            # REST: create/delete/find room
│   ├── RoomWebSocketController   # STOMP: join/quit/select-quiz/start-quiz
│   └── exception/                # @RestControllerAdvice + DTOs de erro
├── dto/                          # records de request/response, por recurso
│   ├── user/, theme/, question/, alternative/, response/, score/, statistic/
│   ├── room/ (Player, RoomRequest, RoomResponse) + room/ws/ (payloads STOMP)
│   └── message/ (QuizMessage)
├── entity/                       # @Entity JPA (tb_*) + enums/ (Role)
│   ├── User, Theme, Question, Alternative, Response, Score,
│   ├── StatisticPerConclusion, Room
├── repository/                   # interfaces JpaRepository
├── security/
│   ├── SecurityFilter.java       # OncePerRequestFilter: lê Bearer, popula SecurityContext
│   └── TokenProvider.java        # gera/valida JWT
├── service/                      # regras de negócio (@Service)
│   └── exception/                # exceções de domínio (mapeadas p/ HTTP no advice)
└── util/Messages.java
src/main/resources/               # application*.yaml, banner.txt
src/test/java/...                 # controller/, service/, mock/, util/
```

## Arquitetura em camadas

`Controller` → `Service` → `Repository` → `Entity` (+ `DTO` records nas bordas).

- **Controllers** (`@RestController`, base `/v1/<recurso>`): validam entrada
  (`@Valid`), recebem JWT via `@RequestHeader("Authorization")`, delegam ao
  service, retornam `ResponseEntity<DTOResponse>`. Anotados com `@Operation`
  (Swagger). Listagens retornam `Page<T>` do Spring Data (`page`, `size`, `name`,
  `direction`).
- **Services** (`@Service`): regra de negócio, transações, e conversão
  entity↔DTO. Lançam exceções de domínio de `service/exception`.
- **Entities**: métodos `entityToResponse()` convertem para o DTO. Tabelas
  prefixadas `tb_` (ex. `tb_user`, `tb_theme`, `tb_room`).
- **DTOs**: `record`s Java. Convenção `XRequest` (entrada), `XResponse` (saída),
  `XUpdate` (patch), `XMinResponse` (resumido).

## Padrões do projeto

### Tratamento de erros

- `ErroExceptionHandler` (`@RestControllerAdvice`) mapeia cada exceção de domínio
  para um status HTTP e devolve `ErroResponse { timestamp, status, error, path }`.
  Validação de bean → `ValidationErro` (400) com `FieldMessage[]`.
- Convenção: novas regras lançam uma exceção específica em `service/exception/` e
  ganham um handler correspondente. Mensagens em português (ver `util/Messages`).

### Segurança / Autenticação

- JWT stateless. `SecurityFilter` extrai `Bearer <token>`, resolve o usuário
  (`UserDetailsServiceImpl`) e autentica no `SecurityContext`.
- `User` implementa `UserDetails`; `username` = email; roles `USER`/`ADMIN`
  (`entity/enums/Role`). `userNotHavePermission(user)` centraliza checagem de dono.
- Rotas **públicas** (`SecurityConfig`): `/`, Swagger, `/h2/**`, **`/ws/**`**,
  `GET /v1/theme/**`, `GET /v1/question/quiz/**`, `POST /v1/user/**`,
  `POST /v1/statistic`. Todo o resto exige autenticação.
- Senhas com `BCryptPasswordEncoder`.

### Base de URL

Controllers usam base **`/v1`**. O frontend chama `/api/v1` — o proxy reverso
(nginx) mapeia `/api` → raiz da API em produção. Swagger em `pathsToMatch: /v1/**`.

## Conceitos de domínio

- **User**: dono de themes/questions; joga quizzes; role USER/ADMIN.
- **Theme** (`tb_theme`): agrupa Questions (o "quiz" para o usuário). Tem ranking
  de Scores.
- **Question** (`tb_question`): título, imagem, pertence a um Theme, tem até
  **4 Alternatives** (`MAXIMUM_NUMBER_OF_ALTERNATIVES`).
- **Alternative** (`tb_alternative`): texto + `correct` (boolean). Regras: no
  máximo 4, exatamente uma correta.
- **Response** (`tb_response`): resposta de um usuário a uma questão (modo
  single-player logado). Base das estatísticas.
- **Score** (`tb_score`): pontuação por tema → ranking.
- **StatisticPerConclusion**: % de acertos por conclusão de quiz.

## Multiplayer / WebSocket (estado atual)

Infra STOMP já existente, base para o modo multiplayer:

- **`WebSocketConfig`**: broker simples em `/topic`, prefixo de app `/app`,
  endpoint STOMP `/ws` com SockJS e origens liberadas.
- **`RoomController`** (REST `/v1/room`): `POST` cria sala, `DELETE
  /{roomId}/{creatorId}` fecha (só o criador), `GET /{roomId}` busca.
- **`RoomWebSocketController`** (destinos `/app/*`): `join-room`, `quit-room`,
  `select-quiz`, `start-quiz`. Cada ação persiste no banco e faz broadcast do
  `RoomResponse` atualizado para **`/topic/room/{roomId}`**.
- **`Room`** (`tb_room`): `creator` (User), `selectedQuizId`, `started`,
  `players` (ManyToMany com User). `RoomResponse { roomId, creator,
  selectedQuizId, started, users[] }`, `Player { playerId, name }`.
- **`QuizMessage { type, content, sender }`**: DTO de mensagem (scaffold de chat,
  ainda sem mapeamento).

### Limitações atuais (a evoluir para o modo Kahoot)

O modelo atual **exige login** (players são `User` do banco) e **persiste salas
no banco**. O modo multiplayer estilo Kahoot pede: jogadores convidados (sem
login), estado de sala/jogo **em memória** (respostas não salvas no banco),
modos individual/equipe, chat em tempo real, sistema de "pronto", expulsão de
jogadores pelo líder, troca de tema pelo líder e configuração de regras da
partida. Ver `docs/MULTIPLAYER.md` (design do modo multiplayer) quando disponível.
