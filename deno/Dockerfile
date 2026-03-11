# ── Image Deno officielle ──────────────────────────────────────
# Contexte de build = deno/  (passer ./deno dans la commande docker build)
FROM denoland/deno:2.2.3

WORKDIR /app

# Copier le serveur
COPY server.ts ./
COPY deno.json ./

# Pré-télécharger les dépendances (sans lockfile pour éviter les conflits de version)
RUN deno cache server.ts

EXPOSE 8000

# --allow-net  : accès réseau (écoute + appels vers Spring)
# --allow-env  : lecture des variables d'environnement
CMD ["deno", "run", "--allow-net", "--allow-env", "server.ts"]
