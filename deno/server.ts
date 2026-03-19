import { Application, Router, Context } from "https://deno.land/x/oak@v12.6.1/mod.ts";
import { create, verify, getNumericDate } from "https://deno.land/x/djwt@v3.0.1/mod.ts";

const PORT       = 8000;
const SPRING_URL = Deno.env.get("SPRING_URL") ?? "http://localhost:8080";
const SECRET_RAW = Deno.env.get("JWT_SECRET")  ?? "kanban-ubo-tiil-2026-secret-key";

// ── WebSocket : boardId → Set<WebSocket> ──────────────────────
const boardClients = new Map<number, Set<WebSocket>>();

function joinBoard(boardId: number, ws: WebSocket) {
  if (!boardClients.has(boardId)) boardClients.set(boardId, new Set());
  boardClients.get(boardId)!.add(ws);
}

function leaveBoard(boardId: number, ws: WebSocket) {
  boardClients.get(boardId)?.delete(ws);
}

function broadcastToBoard(boardId: number, event: Record<string, unknown>, excludeWs?: WebSocket) {
  const clients = boardClients.get(boardId);
  if (!clients) return;
  const payload = JSON.stringify(event);
  for (const ws of clients) {
    if (ws === excludeWs) continue;
    if (ws.readyState === WebSocket.OPEN) {
      try { ws.send(payload); } catch { /* client déconnecté */ }
    }
  }
}

// ── JWT ───────────────────────────────────────────────────────
async function getKey(): Promise<CryptoKey> {
  return await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(SECRET_RAW),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]
  );
}

async function signToken(payload: Record<string, unknown>): Promise<string> {
  return await create(
    { alg: "HS256", typ: "JWT" },
    { ...payload, exp: getNumericDate(60 * 60 * 24 * 7) }, // 7 jours
    await getKey()
  );
}

async function verifyToken(token: string): Promise<Record<string, unknown> | null> {
  try { return await verify(token, await getKey()) as Record<string, unknown>; }
  catch { return null; }
}

// ── Auth middleware ───────────────────────────────────────────
async function authMiddleware(ctx: Context, next: () => Promise<unknown>) {
  const h = ctx.request.headers.get("Authorization");
  if (!h?.startsWith("Bearer ")) {
    ctx.response.status = 401;
    ctx.response.body   = { success: false, message: "Token manquant ou format invalide" };
    return;
  }
  const payload = await verifyToken(h.slice(7));
  if (!payload) {
    ctx.response.status = 401;
    ctx.response.body   = { success: false, message: "Token invalide ou expiré, veuillez vous reconnecter" };
    return;
  }
  ctx.state.user = payload;
  await next();
}

// ── Proxy vers Spring ─────────────────────────────────────────
async function proxy(ctx: Context, path: string) {
  const url    = SPRING_URL + path + (ctx.request.url.search || "");
  const method = ctx.request.method;
  const heads: Record<string, string> = { "Content-Type": "application/json" };
  if (ctx.state.user) {
    heads["X-User-Id"]     = String(ctx.state.user.id    ?? "");
    heads["X-User-Pseudo"] = String(ctx.state.user.pseudo ?? "");
    heads["X-User-Role"]   = String(ctx.state.user.role   ?? "");
  }
  let body: string | undefined;
  if (["POST","PUT","PATCH"].includes(method)) {
    try { body = await ctx.request.body({ type: "text" }).value; } catch { /**/ }
  }
  try {
    const res  = await fetch(url, { method, headers: heads, body });
    const text = await res.text();
    ctx.response.status = res.status;
    ctx.response.headers.set("Content-Type", "application/json");
    ctx.response.body   = text;

    // Après une mutation réussie, on broadcast sur WS
    if (["POST","PUT","PATCH","DELETE"].includes(method) && res.ok) {
      const boardIdMatch = path.match(/\/api\/(?:taches|colonnes|commentaires).*/) ||
                           path.match(/\/api\/tableaux\/(\d+)/);
      // Extraire boardId depuis les headers envoyés par le client WS
      const boardIdStr = ctx.request.headers.get("X-Board-Id");
      if (boardIdStr) {
        const boardId = parseInt(boardIdStr);
        if (!isNaN(boardId)) {
          broadcastToBoard(boardId, {
            type:   "BOARD_UPDATED",
            action: method,
            path,
            by:     ctx.state.user?.pseudo ?? "unknown",
          });
        }
      }
    }
  } catch (_e) {
    ctx.response.status = 502;
    ctx.response.body   = JSON.stringify({
      success: false,
      message: "Le serveur backend est indisponible, veuillez réessayer dans quelques instants"
    });
  }
}

const router = new Router();

