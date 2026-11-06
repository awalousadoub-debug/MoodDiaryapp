# Politique de sécurité - MoodDiaryapp

## Versions supportées

| Version | Supportée |
|---------|-----------|
| v1.0.0  | :white_check_mark: Oui |

## Signaler une vulnérabilité

Si vous découvrez une vulnérabilité de sécurité, **ne créez pas d'issue publique**.

Contactez directement le propriétaire du dépôt :

- **GitHub :** @awalousadoub-debug
- **Dépôt :** https://github.com/awalousadoub-debug/MoodDiaryapp

Incluez dans votre rapport :
1. Une description détaillée de la vulnérabilité
2. Les étapes pour la reproduire
3. L'impact potentiel
4. Toute suggestion de correction

Le propriétaire accusera réception de votre rapport dans les plus brefs délais et vous tiendra informé de l'avancement de la correction.

## Bonnes pratiques de sécurité appliquées
- Aucun secret committé dans le dépôt (mots de passe, clés API, tokens).
- Utilisation de GitHub Secrets pour les variables sensibles.
- Revues de code obligatoires pour tout changement sur `main`.
- Branche `main` protégée contre les pushes directs et les force-pushes.
- CI/CD automatisée pour détecter les régressions et vulnérabilités.

---
*Propriétaire du dépôt : @awalousadoub-debug*