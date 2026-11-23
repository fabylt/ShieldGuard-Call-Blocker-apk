# ShieldGuard Call Blocker — Progetto Android Nativo (Kotlin + Jetpack Compose)

Questo archivio contiene il progetto Android nativo completo di **ShieldGuard**, progettato per Android 10+ (API 29–35) con:

- **`CallScreeningService`** (`ShieldCallScreeningService.kt`): filtraggio chiamate nativo a livello di sistema operativo senza wake-lock (`< 0.1%` batteria in 24 ore), con supporto per:
  - Blocco numeri sconosciuti (non presenti in rubrica)
  - Blocco chiamate anonime / private
  - Blocco numeri esteri con prefissi consentiti configurabili
  - Blocco database spam pubblico verificato e blacklist con wildcard (`+44*`, `899*`)
  - Blocco chiamate segnalate come spam da **Hiya / Android Smart Call** (`CALL_VERIFICATION_STATUS_FAILED` e segnalazioni operatore)
  - Whitelist prioritaria assoluta e bypass numeri di emergenza (112, 113, 115, 118, 911)
- **`WorkManager`** (`SpamListSyncWorker.kt`): sincronizzazione automatica periodica delle liste spam pubbliche (6h / 12h / 24h) con vincoli opzionali **solo Wi-Fi**, **solo in carica** e rispetto assoluto della **Modalità Offline Privacy**.
- **`RoleManager.ROLE_CALL_SCREENING`** (`MainActivity.kt`): richiesta nativa per impostare ShieldGuard come app predefinita ID chiamante e spam di Android.

---

## Opzione 1 — Genera il file `.apk` gratis su GitHub (Consigliata, zero installazioni)

Non serve installare Android Studio né Java sul tuo computer: il progetto include già la pipeline automatica `.github/workflows/build-apk.yml`.

1. Crea un nuovo repository (anche privato) su [GitHub](https://github.com/new).
2. Estrai questo file ZIP e carica tutti i file della cartella `android-app` nel repository (assicurandoti che `.github/workflows/build-apk.yml` sia presente nella root del repository).
3. Apri la scheda **Actions** del repository su GitHub.
4. Esegui il workflow **Build ShieldGuard Android APK** (si avvia anche automaticamente ad ogni push).
5. Al termine della compilazione (circa 2 minuti), scarica l'artefatto **`ShieldGuard-Android-APK`** contenente `app-debug.apk`.
6. Trasferisci `app-debug.apk` sullo smartphone Android, installalo abilitando *Origini sconosciute*, apri ShieldGuard e tocca **Imposta come app ID chiamante e spam**.

---

## Opzione 2 — Compila il file `.apk` in locale con Android Studio

Requisiti: Android Studio (Koala o successivo) con JDK 17 e Android SDK 35.

1. Estrai lo ZIP e apri la cartella in **Android Studio** (*File → Open*).
2. Attendi la sincronizzazione di Gradle.
3. Compila l'APK dal menu **Build → Build Bundle(s) / APK(s) → Build APK(s)** oppure da terminale:

```bash
gradle assembleDebug
```

Il file `.apk` verrà generato in:

```text
app/build/outputs/apk/debug/app-debug.apk
```
