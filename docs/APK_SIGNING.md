# APK Signing Continuity — Asterion Pilot

- The existing GitHub-generated debug APKs used default runner signing keys and cannot be assumed mutually upgrade-compatible.
- The GitHub Actions workflow now caches its own `~/.android/debug.keystore` under `asterion-pilot-ci-debug-signing-v1` so subsequent CI builds can reuse the **same debug certificate while that cache exists**.
- A reproducible debug signer is **not** a production signer: GitHub caches can expire, be evicted, or be invalidated. Reinstallation may still be necessary if the signer changes.
- Each APK artifact now contains `SIGNING_CERTIFICATE.txt` showing the SHA-256 signing identity. Compare the fingerprints before replacing an existing installation. If the fingerprints differ, Android will reject an in-place install.
- **Preserve installed game learning** by exporting game data in the app before any uncertain installer update, particularly if uninstalling.
- The repository contains **no production signing private key**. A stable production signing keystore kept in a private secret store is required for guaranteed long-term update continuity.
- Never commit a production signing key or signing passwords to this public repository.
- This document does not alter the canonical purpose and architecture specification.
