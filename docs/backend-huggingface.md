# Intégration backend Hugging Face

## Périmètre

Cette couche Java expose l'analyse de sentiment sous la forme :

UI / ViewModel -> SentimentRepository -> SentimentApi (Retrofit) -> Hugging Face Inference

La couche UI ne doit pas construire des requêtes HTTP elle-même. Elle appelle
SentimentRepository.analyze(text, callback). Le dépôt vérifie l'entrée et le
token, effectue l'appel de façon asynchrone, puis retourne une catégorie
POSITIVE, NEUTRAL ou NEGATIVE et son score de confiance.

## Modèle et endpoint

- Modèle : clapAI/modernBERT-base-multilingual-sentiment
- Provider : hf-inference
- Endpoint : https://router.huggingface.co/hf-inference/models/{modelId}
- Payload : inputs et parameters.top_k = 3
- Réponse : liste de labels et scores ; l'application retient le score le plus élevé.

Le modèle annonce la prise en charge du français, des labels positif/neutre/négatif
et la licence Apache-2.0. Le résultat reste une prédiction imparfaite, surtout
pour l'ironie, les textes ambigus et les formulations propres aux étudiants.

## Configuration locale

1. Créer un token Hugging Face fine-grained avec seulement la permission
   Make calls to Inference Providers.
2. Copier secrets.properties.example vers secrets.properties.
3. Renseigner HF_TOKEN=... dans le fichier local secrets.properties.
4. Synchroniser Gradle et lancer la variante debug.

secrets.properties est ignoré par Git. Ne jamais envoyer le token dans le chat,
le committer, le placer dans le manifeste ou l'ajouter aux logs.

Limite de sécurité : une valeur ajoutée à BuildConfig est intégrée au fichier
APK debug et peut être extraite par un utilisateur. Ce mécanisme évite de
committer le token et convient uniquement au développement/démonstration.
Ne distribue pas un APK contenant un token. Pour une version distribuée, place
le token sur un serveur intermédiaire contrôlé par l'équipe et ne l'embarque pas
dans l'application Android.

## Confidentialité et erreurs

Le contenu du journal est transmis au service distant pour l'analyse. L'interface
devra informer l'utilisateur et demander son accord avant le premier envoi.
N'envoyer que le texte nécessaire. Le client n'active aucun journal HTTP et ne
retourne jamais le corps d'erreur brut du fournisseur.

Les erreurs d'entrée vide, token absent, authentification, quota (HTTP 429),
modèle indisponible (HTTP 503), réseau et réponse invalide sont distinguées dans
SentimentRepository.FailureKind.

## Vérifications

SentimentApiContractTest vérifie localement l'URL, le header Bearer, le JSON
envoyé et la conversion de la réponse en catégorie/score. Ce test simulé ne
prouve pas l'accès au service Hugging Face.

Pour un test réel depuis un téléphone ou un émulateur connecté à Internet :

1. Configure le token localement comme décrit ci-dessus.
2. Dans Android Studio, lance HuggingFaceLiveApiTest sur l'appareil connecté.
3. Le test envoie une phrase de test non personnelle et vérifie qu'une réponse
   avec un score valide revient en moins de 90 secondes.
4. Répète le test avec une phrase négative et neutre lorsque la fonctionnalité
   sera intégrée à l'écran.

Sans token, Android Studio marque le test live comme ignoré. Aucun token réel ne
doit être ajouté aux tests ou au dépôt.
