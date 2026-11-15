# Ultra Lite
Stage 1: external font packs (8,795,475 raw bytes), R8/resource shrinking, download buttons.
The signed catalog pins each font to the upstream Git revision and validates SHA-256 at runtime.
Sovngarde remains bundled and is the offline fallback. No save format or app ID changes.
Native local inference remains in the main APK until a separately signed IPC companion is implemented and device-tested. Do not load downloaded executable libraries from the resource store.
