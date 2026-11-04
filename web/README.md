# AURA Web & iPhone Companion PWA (`/web`)

This folder is a complete, zero-API-cost Progressive Web App (PWA) for **iPhone (Safari / Add to Home Screen)**, **Chrome**, **Mac**, and **Windows** that connects to the **exact same Firebase project & named Firestore database** as your Android AURA app.

## Cost Protection Guarantee ($0.00 AI Cost)
- **Zero Gemini API keys** and **zero paid LLM SDKs** exist anywhere in `/web`.
- **Local-First Architecture**: Daily timetable, course, flashcard, note, and focus edits save to browser `localStorage` first (`0` Firestore reads/writes during routine offline use).
- **Cloud Sync & Live Study Rooms**: Uses your existing Firebase project (`gen-lang-client-0879201853`) and free **Jitsi Meet** (`meet.jit.si`) for live voice, camera, and slide screen-sharing.

---

## Deploy to Vercel in 3 Steps (2 Minutes)

1. **Export / Push to GitHub**
   - In Google AI Studio, click **Push to GitHub** (or download ZIP and push to a GitHub repository).
2. **Import into Vercel (`https://vercel.com/new`)**
   - Select your GitHub repository.
   - Set **Root Directory** to: `web`
   - Framework Preset: **Vite** (`npm run build`, Output Directory: `dist`)
   - Click **Deploy** — you will receive a live URL like `https://your-aura-app.vercel.app`.
3. **Authorize Your Vercel Domain for Google Sign-In (One-Time)**
   - Open [Firebase Console](https://console.firebase.google.com/project/gen-lang-client-0879201853/authentication/settings) -> **Authentication** -> **Settings** -> **Authorized domains**.
   - Click **Add domain** and paste your Vercel domain (e.g. `your-aura-app.vercel.app`).
   - In your Android AURA app -> **Study -> Cloud -> Host Session -> QR / iPhone Link -> Set Your Vercel Web App Domain**, paste `https://your-aura-app.vercel.app` and tap **Save**. Every QR code and invite link your phone generates will now point directly to your live site!
