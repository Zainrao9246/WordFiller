<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>
Fillwords
A Jetpack Compose word puzzle game for Android, with a mini-games hub and a PIN-locked encrypted media vault.
Features
Word Puzzles — 300 curated levels across 7 packs
Pack	Levels	Board sizes
Warmup	1–25	4×4 & 5×5
Classic	26–50	5×6, 6×6 & 8×11
Nature & Cosmos	51–75	6×7 & 7×7
Mastermind	76–100	7×8 & 8×8
Legends	101–150	8×8 & 9×8
Grand Odyssey	151–200	9×8 & 10×8
Titan	201–300	10×8 & 10×10
Signature milestones: Aurora Crown (150), Odyssey Apex (200), Titan Crown (250), Titan Apex (300).
Levels are deterministically generated from curated seed words (no repeats, no fallback boards) and verified by `PuzzleIntegrityTest`.
Mini Games hub (Home → Mini Games)
Simple navigation: a single hub screen with five self-contained games, all reusing the shared `BonusDictionary` word list where applicable:
Anagram Rush — unscramble 5–7 letter words against the clock
Word Chain — each word must start with the last letter of the previous one; chain combos score more
Hangman — classic letter guessing on 5–8 letter words
Memory Match — flip cards and pair up matching words
Tic Tac Toe — classic X vs O against a board AI (win / block / center strategy); builds a win streak with a persisted best record
Encrypted Vault
PIN-gated folder view: unlocking the vault always opens the "Encrypted Folders" screen first — media is only reachable through a folder
AES-GCM encryption for stored notes/paths; media is decrypted only while being viewed
Auto-locks to the game home screen whenever the app is minimized (ON_PAUSE/ON_STOP); covered by an E2E test simulating minimization
Lives entirely outside the mini-games flow
Run Locally
Prerequisites:  Android Studio

Open Android Studio
Select Open and choose the directory containing this project
Allow Android Studio to fix any incompatibilities as it imports the project.
A `.env` file is already included in the project directory — add your `GEMINI_API_KEY` to it if the app uses the Gemini API (see `.env.example`)
A shared `debug.keystore` is already included in the project root, so debug builds sign out of the box — no build script changes needed
Run the app on an emulator or physical device (use `./gradlew assembleDebug` or Android Studio)
If you have already published your app in AI Studio, please request upload key reset in Google Play Console.
