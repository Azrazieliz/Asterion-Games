# APK signing — current implementation

The production signing identity has **not** been configured. Do not publish any production private key in this public repository.

## CI debug signing (best-effort continuity)

The workflow creates an explicit `.ci-signing/debug.keystore` if no cached key exists, configures the Android Gradle Plugin to use it, and persists the keystore using GitHub Actions cache key `asterion-pilot-ci-debug-signing-v2`. This corrects the previous ineffective cache path `~/.android/debug.keystore`, which was confirmed absent from the runner.

Every build verifies its APK with Android's `apksigner` and includes `SIGNING_CERTIFICATE.txt` with the signer SHA-256 fingerprint. **A second independent CI build must be checked before claiming cross-build certificate stability.**

Cache entries can expire or be evicted. Even when successive debug builds have matching certificates, this is not a durable production signing system. Releases need one securely backed-up keystore stored in private CI secrets.

## Installation compatibility

- Existing v0.2.0/debug APKs may have unrelated certificates. Android rejects an in-place update signed by a different certificate.
- Export the learning backup **before** a potentially incompatible upgrade; uninstalling a package normally removes its local data.
- A new v0.3.0 APK can update another APK of the same package only if the Android signing certificate matches.
- The signing certificate fingerprint is provided with each GitHub Actions artifact so incompatibility can be detected without guessing.
