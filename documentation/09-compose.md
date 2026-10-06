# 09 — L'interface en Jetpack Compose

Toute l'interface est écrite en **Jetpack Compose** (le système d'interface déclaratif d'Android) : plus aucun fichier
`res/layout`. Cette page explique ce qui a été créé, comment ça marche, et ce qui reste en « vue classique » et pourquoi.

## L'idée en trois phrases

1. **Un écran est une fonction.** `LoginScreen(providers, busy, error, …)` reçoit des données et des actions, et décrit
   ce qu'on voit. Quand une donnée change, Compose rappelle la fonction et met l'écran à jour : on ne modifie jamais une
   vue à la main (`textView.text = …`, `isVisible = …`).
2. **Il n'y a plus de Fragment.** Chaque écran a une fonction « route » (`XRoute`, à côté de `XScreen`) qui fait ce que
   faisait le Fragment : elle lit le ViewModel (`collectAsStateWithLifecycle()`), demande les permissions, affiche les
   messages (`Toast`) et passe le tout à l'écran. Elle ne navigue pas elle-même : elle reçoit des lambdas
   (`onBack`, `onOpen`…) que le `NavHost` (`Navigation.kt`) branche.
3. **Les écrans ne connaissent pas les ViewModels.** Ils reçoivent de simples valeurs et des lambdas (`onBack`,
   `onSave`…). C'est ce qui les rend faciles à lire, à prévisualiser (`@Preview`) et à tester.

```
ProjectListRoute ──(ViewModel, permissions, messages)──▶ ProjectListScreen(projects, syncing, onOpen, …)
   fait quoi : brancher                                      fait quoi : dessiner et appeler les lambdas
```

## Les briques communes (`core/ui`)

