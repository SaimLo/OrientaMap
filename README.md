# OrientaMap Android

Squelette initial de l'application Android de cartographie d'orientation.

## Objectif du premier incrément

- Créer et lister des projets cartographiques.
- Persister les projets localement avec Room.
- Naviguer vers un écran de carte placeholder.
- Préparer l'architecture pour les couches IGN, le GPS et le mode hors connexion.

## État

Ce dépôt est un squelette architectural. Les versions Gradle et des dépendances devront être vérifiées dans Android Studio avant le premier build CI.

## Modules prévus

- `presentation`: écrans Compose et ViewModels.
- `domain`: modèles métier et interfaces de repositories.
- `data`: Room, fichiers locaux et connecteurs IGN.
- `map`: moteur cartographique, projections et couches.
- `core`: utilitaires transverses.

## Premier écran cartographique

Le squelette contient maintenant un premier écran MapLibre configuré avec le fond Plan IGN via la Géoplateforme. La permission de localisation est demandée à l'ouverture de l'écran et la position GPS est suivie avec les APIs Android natives.

Le point GPS sera ensuite rendu directement sur la carte, puis complété par les contrôles de couches et le mode hors connexion.
