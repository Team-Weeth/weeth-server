#!/usr/bin/env node
/**
 * 왁뿌볼 부하 점검 봇. 봇들이 복도를 걸어 다니고 가끔 표현을 보내면서, 연결당 받은 상태 메시지 크기·개수를 잰다.
 * 상태 메시지 크기는 같은 월드 인원 수에 비례하므로 걷기만으로도 실제 송신량을 재현한다.
 *
 *   node infra/load/wax-bots.mjs --url ws://127.0.0.1:8087/ws/wax --bots 12 --seconds 20
 *   node infra/load/wax-bots.mjs --bots 12 --mode seated   # 강의실에 걸어 들어가 앉아서 누르기를 반복(최악 페이로드)
 *   node infra/load/wax-bots.mjs --url wss://api-dev.v4.weeth.kr/ws/wax --bots 30 --origin https://dev.v4.weeth.kr
 *
 * 한 머신에서 띄운 봇은 모두 같은 IP라 서버의 IP당 연결 한도(weeth.wax.max-connections-per-ip)를
 * 점검 동안만 봇 수 이상으로 올려야 한다. Node 22 이상(내장 WebSocket)이 필요하다.
 */

const args = Object.fromEntries(
  process.argv
    .slice(2)
    .join(' ')
    .split('--')
    .filter(Boolean)
    .map((pair) => pair.trim().split(/\s+/)),
);
const url = args.url ?? 'ws://127.0.0.1:8087/ws/wax';
const bots = Number(args.bots ?? 12);
const seconds = Number(args.seconds ?? 20);
const origin = args.origin;
const rampMs = Number(args.ramp ?? 100);
// walk: 복도 왕복 + 표현. seated: 문으로 강의실에 들어가 앉은 뒤 누르기를 반복한다(seat·pressing이 실려 메시지가 가장 크다).
const mode = args.mode ?? 'walk';

const EMOTES = ['wave', 'laugh', 'thumbs', 'shh', 'help'];
const stats = [];
const errors = new Map();

