# 📋 KanbanApp — Projet SI-SOR 2026

## 🏗 Architecture
```
Vue 3 (5173) → Deno/Oak (8000) → Spring Boot (8080)
                    ↓                    ↓
                JWT Auth            MariaDB obiwan:3306
                                    MongoDB obiwan:27017
```

## ⚙️ Configuration requise

### 1. Mettre à jour le mot de passe SQL dans `spring/src/main/resources/application.yml`
```yaml
spring.datasource.password: VOTRE_MOT_DE_PASSE
```

### 2. Base de données obiwan
La base `e22406953_db1` doit exister sur obiwan.
Lancez le script SQL : `kanban_db.sql`

## 🚀 Démarrage

```bash
# Terminal 1 — Spring Boot (port 8080)
cd spring
./gradlew bootRun

# Terminal 2 — Deno (port 8000)
cd deno
deno task dev

# Terminal 3 — Vue frontend (port 5173)
cd frontend
npm install
npm run dev
```

## 📁 Structure Spring (par module)
```
fr.ubo.kanban/
├── users/          → Utilisateurs (register, verify, CRUD)
├── boards/         → Tableaux + membres
├── columns/        → Colonnes (ordre, couleur)
├── tasks/          → Tâches + historique
├── comments/       → Commentaires MongoDB
├── admin/          → Stats + gestion admin
└── common/         → ApiResponse, CorsConfig
```

## 🔌 API REST (Spring — port 8080)

| Module    | Méthode | Route                              | Description            |
|-----------|---------|-----------------------------------|------------------------|
| users     | POST    | /api/utilisateurs/register        | Inscription            |
| users     | POST    | /api/utilisateurs/verify          | Vérif mot de passe     |
| users     | GET     | /api/utilisateurs/{id}            | Profil                 |
| boards    | GET     | /api/tableaux/mes/{userId}        | Mes tableaux           |
| boards    | POST    | /api/tableaux                     | Créer tableau          |
| boards    | PUT     | /api/tableaux/{id}                | Modifier tableau       |
| boards    | DELETE  | /api/tableaux/{id}                | Supprimer tableau      |
| boards    | POST    | /api/tableaux/{id}/inviter        | Inviter membre         |
| boards    | DELETE  | /api/tableaux/{id}/membres/{uid}  | Retirer membre         |
| columns   | GET     | /api/colonnes/tableau/{id}        | Colonnes d'un tableau  |
| columns   | POST    | /api/colonnes?tableauId=x         | Créer colonne          |
| columns   | PUT     | /api/colonnes/{id}                | Modifier colonne       |
| columns   | DELETE  | /api/colonnes/{id}                | Supprimer colonne      |
| tasks     | GET     | /api/taches/colonne/{id}          | Tâches d'une colonne   |
| tasks     | POST    | /api/taches                       | Créer tâche            |
| tasks     | PUT     | /api/taches/{id}                  | Modifier/déplacer      |
| tasks     | DELETE  | /api/taches/{id}                  | Supprimer tâche        |
| tasks     | GET     | /api/taches/search?tableauId&q    | Recherche              |
| tasks     | GET     | /api/taches/historique/{tableauId}| Historique             |
| comments  | GET     | /api/commentaires/tache/{id}      | Commentaires (MongoDB) |
| comments  | POST    | /api/commentaires?tacheId=x       | Ajouter commentaire    |
| comments  | DELETE  | /api/commentaires/{id}            | Supprimer commentaire  |
| admin     | GET     | /api/admin/stats                  | Statistiques           |
| admin     | GET     | /api/admin/utilisateurs           | Liste utilisateurs     |
| admin     | DELETE  | /api/admin/utilisateurs/{id}      | Supprimer utilisateur  |
