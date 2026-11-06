# OrientaMap Android

OrientaMap est une application Android Kotlin / Jetpack Compose destinée à préparer des cartes d’orientation à partir de données géographiques IGN, puis à les enrichir progressivement avec des objets vectoriels inspirés de l’ISOM.

## Fonctionnalités actuelles

### Projets cartographiques

- création d’un projet avec un nom personnalisé ;
- liste, ouverture, renommage et suppression des projets ;
- persistance locale avec Room ;
- conservation des bornes géographiques de la zone ;
- cycle de vie du projet : `BROUILLON`, `ZONE_DEFINIE`, `EDITION_EN_COURS`, `ARCHIVE`.

### Carte et mode hors connexion

- affichage du fond Plan IGN avec MapLibre Native Android ;
- sélection d’une emprise géographique ;
- téléchargement d’un paquet offline Plan IGN ;
- niveaux de zoom actuellement supportés : 10 à 16 ;
- suivi de la progression, pause, reprise et annulation ;
- persistance des paquets téléchargés ;
- utilisation de la carte en mode avion après téléchargement ;
- affichage de la position GPS.

Le cache de tuiles MapLibre et la base Room sont des données locales à l’application. Une désinstallation ou un `pm clear` les supprime.

### Export PDF imprimable

L’application peut générer un PDF :

- format A3 paysage ;
- échelle `1:10 000` ;
- référentiel de travail `EPSG:2154` ;
- fond raster capturé avec MapLibre ;
- cadre cartographique, échelle graphique, nord et métadonnées ;
- courbes de niveau IGN converties vers Lambert-93 ;
- styles distincts pour les courbes normales et maîtresses ;
- clipping des courbes au cadre cartographique.

Le bouton d’export est disponible uniquement pour un projet dont la zone est définie et dont le paquet offline est terminé.

### Courbes de niveau IGN

Les courbes sont récupérées depuis le service WFS de la Géoplateforme :

```text
https://data.geopf.fr/wfs/ows
```

Couche utilisée :

```text
ELEVATION.CONTOUR.LINE:courbe
```

Le premier export d’une zone nécessite une connexion réseau. La réponse GeoJSON est ensuite conservée dans le cache local du projet, dans le répertoire privé de l’application. Les exports suivants de la même zone peuvent donc fonctionner sans réseau.

La clé du cache dépend du projet, de ses bornes, de la source IGN et de la version du format de cache. Une nouvelle zone ou une nouvelle version de source déclenche automatiquement une nouvelle récupération.

## Architecture principale

```text
presentation/
├── Compose, navigation et ViewModels
├── écran de carte et sélection de zone
└── écrans de projets et de paquets offline

data/
├── local/       Room, entités et migrations
├── maplibre/    téléchargement et affichage offline
├── offline/     coordination des téléchargements
├── importer/    WFS IGN, GeoJSON, cache local et import RGE ALTI
└── print/       snapshot MapLibre et export PDF

domain/
├── modèles projet et géographiques
├── modèles vectoriels ISOM
└── interfaces de repositories
```

## Pré-requis

- Android Studio récent ;
- Android SDK Platform 35 ;
- JDK 17 ;
- Gradle via le wrapper du projet ;
- téléphone Android ou émulateur ;
- connexion réseau pour le premier téléchargement IGN et le premier export d’une zone non mise en cache.

## Compiler et tester

Depuis le dossier racine du projet :

```powershell
.\gradlew.bat app:testDebugUnitTest
.\gradlew.bat app:connectedDebugAndroidTest
```

Pour construire et installer l’APK debug :

```powershell
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:installDebug
```

Les tests unitaires couvrent notamment :

- le modèle de projet et son cycle de vie ;
- les bornes géographiques ;
- la projection Lambert-93 ;
- le modèle et l’import des courbes RGE ALTI ;
- la construction de la requête WFS ;
- le cache local GeoJSON ;
- les calculs et paramètres d’impression.

Les tests applicatifs couvrent notamment :

- la persistance Room d’une zone définie après réouverture de la base ;
- les écrans Compose de projet et de carte ;
- la sélection et le redimensionnement de zone ;
- la préparation d’un paquet offline ;
- la visibilité de l’export PDF ;
- la génération d’un PDF A3 avec fond et courbes ;
- la lecture du cache de courbes dans le stockage Android.

## Validation manuelle recommandée

1. Créer un projet et lui donner un nom.
2. Ouvrir le projet et sélectionner une zone.
3. Valider la zone.
4. Télécharger le paquet Plan IGN jusqu’à l’état `Terminé`.
5. Ouvrir un premier export PDF avec le réseau activé afin de récupérer les courbes IGN.
6. Vérifier le cadre, le fond, les courbes, l’échelle et le nord.
7. Activer le mode avion.
8. Réexporter le même projet et la même zone.
9. Vérifier que l’export utilise le cache local sans accès réseau.

## Limites connues

- les courbes IGN sont actuellement téléchargées à la demande lors du premier export ;
- le cache est local à l’application et n’est pas synchronisé ;
- le fond raster Plan IGN reste un fond de référence, pas encore une carte ISOM complète ;
- les bâtiments, chemins, hydrographie, végétation et symboles de course seront ajoutés progressivement ;
- une nouvelle zone nécessite un nouveau téléchargement des courbes.

## Données IGN et attribution

Les données IGN doivent rester accompagnées de leur attribution et être utilisées conformément aux conditions de la Géoplateforme et de la Licence Ouverte applicable aux données téléchargées.