function spawn(index) {
  return new Promise((resolve) => {
    // 브라우저처럼 Origin을 보내야 서버의 origin 허용 목록 검사를 같이 점검할 수 있다(Node 22 undici 확장 옵션).
    const ws = new WebSocket(url, origin ? { headers: { Origin: origin } } : undefined);
    const stat = { index, bytes: 0, states: 0, welcome: false, players: 0, closed: null };
    stats.push(stat);
    let x = 0;
    let z = 4.5;
    let direction = Math.random() < 0.5 ? -1 : 1;
    let timers = [];
    let me = null;
    ws.onopen = () => ws.send(JSON.stringify({ type: 'join' }));
    ws.onmessage = (event) => {
      const text = String(event.data);
      const message = JSON.parse(text);
      if (message.type === 'welcome' && mode === 'seated') {
        stat.welcome = true;
        resolve();
        stat.layout = message.layout;
        timers = [setInterval(() => ws.send(JSON.stringify({ type: 'heartbeat' })), 10000)];
      } else if (message.type === 'welcome') {
        stat.welcome = true;
        resolve();
        timers = [
          // 브라우저와 같은 80ms 간격으로 걷는다(초당 약 3.2m).
          setInterval(() => {
            x += direction * 0.25;
            if (Math.abs(x) > 3) direction *= -1;
            ws.send(JSON.stringify({ type: 'move', x, z, yaw: direction > 0 ? 1.57 : -1.57 }));
          }, 80),
          setInterval(() => {
            ws.send(JSON.stringify({ type: 'emote', kind: EMOTES[index % EMOTES.length] }));
          }, 4000 + (index % 7) * 300),
          setInterval(() => ws.send(JSON.stringify({ type: 'heartbeat' })), 10000),
        ];
      } else if (message.type === 'state') {
        stat.bytes += Buffer.byteLength(text);
        stat.states += 1;
        stat.players = message.players.length;
        stat.seated = message.you.status !== 'walking';
        if (stat.welcome && stat.states === 1) z = message.you.z;
        if (mode === 'seated' && !me && stat.layout) {
          me = message.you;
          void sitDown(ws, stat, index, me, timers);
        }
      } else if (message.type === 'error') {
        const key = message.code ?? message.message;
        errors.set(key, (errors.get(key) ?? 0) + 1);
        resolve();
      }
    };
    ws.onclose = (event) => {
      stat.closed = event.code;
      timers.forEach(clearInterval);
      resolve();
    };
    ws.onerror = () => resolve();
    stat.ws = ws;
  });
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** 서버 이동 검증(한 번에 1.5m, 벽은 문으로만)을 지키며 0.5m씩 걷는다. 초당 메시지 한도(60)를 넘지 않게 20ms 간격. */
async function walk(ws, from, to) {
  const steps = Math.ceil(Math.hypot(to.x - from.x, to.z - from.z) / 0.5);
  for (let i = 1; i <= steps; i++) {
    const t = i / steps;
    ws.send(JSON.stringify({ type: 'move', x: from.x + (to.x - from.x) * t, z: from.z + (to.z - from.z) * t, yaw: 0 }));
    await sleep(20);
  }
  return to;
}

async function sitDown(ws, stat, index, me, timers) {
  const rooms = stat.layout.rooms;
  const room = rooms[index % rooms.length];
  const seat = room.seats[Math.floor(index / rooms.length) % room.seats.length];
  let at = { x: me.x, z: me.z };
  for (const point of [
    { x: at.x, z: 1 },
    { x: room.doorX, z: 1 },
    { x: room.doorX, z: -1 },
    { x: seat.x + 1.5, z: -1 },
    { x: seat.x + 1.5, z: seat.z },
  ])
    at = await walk(ws, at, point);
  ws.send(JSON.stringify({ type: 'sit', room: room.id, seat: seat.index }));
  // 2초마다 0.4초씩 누른다. 소음이 그사이 빠져서 대부분 들키지 않고 앉아 있는다(seat가 계속 실린다).
  timers.push(
    setInterval(() => {
      ws.send(JSON.stringify({ type: 'press', active: true }));
      setTimeout(() => ws.send(JSON.stringify({ type: 'press', active: false })), 400);
    }, 2000),
  );
}

for (let i = 0; i < bots; i++) {
  void spawn(i);
  await new Promise((r) => setTimeout(r, rampMs));
}
// 램프업(착석 모드는 강의실까지 걷기)이 끝난 뒤부터 측정한다.
await new Promise((r) => setTimeout(r, mode === 'seated' ? 6000 : 1500));
stats.forEach((stat) => {
  stat.bytes = 0;
  stat.states = 0;
});
const started = Date.now();
await new Promise((r) => setTimeout(r, seconds * 1000));
const elapsed = (Date.now() - started) / 1000;

const joined = stats.filter((stat) => stat.welcome && stat.closed === null);
const perConnection = joined.map((stat) => stat.bytes / elapsed / 1024);
const average = perConnection.reduce((sum, value) => sum + value, 0) / Math.max(1, joined.length);
const max = Math.max(0, ...perConnection);
const statesPerSecond = joined.reduce((sum, stat) => sum + stat.states, 0) / Math.max(1, joined.length) / elapsed;
const totalKbps = perConnection.reduce((sum, value) => sum + value, 0);

console.log(
  JSON.stringify(
    {
      url,
      bots,
      mode,
      joined: joined.length,
      seatedAtEnd: joined.filter((stat) => stat.seated).length,
      playersSeenByLastBot: joined.at(-1)?.players ?? 0,
      statesPerSecond: Number(statesPerSecond.toFixed(1)),
      kbPerSecondPerConnection: { average: Number(average.toFixed(1)), max: Number(max.toFixed(1)) },
      totalKbPerSecond: Number(totalKbps.toFixed(1)),
      errors: Object.fromEntries(errors),
    },
    null,
    2,
  ),
);
stats.forEach((stat) => stat.ws?.close());
setTimeout(() => process.exit(0), 300);
