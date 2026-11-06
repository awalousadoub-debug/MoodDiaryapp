# MoodDiaryapp

Application de suivi d'humeur (Mood Diary) — Projet de développement professionnel.

## Structure du dépôt

| Branche | Rôle |
|---------|------|
| `main` | Branche de production protégée (code stable, revu et approuvé uniquement) |
| `backend` | Branche de développement principale backend |
| `frontend` | Branche de développement principale frontend |
| `develop` | Branche d'intégration (si requise) |
| `feature/*` | Branches de fonctionnalités individuelles |
| `bugfix/*` | Branches de correction de bugs |
| `hotfix/*` | Branches de correctifs urgents en production |

## Gouvernance

- **Propriétaire du dépôt :** @awalousadoub-debug (contrôle total, approbation finale sur `main`)
- **Aucun push direct sur `main`** — Pull Request obligatoire avec révision
- **CODEOWNERS :** Le propriétaire est réviseur obligatoire pour tous les fichiers

## Démarrage rapide

```bash
# Cloner le dépôt
git clone https://github.com/awalousadoub-debug/MoodDiaryapp.git
cd MoodDiaryapp

# Vérifier la branche actuelle
git branch --show-current

# Créer une branche de fonctionnalité
git checkout -b feature/nom-de-la-fonctionnalité
```

## Documentation

- [`CONTRIBUTING.md`](CONTRIBUTING.md) — Règles de contribution
- [`SECURITY.md`](SECURITY.md) — Politique de sécurité
- [`.github/CODEOWNERS`](.github/CODEOWNERS) — Propriété du code et révision
- [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) — Template de Pull Request

## Licence

Privé — Tous droits réservés au propriétaire du dépôt.
