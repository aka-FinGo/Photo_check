# Graph Report - .  (2026-08-01)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 57 nodes · 109 edges · 10 communities (7 shown, 3 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 2 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8d9e6f1a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MediaSorterApp
- MainActivity
- MediaItem
- MediaType
- app.js
- gradlew

## God Nodes (most connected - your core abstractions)
1. `MediaSorterApp` - 25 edges
2. `MediaItem` - 8 edges
3. `PhotoCheckApp()` - 8 edges
4. `MainActivity` - 6 edges
5. `MediaRepository` - 4 edges
6. `MediaType` - 3 edges
7. `SorterScreen()` - 3 edges
8. `FavoritesScreen()` - 3 edges
9. `TrashScreen()` - 3 edges
10. `FullScreenMediaViewer()` - 3 edges

## Surprising Connections (you probably didn't know these)
- `MainActivity` --references--> `MediaRepository`  [EXTRACTED]
  android-app/app/src/main/java/com/fingo/photocheck/MainActivity.kt → android-app/app/src/main/java/com/fingo/photocheck/repository/MediaRepository.kt
- `AnalyticsScreen()` --references--> `MediaItem`  [EXTRACTED]
  android-app/app/src/main/java/com/fingo/photocheck/ui/PhotoCheckApp.kt → android-app/app/src/main/java/com/fingo/photocheck/model/MediaItem.kt
- `FavoritesScreen()` --references--> `MediaItem`  [EXTRACTED]
  android-app/app/src/main/java/com/fingo/photocheck/ui/PhotoCheckApp.kt → android-app/app/src/main/java/com/fingo/photocheck/model/MediaItem.kt
- `FullScreenMediaViewer()` --references--> `MediaItem`  [EXTRACTED]
  android-app/app/src/main/java/com/fingo/photocheck/ui/PhotoCheckApp.kt → android-app/app/src/main/java/com/fingo/photocheck/model/MediaItem.kt
- `PhotoCheckApp()` --references--> `MediaItem`  [EXTRACTED]
  android-app/app/src/main/java/com/fingo/photocheck/ui/PhotoCheckApp.kt → android-app/app/src/main/java/com/fingo/photocheck/model/MediaItem.kt

## Import Cycles
- None detected.

## Communities (10 total, 3 thin omitted)

### Community 1 - "MainActivity"
Cohesion: 0.20
Nodes (5): MainActivity, MediaRepository, PhotoCheckTheme(), Bundle, ComponentActivity

### Community 3 - "MediaItem"
Cohesion: 0.61
Nodes (7): MediaItem, AnalyticsScreen(), FavoritesScreen(), FullScreenMediaViewer(), PhotoCheckApp(), SorterScreen(), TrashScreen()

### Community 4 - "MediaType"
Cohesion: 0.50
Nodes (3): MediaType, IMAGE, VIDEO

## Knowledge Gaps
- **4 isolated node(s):** `IMAGE`, `VIDEO`, `state`, `DOM`
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MediaSorterApp` connect `MediaSorterApp` to `.renderStack`, `app.js`?**
  _High betweenness centrality (0.158) - this node is a cross-community bridge._
- **Why does `PhotoCheckApp()` connect `MediaItem` to `MainActivity`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **Why does `MediaItem` connect `MediaItem` to `MainActivity`, `MediaType`?**
  _High betweenness centrality (0.059) - this node is a cross-community bridge._
- **What connects `IMAGE`, `VIDEO`, `state` to the rest of the system?**
  _4 weakly-connected nodes found - possible documentation gaps or missing edges._