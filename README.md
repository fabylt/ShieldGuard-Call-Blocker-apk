# 🛡️ ShieldGuard Call Blocker

**ShieldGuard** è un'app Android nativa (Kotlin + Jetpack Compose) per il filtraggio automatico delle chiamate indesiderate, spam e telemarketing, progettata con focus su **privacy locale** e **consumo minimo di batteria**.

---

## ✨ Funzionalità principali

- **Integrazione nativa Android (`CallScreeningService`)**: filtra le chiamate direttamente a livello di sistema operativo senza processi sempre attivi né wake-lock (`< 0,1%` di batteria in 24 ore).
- **Protezione completa su 5 filtri rapidi**:
  - Blocco **numeri sconosciuti** (non salvati in rubrica o Whitelist)
  - Blocco **chiamate anonime / private**
  - Blocco **numeri esteri** con eccezioni configurabili per prefisso (es. `+41`)
  - Blocco tramite **5 liste spam pubbliche** selezionabili singolarmente (*AGCOM Registro Pubblico*, *Tellows Community*, *Hiya / Smart Call Threat Feed*, *Global Robocall DB*, *Osservatorio Truffe Italia*) con conteggio dei numeri coperti
  - Blocco **Spam Hiya / Android Smart Call** e chiamate non verificate (STIR/SHAKEN)
- **Statistiche e Registro Chiamate**: contatori giornalieri e totali, grafico degli ultimi 7 giorni, filtri per categoria, simulatore di test e aggiunta rapida in Whitelist.
- **Liste personalizzate**: gestione di **Whitelist** (priorità assoluta), **Blacklist** con wildcard (es. `+44*`, `899*`) e prefissi esteri autorizzati. I numeri di emergenza (`112`, `113`, `115`, `118`, `911`) non vengono mai bloccati.
- **Sincronizzazione automatica (`WorkManager`)**: aggiornamento programmato ogni 6h / 12h / 24h con opzioni *Solo Wi-Fi* e *Solo in ricarica*.
- **Modalità Offline Privacy & Backup JSON**: funzionamento 100% locale senza richieste di rete ed esportazione/ripristino delle impostazioni in formato JSON.

---

## 📱 Requisiti

- **Android 10 (API 29) o superiore** (consigliato Android 12–15)
- Impostazione di ShieldGuard come app predefinita per **ID chiamante e spam**

---

## 📥 Installazione (APK)

1. Scarica l'ultimo file `.apk` dalla sezione **[Releases](../../releases)** di questo repository.
2. Apri il file `.apk` sullo smartphone Android (se richiesto, consenti l'installazione da *origini sconosciute* per il browser o file manager).
3. Avvia **ShieldGuard** e tocca **Imposta App Predefinita** per abilitare il filtraggio di sistema.
4. *(Consigliato)* Nella scheda **Opzioni**, concedi l'accesso alla **Rubrica** per evitare di bloccare i tuoi contatti salvati.

---

## 🛠️ Compilazione dell'APK

### Tramite GitHub Actions (consigliato)
1. Apri la scheda **Actions** del repository.
2. Seleziona il workflow **Build ShieldGuard Android APK** e clicca su **Run workflow**.
3. Al termine della build (~2 minuti), scarica l'artefatto contenente il file `.apk`.

### In locale con Android Studio
Requisiti: **Android Studio** con **JDK 17** e **Android SDK 35**.
```bash
gradle assembleDebug
# oppure per la release firmata:
gradle assembleRelease