| Fichier | Contenu |
|---|---|
| `theme/PuzzleTheme.kt` | `PuzzleTheme` : les couleurs de `res/values/colors.xml` (une seule source), la police Space Grotesk, les coins 12 / 20 / 28 dp. `PuzzleText.display(taille)` = les grands titres, `PuzzleText.eyebrow` = le petit libellé citron. `PuzzleColors` = les couleurs sans case dans Material (`warning`, `scrim`, `secondaryText`). |
| `PuzzleComponents.kt` | `RoundIconButton` (bouton rond 48 dp), `ScreenHeader` (flèche retour + titre), `PrimaryButton` (citron), `TonalButton` (discret), `PuzzleCard` (carte à contour), `EmptyMessage`, `ConfirmDialog` (« Supprimer ? »). Un écran n'utilise jamais un bouton Material brut : il appelle l'un de ceux-là. |
| `PuzzleLoader.kt` | Les quatre carreaux qui se soulèvent (l'indicateur « je réfléchis »), dessinés dans un `Canvas`. |
| `FileImage.kt` | Affiche une photo stockée dans un fichier (Glide décode, tourne selon l'EXIF et réduit). |

## Écran par écran

| Écran | Fichiers | À savoir |
|---|---|---|
| Connexion | `account/LoginScreen.kt`, `LoginRoute.kt` | Les champs (`email`, `password`) vivent dans l'écran avec `rememberSaveable` (ils survivent à une rotation). Un bouton par fournisseur renvoyé par le serveur. |
| Compte | `account/AccountScreen.kt` | |
| Liste | `project/list/ProjectListScreen.kt` | `LazyColumn` (l'équivalent du RecyclerView) ; chaque carte « tombe » à sa première apparition (`Animatable`). |
| Création / modification | `project/create/ProjectCreationScreen.kt` | Le formulaire est local à l'écran ; il n'appelle le ViewModel qu'au clic sur « Créer ». La grille (lignes × colonnes) est suggérée quand le nombre de pièces ou la photo change. |
| Cadrage de la boîte | `project/create/PuzzleBoundsScreen.kt` | Les 4 coins sont toujours la vue `QuadSelectionView` (voir plus bas). |
| Table du puzzle | `puzzle/PuzzleWorkingScreen.kt`, `ScanSheet.kt`, `LevelSheet.kt`, `LeadComparison.kt`, `PuzzleMap.kt` | Voir la section suivante. |
| Caméra | `puzzle/capture/PieceCaptureScreen.kt` | Aperçu CameraX (vue), cadre de visée dessiné en Compose, obturateur qui rétrécit quand on appuie. |
| Historique, Progression | `history/HistoryScreen.kt`, `progress/ProgressScreen.kt` | Suppression avec `ConfirmDialog`. |

## La table du puzzle (le plus gros écran)

* **`PuzzleWorkingScreen`** pose les couches : la carte plein écran, la barre flottante (`TopBar`), puis la feuille.
* **`ScanSheet.kt`** contient ce que montre la feuille, un composable par état du scan (`ScanState`) :
  `IdleContent` → `AnalyzingContent` → `ResultContent`, ou `ErrorContent`. `ResultContent` se découpe en
  `LeadChips`, `Compare`, `TurningPiece` (la pièce qui apparaît puis tourne), `Confidence` (le nombre qui monte) et
  `ExplainCard` (une carte par piste).
* **`LevelSheet.kt`** remplace le `BottomSheetBehavior`. `LevelSheetState` retient où est le haut de la feuille
  (`offset`, en pixels) et les 4 niveaux (`SheetLevel` : caché, aperçu, moitié, plein) avec leur position (`anchors`).
  Le doigt déplace `offset` (`dragBy`), à la levée du doigt la feuille glisse vers le niveau le plus proche en tenant
  compte de la vitesse (`settle`). Un `NestedScrollConnection` partage le geste avec le contenu qui défile : on monte la feuille
  d'abord, puis le contenu défile ; en haut du contenu, tirer vers le bas redescend la feuille.
  Les hauteurs des niveaux « aperçu » et « moitié » sont **mesurées** dans `ResultContent` (`onGloballyPositioned`).
* **`LeadComparison.kt`** : la boîte avec la pièce qui clignote dessus, dessinée dans un `Canvas` (un tap fige le clignotement).

## Ce qui reste en vue classique (et pourquoi)

Ces vues dessinent tout elles-mêmes et gèrent leurs gestes ; les réécrire n'apporterait rien et risquerait de les
dégrader. Elles sont insérées dans Compose avec `AndroidView`, chacune derrière une fonction qui lit comme un composable :

| Vue | Enveloppe | Pourquoi |
|---|---|---|
| `PuzzleMapView` | `PuzzleMap` | pincer, déplacer, double tap, caméra animée, projecteur sur la piste (300 lignes de dessin et de gestes) |
| `QuadSelectionView` | `PuzzleBoundsScreen` | poignées des 4 coins et glissement du cadre |
| `PreviewView` (CameraX) | `PieceCaptureScreen` | l'aperçu caméra est une vue |

## La navigation (`Navigation.kt`)

* **Un seul `NavHost`**, créé par `MainActivity` (`setContent`, thème et fond sombre). Plus de `nav_graph.xml`, de
  `NavHostFragment`, de Safe Args ni de `Toolbar`.
* **Routes typées** : `Dest` (une `sealed interface` `@Serializable`) liste les écrans et leurs arguments
  (`Dest.PuzzleWorking(projectId)`, `Dest.ProjectCreation(projectId = null)`…). On navigue avec
  `navController.navigate(Dest.History(id))` : une faute de frappe ne compile pas. Les ViewModels qui lisent
  `SavedStateHandle["projectId"]` (historique, progression) n'ont pas changé : le nom du champ est la clé.
* **Résultats entre écrans** : l'écran du dessus écrit dans le `savedStateHandle` de l'entrée en dessous
  (`previousBackStackEntry`), puis revient. La caméra écrit `PIECE_PHOTO` (la table l'analyse puis l'efface) ; le cadrage
  écrit les 4 coins et la photo redressée (la création les lit puis les efface). Remplace `setFragmentResult`.
* **Connexion** : `PuzzleNavHost` suit `AccountStore.account` ; si le compte disparaît (déconnexion, 401) il revient sur
  `Dest.Login`, s'il apparaît il passe à la liste.
* **Transitions** : un léger glissement horizontal + fondu entre écrans (l'ancien `MaterialSharedAxis` X), la caméra
  monte du bas (Y), la connexion fait un fondu.
* **En-têtes** : tous les écrans dessinent leur `ScreenHeader`, y compris la création et le cadrage (ils utilisaient la
  `Toolbar` de l'activité). Plus de liste `ownHeader` à tenir à jour.

## Notions Compose utilisées (petit lexique)

| Notion | Où | En clair |
|---|---|---|
| `@Composable` | partout | fonction qui décrit de l'interface |
| `Modifier` | partout | chaîne d'ajustements (taille, marge, fond, clic…), **l'ordre compte** : `padding` puis `background` ≠ `background` puis `padding` |
| `remember { … }` | `ProjectCard`, `LevelSheet` | garde une valeur entre deux dessins |
| `rememberSaveable` | formulaires | pareil, et survit à une rotation |
| `mutableStateOf` | `overlayOn`, `confirmDelete` | une valeur dont le changement redessine l'écran |
| `collectAsStateWithLifecycle()` | routes | transforme un `StateFlow` du ViewModel en état Compose |
| `LaunchedEffect(clé) { … }` | `PuzzleMap`, `PuzzleWorkingScreen` | exécute du code (souvent animé) quand la clé change |
| `Animatable`, `animate*AsState` | `TurningPiece`, `Confidence` | animations |
| `Canvas` | `PuzzleLoader`, `LeadComparison`, cadre de visée | dessin libre |
| `AndroidView` | `PuzzleMap`, caméra | héberger une vue classique |
| `testTag("nom")` | partout | nom d'un élément pour les tests (les anciens identifiants de vues) |

## Tester

* **Aperçus** : chaque écran a une fonction `@Preview` (Android Studio → « Split »), sans lancer l'application.
* **Tests e2e** (`ScanFlowTest`) : `createEmptyComposeRule()` ; on trouve les éléments par leur tag
  (`onNodeWithTag("buttonDismiss")`). `DemoTourTest` (la vidéo du README) n'utilise pas cette règle, qui gèlerait les
  animations infinies : il lit l'arbre d'accessibilité (les tags sont exposés comme identifiants) et envoie de vrais
  gestes `input`.

## Suite prévue

* Régénérer les images et la vidéo du README (elles montrent encore l'ancienne interface).
