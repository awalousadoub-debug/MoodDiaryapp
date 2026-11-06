# Guide de contribution - MoodDiaryapp

Merci de votre intérêt pour contribuer à MoodDiaryapp ! Ce document définit les règles et processus à suivre pour contribuer au projet de manière professionnelle et organisée.

## 1. Code de conduite
En contribuant, vous acceptez de respecter les autres membres de l'équipe et de suivre les règles établies par le propriétaire du dépôt.

## 2. Avant de commencer
- Assurez-vous d'avoir les permissions appropriées sur le dépôt.
- Consultez les issues existantes pour éviter les doublons.
- Créez une issue pour discuter de votre proposition avant de commencer le développement.

## 3. Workflow de contribution

### Étape 1 : Créer une branche
Ne travaillez jamais directement sur `main`. Créez une branche à partir de `develop` (ou de `main` si `develop` n'existe pas encore) :

```bash
git checkout develop
git pull origin develop
git checkout -b feature/nom-de-la-fonctionnalité
# ou
git checkout -b bugfix/description-du-bug
# ou
git checkout -b hotfix/description-du-correctif-urgent
```

### Étape 2 : Développer
Implémentez les changements sur votre branche. Suivez les conventions de commit ci-dessous.

### Étape 3 : Commiter
Utilisez des messages de commit structurés (Conventional Commits) :

```
feat: ajouter l'authentification utilisateur
fix: corriger la connexion à la base de données
docs: mettre à jour la documentation d'installation
refactor: améliorer l'architecture backend
test: ajouter des tests d'intégration API
security: corriger la vulnérabilité d'authentification
```

### Étape 4 : Pousser et ouvrir une Pull Request
```bash
git push origin feature/nom-de-la-fonctionnalité
```

Ouvrez ensuite une Pull Request sur GitHub ciblant `develop` (ou `main` avec l'approbation du propriétaire).

### Étape 5 : Révision et approbation
- Le propriétaire du dépôt examinera vos modifications.
- Tous les commentaires doivent être résolus avant la fusion.
- Au moins une approbation du propriétaire est requise.
- Les vérifications automatisées (CI) doivent réussir.

## 4. Conventions de nommage des branches

| Type | Exemple |
|------|---------|
| `feature/*` | `feature/user-authentication`, `feature/product-management` |
| `bugfix/*` | `bugfix/database-connection`, `bugfix/login-validation` |
| `hotfix/*` | `hotfix/security-patch`, `hotfix/critical-crash` |

## 5. Règles de sécurité
- **Ne jamais committer** de mots de passe, clés API, tokens ou identifiants de base de données.
- Utilisez des variables d'environnement et les GitHub Secrets.
- Respectez le fichier `.gitignore`.

## 6. Contact
Pour toute question, contactez le propriétaire du dépôt : **@awalousadoub-debug**.

---
*Propriétaire du dépôt : @awalousadoub-debug*