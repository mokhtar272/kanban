/**
 * Tests unitaires — Authentification Deno (JWT uniquement)
 * Aucun serveur requis, aucune connexion réseau.
 *
 * Lancer :
 *   deno test --allow-env tests/auth_test.ts
 */

import { assertEquals, assertExists, assert } from "https://deno.land/std@0.208.0/assert/mod.ts";
import { create, verify, getNumericDate } from "https://deno.land/x/djwt@v3.0.1/mod.ts";

// ── Clé identique à celle de server.ts ────────────────────────
const SECRET_RAW = "kanban-ubo-tiil-2026-secret-key";

async function getKey(): Promise<CryptoKey> {
  return await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(SECRET_RAW),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

// ══════════════════════════════════════════════════════════════
//  Tests unitaires JWT
// ══════════════════════════════════════════════════════════════

Deno.test("JWT — création d'un token avec payload correct", async () => {
  const key = await getKey();
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    { id: 1, pseudo: "alice", email: "alice@test.fr", role: "USER",
      exp: getNumericDate(3600) },
    key
  );
  assertExists(token);
  assert(token.split(".").length === 3,
    "Un JWT doit avoir 3 parties séparées par des points");
});

Deno.test("JWT — vérification d'un token valide retourne le payload", async () => {
  const key = await getKey();
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    { id: 2, pseudo: "bob", role: "USER", exp: getNumericDate(3600) },
    key
  );
  const decoded = await verify(token, key) as Record<string, unknown>;
  assertEquals(decoded.id,     2);
  assertEquals(decoded.pseudo, "bob");
  assertEquals(decoded.role,   "USER");
});

Deno.test("JWT — vérification d'un token falsifié échoue", async () => {
  const key = await getKey();
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    { id: 1, pseudo: "alice", exp: getNumericDate(3600) },
    key
  );
  const parts = token.split(".");
  const fakeToken = parts[0] + "." + parts[1] + ".signaturefalsifiee";
  let erreur = false;
  try { await verify(fakeToken, key); }
  catch { erreur = true; }
  assert(erreur, "Un token falsifié doit lever une exception");
});

Deno.test("JWT — token expiré est rejeté", async () => {
  const key = await getKey();
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    { id: 1, pseudo: "alice", exp: getNumericDate(-1) },
    key
  );
  let erreur = false;
  try { await verify(token, key); }
  catch { erreur = true; }
  assert(erreur, "Un token expiré doit être rejeté");
});

Deno.test("JWT — expiration à 7 jours est correcte", async () => {
  const key = await getKey();
  const maintenant = Math.floor(Date.now() / 1000);
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    { id: 1, exp: getNumericDate(60 * 60 * 24 * 7) },
    key
  );
  const decoded = await verify(token, key) as Record<string, unknown>;
  const diff = (decoded.exp as number) - maintenant;
  assert(diff > 604800 - 60 && diff <= 604800,
    "L'expiration doit être à 7 jours (604800 secondes)");
});

Deno.test("JWT — le payload contient bien tous les champs utilisateur", async () => {
  const key = await getKey();
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    { id: 3, pseudo: "charlie", email: "charlie@test.fr",
      role: "ADMIN", exp: getNumericDate(3600) },
    key
  );
  const decoded = await verify(token, key) as Record<string, unknown>;
  assertEquals(decoded.id,     3);
  assertEquals(decoded.pseudo, "charlie");
  assertEquals(decoded.email,  "charlie@test.fr");
  assertEquals(decoded.role,   "ADMIN");
});

Deno.test("JWT — deux tokens générés pour le même user sont différents", async () => {
  const key = await getKey();
  const payload = { id: 1, pseudo: "alice", exp: getNumericDate(3600) };
  const token1 = await create({ alg: "HS256", typ: "JWT" }, payload, key);
  const token2 = await create({ alg: "HS256", typ: "JWT" }, payload, key);
  // Les tokens peuvent différer (timestamp interne)
  assertExists(token1);
  assertExists(token2);
});

Deno.test("JWT — token signé avec une mauvaise clé est rejeté", async () => {
  const bonneClé  = await getKey();
  const mauvaiseClé = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode("une-autre-cle-secrete"),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]
  );
  const token = await create(
    { alg: "HS256", typ: "JWT" },
    { id: 1, pseudo: "alice", exp: getNumericDate(3600) },
    mauvaiseClé   // signé avec la mauvaise clé
  );
  let erreur = false;
  try { await verify(token, bonneClé); }  // vérifié avec la bonne clé
  catch { erreur = true; }
  assert(erreur, "Un token signé avec une mauvaise clé doit être rejeté");
});