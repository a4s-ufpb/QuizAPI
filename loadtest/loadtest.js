/**
 * Teste de carga do modo multiplayer do Quiz A4S.
 *
 * Simula salas completas jogando partidas via STOMP sobre WebSocket cru
 * (mesmo protocolo do navegador, endpoint SockJS /ws/websocket), medindo:
 *  - latência de entrega da questão (recebimento - startAt do servidor)
 *  - latência resposta->RESULT (do último answer enviado localmente)
 *  - tempo de join (join enviado -> primeiro STATE)
 *  - erros, desconexões, partidas concluídas
 *
 * Uso: node loadtest.js --rooms 4 --players 12 --questions 6
 */
const { Client } = require("@stomp/stompjs");
const WebSocket = require("ws");
Object.assign(global, { WebSocket });

const args = Object.fromEntries(
  process.argv.slice(2).map((a, i, arr) =>
    a.startsWith("--") ? [a.slice(2), arr[i + 1]] : null
  ).filter(Boolean)
);

const BASE = args.url || "http://localhost:8080";
const WS_URL = BASE.replace("http", "ws") + "/ws/websocket";
const ROOMS = Number(args.rooms || 1);
const PLAYERS = Number(args.players || 12); // por sala, sem contar o host
const QUESTIONS = Number(args.questions || 6);
const QUESTION_TIME = Number(args.qtime || 20);
const ANSWER_DELAY_MIN = 300, ANSWER_DELAY_MAX = 2500;
const HOST_NEXT_DELAY = 700;
const SCENARIO_TIMEOUT_MS = Number(args.timeout || 240000);

let shuttingDownCount = 0; // clientes desativados de propósito não contam como queda
const metrics = {
  joinLatency: [], questionLatency: [], resultLatency: [],
  errors: [], disconnects: 0, roomsFinished: 0, questionsPlayed: 0,
  answersSent: 0,
};

