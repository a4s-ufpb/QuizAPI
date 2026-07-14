# Teste de carga — modo multiplayer

Simula várias salas jogando partidas completas ao mesmo tempo, usando o mesmo
protocolo do navegador (STOMP sobre WebSocket, endpoint SockJS `/ws/websocket`),
e mede latências e consumo de CPU/RAM da API.

## O que cada arquivo faz

| Arquivo | Função |
|---|---|
| `compose.loadtest.yaml` (raiz do repo) | Override do docker-compose que limita a API a **2 vCPUs e 2,5 GB de RAM** (heap Java em 1,75 GB) |
| `loadtest/loadtest.js` | Cliente de carga em Node: cria salas via REST, conecta host + jogadores via STOMP, joga a partida inteira e imprime as métricas |
| `loadtest/run-scenario.ps1` | Wrapper PowerShell: roda um cenário e amostra CPU/memória do container durante o teste |
| `loadtest/package.json` | Dependências do cliente de carga (`@stomp/stompjs` e `ws`) |

## Pré-requisitos

- Docker Desktop rodando
- Node.js 18+ (usa `fetch` nativo)

## Passo a passo

```powershell
# 1. Subir o stack com a API limitada a 2 CPUs / 2.5 GB
cd QuizAPI
docker compose -f docker-compose.yaml -f compose.loadtest.yaml up -d --build db minio quiz-api

# 2. Popular o banco (cria o tema "Seed - Geografia" com 10 questões e o admin)
./seed.ps1

# 3. Instalar as dependências do cliente de carga (uma vez)
cd loadtest
npm install

# 4. Rodar um cenário (com amostragem de CPU/RAM do container)
./run-scenario.ps1 -Rooms 10 -Players 12 -Questions 6
```

Parâmetros do `run-scenario.ps1`:

- `-Rooms` — número de salas simultâneas
- `-Players` — jogadores por sala, sem contar o host (acima de 12 o script loga
  como admin `admin@quizapp.com` / `Admin@123` do seed, que permite até 48)
- `-Questions` — questões por partida
- `-Timeout` — teto em ms por sala (default 240000)

O `loadtest.js` também roda direto, sem amostrar o container:

```bash
node loadtest.js --rooms 10 --players 12 --questions 6 --url http://localhost:8080
```

## Métricas reportadas

- **join→STATE** — tempo entre enviar o `join` e receber o primeiro estado da sala
- **entrega QUESTION** — atraso entre o `startAt` do servidor e o recebimento da
  questão no cliente (latência real de broadcast)
- **answer→RESULT** — tempo entre a resposta enviada e o resultado da questão;
  inclui a espera pelos demais jogadores (respostas simuladas em 0,3–2,5 s),
  então ~1 s é o esperado, não latência do servidor
- **desconexões / erros** — quedas de WebSocket e eventos `ERROR` recebidos
- **CPU/RAM do container** — pico observado durante o cenário (via `docker stats`)

## Resultados de referência (2 vCPU / 2,5 GB, jul/2026)

| Cenário | Conexões | Entrega questão p95 | CPU pico | RAM pico | Erros |
|---|---|---|---|---|---|
| 1×12 | 13 | 7 ms | 12% | 765 MiB | 0 |
| 20×12 | 260 | 5 ms | 90% | 851 MiB | 0 |
| 80×12 | 1.040 | 9 ms | 85% | 1.020 MiB | 0 |
| 1×48 | 49 | 16 ms | 102% | 813 MiB | 0 |
| 10×48 | 490 | 18 ms | 77% | 925 MiB | 0 |
| 150×12 | 1.950 | 20 ms | 111% | 1.060 MiB | 0 |

(CPU: 200% = 2 cores cheios.)

Conclusões: o gargalo é CPU, não memória; ~150 salas simultâneas (~2 mil
conexões) rodam com folga, teto prático estimado em 250–300 salas. Salas
grandes custam proporcionalmente mais que várias pequenas — cada join/ready no
lobby faz broadcast do estado completo pra sala inteira (O(n²)).

## Limpeza

```powershell
docker compose down          # mantém volumes (banco/minio)
docker compose down -v      # apaga também os dados
```
