# 🚀 Guide de déploiement — KanbanApp

## Informations de configuration

| Élément          | Valeur                                          |
|------------------|-------------------------------------------------|
| Login ENT        | `e22406953`                                     |
| Serveur          | `info-tpsi.univ-brest.fr`                       |
| Registry GitLab  | `gitlab-depinfo.univ-brest.fr:5050`             |
| Port Spring      | **10010** → interne 8080                        |
| Port Deno        | **10011** → interne 8000                        |
| BDD MariaDB      | `obiwan.univ-brest.fr:3306/e22406953_db1`       |
| BDD MongoDB      | `obiwan.univ-brest.fr:27017/e22406953_db1`      |

---

## Méthode 1 — Pipeline automatique (recommandé)

### Étape 1 : Créer le projet sur GitLab

1. Aller sur https://gitlab-depinfo.univ-brest.fr
2. Se connecter avec le login ENT
3. Créer un projet : **`e22406953/kanban`**
4. Dans *Settings → CI/CD → Variables*, activer le Container Registry

### Étape 2 : Pousser le code

```bash
git init
git remote add origin https://gitlab-depinfo.univ-brest.fr/e22406953/kanban.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

→ Le pipeline `.gitlab-ci.yml` se déclenche automatiquement et construit + pousse les 2 images.

### Étape 3 : Déployer sur le serveur

```bash
# Connexion SSH
ssh e22406953@info-tpsi.univ-brest.fr

# Se connecter au registry GitLab
docker login gitlab-depinfo.univ-brest.fr:5050

# Récupérer le docker-compose.yml
# (soit le copier via scp, soit le créer directement sur le serveur)

# Lancer la stack
docker compose up -d

# Vérifier que les conteneurs tournent
docker compose ps
docker compose logs -f
```

---

## Méthode 2 — Build manuel en local

Si le pipeline GitLab ne fonctionne pas, on peut tout faire en local.

### Prérequis
- Docker installé sur votre machine
- Accès au registry GitLab

### Build et push Spring

```bash
cd spring/

# Build de l'image
docker build -t gitlab-depinfo.univ-brest.fr:5050/e22406953/kanban/spring:latest .

# Login registry
docker login gitlab-depinfo.univ-brest.fr:5050

# Push
docker push gitlab-depinfo.univ-brest.fr:5050/e22406953/kanban/spring:latest
```

### Build et push Deno

```bash
cd deno/

# Build de l'image
docker build -t gitlab-depinfo.univ-brest.fr:5050/e22406953/kanban/deno:latest .

# Push
docker push gitlab-depinfo.univ-brest.fr:5050/e22406953/kanban/deno:latest
```

### Déploiement sur le serveur

```bash
# Copier le docker-compose.yml sur le serveur
scp docker-compose.yml e22406953@info-tpsi.univ-brest.fr:~/kanban/

# Se connecter
ssh e22406953@info-tpsi.univ-brest.fr

# Aller dans le dossier
cd ~/kanban/

# Se connecter au registry et lancer
docker login gitlab-depinfo.univ-brest.fr:5050
docker compose pull
docker compose up -d
```

---

## Commandes utiles sur le serveur

```bash
# Voir l'état des conteneurs
docker compose ps

# Voir les logs en temps réel
docker compose logs -f

# Voir les logs d'un seul service
docker compose logs -f spring
docker compose logs -f deno

# Redémarrer un service
docker compose restart spring
docker compose restart deno

# Mettre à jour après un nouveau push
docker compose pull
docker compose up -d

# Arrêter tout
docker compose down
```

---

## Vérification du déploiement

Une fois déployé, tester les endpoints :

```bash
# Test Spring (directement)
curl http://info-tpsi.univ-brest.fr:10010/api/utilisateurs

# Test Deno (auth + proxy)
curl -X POST http://info-tpsi.univ-brest.fr:10011/auth/login \
  -H "Content-Type: application/json" \
  -d '{"pseudo":"test","password":"test"}'
```

---

## Frontend — Connexion au serveur déployé

Le frontend Vue tourne en local (`npm run dev`).  
Le fichier `.env.production` est déjà configuré pour pointer vers le serveur :

```
VITE_API_URL=http://info-tpsi.univ-brest.fr:10011
```

Pour utiliser le serveur déployé pendant le développement, éditer `.env` :

```
VITE_API_URL=http://info-tpsi.univ-brest.fr:10011
```

---

## Architecture des conteneurs

```
Navigateur
    │
    │ HTTP
    ▼
┌─────────────────────────────────────────┐
│  info-tpsi.univ-brest.fr                │
│                                         │
│  ┌──────────────────┐                  │
│  │  kanban-deno     │  port 10011       │
│  │  (Deno + JWT)    │                  │
│  └────────┬─────────┘                  │
│           │ HTTP interne (spring:8080)   │
│  ┌────────▼─────────┐                  │
│  │  kanban-spring   │  port 10010       │
│  │  (Spring Boot)   │                  │
│  └──────────────────┘                  │
│                                         │
└─────────────────────────────────────────┘
         │                    │
         │ JDBC               │ MongoDB
         ▼                    ▼
  obiwan.univ-brest.fr  obiwan.univ-brest.fr
  (MariaDB :3306)        (MongoDB :27017)
```