function pct(arr, p) {
  if (arr.length === 0) return 0;
  const s = [...arr].sort((a, b) => a - b);
  return s[Math.min(s.length - 1, Math.floor((p / 100) * s.length))];
}
function stats(arr) {
  if (arr.length === 0) return "n=0";
  const avg = arr.reduce((a, b) => a + b, 0) / arr.length;
  return `n=${arr.length} avg=${avg.toFixed(0)}ms p50=${pct(arr, 50)}ms p95=${pct(arr, 95)}ms max=${Math.max(...arr)}ms`;
}
const rand = (min, max) => min + Math.random() * (max - min);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** Login admin (permite salas de até 48 jogadores). */
async function adminToken() {
  const res = await fetch(`${BASE}/v1/user/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "admin@quizapp.com", password: "Admin@123" }),
  });
  if (!res.ok) throw new Error(`login admin -> ${res.status}`);
  const body = await res.json();
  return body.token || body.accessToken;
}

async function fetchThemeId() {
  const res = await fetch(`${BASE}/v1/theme?page=0&size=50`);
  if (!res.ok) throw new Error(`GET /v1/theme -> ${res.status}`);
  const page = await res.json();
  const theme = (page.content || []).find((t) => t.name?.startsWith("Seed"));
  if (!theme) throw new Error("Tema seed não encontrado — rode seed.ps1");
  return theme.id;
}

function connectStomp() {
  return new Promise((resolve, reject) => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 0,
      heartbeatIncoming: 5000,
      heartbeatOutgoing: 5000,
      onConnect: () => resolve(client),
      onStompError: (f) => reject(new Error("STOMP error: " + f.headers?.message)),
      onWebSocketError: (e) => reject(new Error("WS error: " + (e?.message || e))),
      onWebSocketClose: () => {
        if (shuttingDownCount > 0) { shuttingDownCount--; return; }
        metrics.disconnects++;
      },
      debug: () => {},
    });
    client.activate();
  });
}

function send(client, action, body) {
  client.publish({ destination: `/app/game/${action}`, body: JSON.stringify(body) });
}

/** Um jogador simulado dentro de uma sala. */
async function runPlayer(code, index) {
  const playerId = `lt-${code}-p${index}-${Math.random().toString(36).slice(2, 8)}`;
  const client = await connectStomp();
  let answeredIndex = -1;
  let lastAnswerAt = 0;
  let joinedAt = 0;
  let sawFirstState = false;

  client.subscribe(`/topic/room/${code}`, (msg) => {
    let ev;
    try { ev = JSON.parse(msg.body); } catch { return; }
    const now = Date.now();
    switch (ev.type) {
      case "STATE":
        if (!sawFirstState && joinedAt) {
          sawFirstState = true;
          metrics.joinLatency.push(now - joinedAt);
        }
        break;
      case "QUESTION": {
        const q = ev.data;
        metrics.questionLatency.push(Math.max(0, now - q.startAt));
        if (q.index === answeredIndex) break;
        answeredIndex = q.index;
        const alt = q.alternatives[Math.floor(Math.random() * q.alternatives.length)];
        setTimeout(() => {
          lastAnswerAt = Date.now();
          metrics.answersSent++;
          send(client, "answer", { code, playerId, alternativeId: alt.id });
        }, rand(ANSWER_DELAY_MIN, ANSWER_DELAY_MAX));
        break;
      }
      case "RESULT":
        if (lastAnswerAt) metrics.resultLatency.push(now - lastAnswerAt);
        break;
      case "ERROR": {
        const data = ev.data;
        const msgTxt = typeof data === "object" ? data.message : String(data);
        const target = typeof data === "object" ? data.targetPlayerId : null;
        if (!target || target === playerId) metrics.errors.push(msgTxt);
        break;
      }
    }
  });

  joinedAt = Date.now();
  send(client, "join", { code, playerId, name: `Aluno ${index}` });
  await sleep(rand(100, 600));
  send(client, "ready", { code, playerId, ready: true });
  return { client, playerId };
}

/** Uma sala completa: cria via REST, host + N jogadores, joga a partida inteira. */
async function runRoom(roomIndex, themeId, token) {
  const hostId = `lt-host-${roomIndex}-${Math.random().toString(36).slice(2, 8)}`;
  const createRes = await fetch(`${BASE}/v1/game/room`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({
      hostId,
      hostName: `Prof ${roomIndex}`,
      themeId,
      config: {
        roomMode: "INDIVIDUAL", scoringMode: "SPEED", advanceMode: "HOST",
        questionTimeSeconds: QUESTION_TIME, questionCount: QUESTIONS,
        gameStyle: "NORMAL", maxPlayers: Math.max(12, PLAYERS),
      },
    }),
  });
  if (!createRes.ok) throw new Error(`create room -> ${createRes.status}`);
  const { code } = await createRes.json();

  const hostClient = await connectStomp();
  const players = [];

  const finished = new Promise((resolve) => {
    hostClient.subscribe(`/topic/room/${code}`, (msg) => {
      let ev;
      try { ev = JSON.parse(msg.body); } catch { return; }
      if (ev.type === "RESULT") {
        metrics.questionsPlayed++;
        setTimeout(() => send(hostClient, "next", { code, hostId }), HOST_NEXT_DELAY);
      } else if (ev.type === "STATE" && ev.data.status === "FINISHED") {
        metrics.roomsFinished++;
        resolve();
      } else if (ev.type === "ERROR") {
        const data = ev.data;
        const target = typeof data === "object" ? data.targetPlayerId : null;
        if (!target || target === hostId) {
          metrics.errors.push(typeof data === "object" ? data.message : String(data));
        }
      }
    });
  });

  send(hostClient, "join", { code, playerId: hostId, name: `Prof ${roomIndex}` });
  for (let i = 0; i < PLAYERS; i++) {
    players.push(await runPlayer(code, i));
    await sleep(rand(30, 120)); // entrada escalonada, como numa sala real
  }
  await sleep(1200);
  send(hostClient, "start", { code, hostId });

  await Promise.race([finished, sleep(SCENARIO_TIMEOUT_MS)]);

  send(hostClient, "leave", { code, playerId: hostId }); // fecha a sala
  await sleep(200);
  shuttingDownCount += players.length + 1;
  for (const p of players) p.client.deactivate();
  hostClient.deactivate();
}

(async () => {
  const startedAt = Date.now();
  console.log(`Cenário: ${ROOMS} sala(s) x ${PLAYERS} jogadores (+host) | ${QUESTIONS} questões | total conexões=${ROOMS * (PLAYERS + 1)}`);
  const themeId = await fetchThemeId();
  const token = PLAYERS > 12 ? await adminToken() : null;

  const rooms = [];
  for (let r = 0; r < ROOMS; r++) {
    rooms.push(runRoom(r, themeId, token).catch((e) => metrics.errors.push("room fail: " + e.message)));
    await sleep(400); // salas não começam exatamente juntas
  }
  await Promise.all(rooms);

  const durationS = ((Date.now() - startedAt) / 1000).toFixed(1);
  console.log(`--- RESULTADO (${durationS}s) ---`);
  console.log(`salas concluídas: ${metrics.roomsFinished}/${ROOMS} | questões jogadas: ${metrics.questionsPlayed} | respostas: ${metrics.answersSent}`);
  console.log(`join->STATE:      ${stats(metrics.joinLatency)}`);
  console.log(`entrega QUESTION: ${stats(metrics.questionLatency)}`);
  console.log(`answer->RESULT:   ${stats(metrics.resultLatency)}`);
  console.log(`desconexões: ${metrics.disconnects} | erros: ${metrics.errors.length}`);
  if (metrics.errors.length) {
    const grouped = {};
    for (const e of metrics.errors) grouped[e] = (grouped[e] || 0) + 1;
    for (const [m, c] of Object.entries(grouped)) console.log(`  [${c}x] ${m}`);
  }
  process.exit(0);
})().catch((e) => { console.error("FALHA:", e); process.exit(1); });
