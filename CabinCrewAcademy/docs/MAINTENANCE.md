# Maintaining the question database

## Recommended workflow

1. **Keep your master copy outside the app.** Use a JSON or CSV file on your computer or in cloud storage, and edit questions there.
2. Import it into the app. To update questions that are already in the app, re-import the file and choose **Import and overwrite existing**. Matching IDs are updated, new IDs are added, and your progress is kept.
3. **Back up regularly** with **Settings → Export database**. The exported JSON uses the exact import format, so it can be re-imported on any device.

## IDs are the key

- The `id` of each question is how the app recognises a question across imports.
- If you change a question's text but keep its ID, overwriting updates it in place and keeps its history.
- If you change the ID, the app treats it as a new question, and the old one stays until you delete it.
- Use a consistent scheme, such as `AV-001` for Aviation Knowledge and `FA-001` for First Aid.

## What each data-management action does

| Action | Questions & categories | Bookmarks / favourites / difficult flags | Answer statistics | Exam history | Streak & daily activity |
|---|---|---|---|---|---|
| Import (skip existing) | adds new | kept | kept | kept | kept |
| Import (overwrite existing) | adds new + updates matching IDs | kept | kept | kept | kept |
| Edit question | updates | kept | kept | kept (reports keep the original wording) | kept |
| Delete question | removes it | removed for that question | removed for that question | kept (reports keep a snapshot) | kept |
| Delete category | removes the category **and all its questions** | removed for those | removed for those | kept | kept |
| Settings → Reset progress | kept | **kept** | cleared | cleared | cleared |
| Settings → Delete all questions | **all removed** | removed | removed | kept | kept |

Exam results store a snapshot of each question. Correction reports therefore stay accurate even after you edit or delete questions.

## Moving to a new tablet

1. On the old tablet, go to **Settings → Export database** and save the JSON file.
2. Install the APK on the new tablet.
3. Import the JSON file.

Only questions and categories are transferred. Progress and exam history stay on the original device.

## Where the data lives

| Data | Storage |
|---|---|
| Categories, questions, progress, exam results, daily activity | Room/SQLite database `cabin_crew_academy.db` in the app's private storage |
| Settings (theme, name, exam defaults, daily goal, last study position) | DataStore `settings.preferences_pb` |

Uninstalling the app deletes both. To update the app, install the new APK over the old one instead of uninstalling.

## For developers

- **Schema changes:** bump `version` in `AppDatabase` and add a Room `Migration`. Don't use destructive migration, because users would lose their data. Exported schemas are written to `app/schemas/` for reference.
- **Import format changes:** the parser lives in `data/importexport/QuestionParser.kt` and the exporter in `Exporter.kt`. `QuestionParserTest` checks that the shipped templates and the export→import round trip stay valid.
- **UI regression check:** `./gradlew testDebugUnitTest` runs a Robolectric tour of every screen on a simulated 1280×800 dp tablet in landscape and an 800×1280 dp tablet in portrait, and writes screenshots to `app/build/screenshots/`. On networks where Robolectric can't download its Android runtime, set `ROBOLECTRIC_DEPS_DIR` to a folder that contains `android-all-instrumented-15-robolectric-12650502-i7.jar`.
- **Versioning:** increase `versionCode` and `versionName` in `app/build.gradle.kts` for every release so that Android accepts the update.