// ── Auth public ───────────────────────────────────────────────
router.post("/auth/register", async (ctx) => {
  let body: Record<string, unknown>;
  try {
    body = await ctx.request.body({ type: "json" }).value;
  } catch {
    ctx.response.status = 400;
    ctx.response.body   = { success: false, message: "Corps de requête JSON invalide" };
    return;
  }
  if (!body.pseudo || !body.email || !body.password) {
    ctx.response.status = 400;
    ctx.response.body   = { success: false, message: "Pseudo, email et mot de passe sont requis" };
    return;
  }
  const res  = await fetch(`${SPRING_URL}/api/utilisateurs/register`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await res.json();
  if (!data.success) { ctx.response.status = 400; ctx.response.body = data; return; }
  const user  = data.data;
  const token = await signToken({ id: user.id, pseudo: user.pseudo, email: user.email, role: user.role });
  ctx.response.body = { success: true, token, user };
});

router.post("/auth/login", async (ctx) => {
  let body: Record<string, unknown>;
  try {
    body = await ctx.request.body({ type: "json" }).value;
  } catch {
    ctx.response.status = 400;
    ctx.response.body   = { success: false, message: "Corps de requête JSON invalide" };
    return;
  }
  if (!body.pseudo || !body.password) {
    ctx.response.status = 400;
    ctx.response.body   = { success: false, message: "Pseudo et mot de passe sont requis" };
    return;
  }
  const res  = await fetch(`${SPRING_URL}/api/utilisateurs/verify`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await res.json();
  if (!data.success) { ctx.response.status = 401; ctx.response.body = data; return; }
  const user  = data.data;
  const token = await signToken({ id: user.id, pseudo: user.pseudo, email: user.email, role: user.role });
  ctx.response.body = { success: true, token, user };
});

// ── WebSocket : /ws/board/:id?token=... ───────────────────────
router.get("/ws/board/:id", async (ctx) => {
  const boardId = parseInt(ctx.params.id);
  if (isNaN(boardId)) { ctx.response.status = 400; return; }

  // Auth via query param (les WS ne peuvent pas envoyer de header Authorization)
  const token = ctx.request.url.searchParams.get("token");
  if (!token) { ctx.response.status = 401; ctx.response.body = "Token manquant"; return; }
  const payload = await verifyToken(token);
  if (!payload) { ctx.response.status = 401; ctx.response.body = "Token invalide"; return; }

  if (!ctx.isUpgradable) { ctx.response.status = 400; ctx.response.body = "WebSocket requis"; return; }

  const ws = ctx.upgrade();
  joinBoard(boardId, ws);

  ws.onopen = () => {
    ws.send(JSON.stringify({ type: "CONNECTED", boardId, pseudo: payload.pseudo }));
  };

  ws.onmessage = (evt) => {
    // Le client peut envoyer un ping pour maintenir la connexion
    try {
      const msg = JSON.parse(evt.data);
      if (msg.type === "PING") ws.send(JSON.stringify({ type: "PONG" }));
    } catch { /* ignore */ }
  };

  ws.onclose = () => { leaveBoard(boardId, ws); };
  ws.onerror = () => { leaveBoard(boardId, ws); };
});

// ── Routes protégées /api/* ───────────────────────────────────
router.all("/api/(.*)", authMiddleware, async (ctx) => {
  const path = ctx.request.url.pathname;

  // Vérification rôle admin
  if (path.startsWith("/api/admin") && ctx.state.user?.role !== "ADMIN") {
    ctx.response.status = 403;
    ctx.response.body   = JSON.stringify({
      success: false,
      message: "Accès réservé aux administrateurs"
    });
    return;
  }

  await proxy(ctx, path);
});

// ── Application ───────────────────────────────────────────────
const app = new Application();

// CORS
app.use(async (ctx, next) => {
  ctx.response.headers.set("Access-Control-Allow-Origin",  "*");
  ctx.response.headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
  ctx.response.headers.set("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Board-Id");
  if (ctx.request.method === "OPTIONS") { ctx.response.status = 204; return; }
  await next();
});

// Gestion globale des erreurs Deno
app.use(async (ctx, next) => {
  try { await next(); }
  catch (err) {
    console.error("[Deno Error]", err);
    ctx.response.status = 500;
    ctx.response.body   = JSON.stringify({ success: false, message: "Erreur interne du serveur" });
  }
});

app.use(router.routes());
app.use(router.allowedMethods());

console.log(`\n🟢 Deno Kanban Server`);
console.log(`   Port   : http://localhost:${PORT}`);
console.log(`   Spring : ${SPRING_URL}`);
console.log(`\n   POST /auth/register       — Inscription`);
console.log(`   POST /auth/login           — Connexion → JWT`);
console.log(`   GET  /ws/board/:id?token=  — WebSocket temps réel`);
console.log(`   /api/**                    — Proxy sécurisé vers Spring\n`);

await app.listen({ port: PORT });
