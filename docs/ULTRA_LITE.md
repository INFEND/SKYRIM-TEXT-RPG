# Ultra Lite
The main signed APK contains the RPG, Sovngarde fallback and signed catalog; optional fonts, maps, galleries, portraits and extended icons remain outside.
The optional dev.infend.skyrimtext.engine APK is signed with the same certificate. Its signature-protected service streams results over Messenger; a read-only signature-protected provider exposes exactly the existing imported GGUF, without copying it.
The release embeds engine APK size and SHA-256, validates both certificates and rejects any native .so inside the main APK.
Engine installation uses the Android system installer. The game retains saves and GGUF after engine uninstall. Only reinstall the engine if its protocol/version changes.
Device inference test required before claiming full GGUF compatibility.
