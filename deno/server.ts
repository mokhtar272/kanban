import { Application, Router, Context } from "https://deno.land/x/oak@v12.6.1/mod.ts";
import { create, verify, getNumericDate } from "https://deno.land/x/djwt@v3.0.1/mod.ts";

const PORT       = 8000;
// En production Docker, SPRING_URL pointe vers le conteneur Spring via son nom de service
const SPRING_URL = Deno.env.get("SPRING_URL") ?? "http://localhost:8080";
const SECRET_RAW = Deno.env.get("JWT_SECRET")  ?? "kanban-ubo-tiil-2026-secret-key";

async function getKey(): Promise<CryptoKey> {
  return await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(SECRET_RAW),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]
  );
}

async function signToken(payload: Record<string, unknown>): Promise<string> {
  return await create({ alg: "HS256", typ: "JWT" },
    { ...payload, exp: getNumericDate(60 * 60 * 24 * 7) }, await getKey()); // 7 jours
}

async function verifyToken(token: string): Promise<Record<string, unknown> | null> {
  try { return await verify(token, await getKey()) as Record<string, unknown>; }
  catch { return null; }
}

async function authMiddleware(ctx: Context, next: () => Promise<unknown>) {
  const h = ctx.request.headers.get("Authorization");
  if (!h?.startsWith("Bearer ")) {
    ctx.response.status = 401;
    ctx.response.body   = { success: false, message: "Token manquant" };
    return;
  }
  const payload = await verifyToken(h.slice(7));
  if (!payload) {
    ctx.response.status = 401;
    ctx.response.body   = { success: false, message: "Token invalide ou expiré" };
    return;
  }
  ctx.state.user = payload;
  await next();
}

async function proxy(ctx: Context, path: string) {
  const url    = SPRING_URL + path + (ctx.request.url.search || "");
  const method = ctx.request.method;
  const heads: Record<string, string> = { "Content-Type": "application/json" };
  if (ctx.state.user) {
    heads["X-User-Id"]    = String(ctx.state.user.id    ?? "");
    heads["X-User-Pseudo"]= String(ctx.state.user.pseudo ?? "");
    heads["X-User-Role"]  = String(ctx.state.user.role  ?? "");
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
  } catch (e) {
    ctx.response.status = 502;
    ctx.response.body   = JSON.stringify({ success: false, message: "Backend Spring indisponible" });
  }
}

const router = new Router();

// ── Auth (public) ─────────────────────────────────────────────
router.post("/auth/register", async (ctx) => {
  const body = await ctx.request.body({ type: "json" }).value;
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
  const body = await ctx.request.body({ type: "json" }).value;
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

// ── Routes protégées ─────────────────────────────────────────
router.all("/api/(.*)", authMiddleware, async (ctx) => {
  const path = ctx.request.url.pathname;
  if (path.startsWith("/api/admin") && ctx.state.user?.role !== "ADMIN") {
    ctx.response.status = 403;
    ctx.response.body   = JSON.stringify({ success: false, message: "Accès réservé aux admins" });
    return;
  }
  await proxy(ctx, path);
});

const app = new Application();

// CORS global
app.use(async (ctx, next) => {
  ctx.response.headers.set("Access-Control-Allow-Origin",  "*");
  ctx.response.headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
  ctx.response.headers.set("Access-Control-Allow-Headers", "Content-Type,Authorization");
  if (ctx.request.method === "OPTIONS") { ctx.response.status = 204; return; }
  await next();
});

app.use(router.routes());
app.use(router.allowedMethods());

console.log(`\n🦕 Deno Kanban Server`);
console.log(`   Port   : http://localhost:${PORT}`);
console.log(`   Spring : ${SPRING_URL}`);
console.log(`\n   POST /auth/register   — Inscription`);
console.log(`   POST /auth/login      — Connexion → JWT`);
console.log(`   /api/**               — Proxy sécurisé vers Spring\n`);
await app.listen({ port: PORT });
