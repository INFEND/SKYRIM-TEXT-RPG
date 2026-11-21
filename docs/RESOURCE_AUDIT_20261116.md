# Resource audit · 2026-11-16

Baseline v1.0.129: 29 galleries / 166 views, 31 ZIP archives (85,693,018 bytes); 60 hero portraits. The 512-point map has 28 directly matched gallery names; Blackreach is an additional gallery. The other 484 mapped points do not yet have dedicated galleries. The latest verified replacement is Riverwood v1.0.101 (five views). Whiterun and other city galleries have not been regenerated in the release resource tree since that version. No dedicated complete catalog of tavern and shop interiors exists.

Correction: the nine city emblems, two generic settlement markers and six location-type markers were already present as small Android drawable atlases, rather than optional files. Their exact approved WebP blobs are copied into media/mapicons, with core fallbacks retained. No replacement image has been invented.

The signed catalog can now be published independently. New galleries use docs/gallery_registry.json entries with name, slug, captions and optional aliases; their ZIP files must contain consecutive 01.webp...NN.webp. The build verifies all ZIP entries and caption counts. The full registry is signed; an installed game can discover new gallery names and captions without an APK change.

Remaining art work: replace incorrect city views, dedicated tavern/shop interiors, and the 484 remaining map points. Do not declare any gallery complete without checking source images and names. No placeholder art is included.
