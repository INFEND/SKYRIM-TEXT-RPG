# Sovngarde font in Android APK

Source: user uploaded Sovngarde.zip, containing Sovngarde Light.ttf, Sovngarde Bold.ttf
and Sovngarde OFL License.txt, all from the 2025 Desktop Edition, including Cyrillic.

The verified identical copies are fetched from a fixed public GitHub revision
of B4lex/PlayerHub at e9fa97b3c312eb948f3808f5c2d1f053555d1a48.
Before any Android build the SHA-256 of both fonts and the license is validated.
The TTF files and the full SIL OFL 1.1 license are bundled into the final APK
under assets/fonts. Displaying the font at runtime requires no network.

Light SHA-256: 8a080bba62534ca37bdd3137e933f9215dbf18e8a070495e4ae782ea7c3b9851
Bold SHA-256: a4abb4f2cf49f7e52d87f45d59b1fe2592d464c94f049094f66520072317c2ef
License SHA-256: beb729413bd1bb04bfecbfd530eb0b899777fdd57472c8496a26e2cd53ef8bd5

License: (c) 2016 mjorka, SIL OPEN FONT LICENSE v1.1, bundled into APK.

UI contract: use both family weights throughout native views and dialogs.
Speaker is on its own line, spoken text on next line, no quote rule; narration is italic.
Nested vertical and horizontal scroll offsets are remembered for each tab.
