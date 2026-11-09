# SKYRIM TEXT RPG: terrain atlas provenance and limitations

## Included map texture (v1.0.37)

- Runtime file: `app/src/main/assets/maps/skyrim_user_mosaic.webp`
- Raster size: **3500 × 1720**, RGB WebP, bundled for offline use
- SHA-256: `2a05447a23ce0df34a4f8e4a8f79c11a5078a8f56838b55a2f25d3d473e3321f`
- Six source screenshots: `1000041469.jpg` through `1000041474.jpg` provided by the user in this chat.
- Captured portions were aligned and feather-blended in the existing map coordinate system.
- Uncaptured regions are an **artistic terrain reconstruction**, including snowy ranges, shoreline, woodland, roads and rivers. Those details are **not a geospatially verified copy** of the game world.
- The additional terrain imagery has been assembled from image-generated art alongside the user's screenshots. This improves coverage but **cannot establish canonical topography**.

## Logical map layers

The programmatic city, player and location markers are separate from the background texture in `SkyrimTerrainMapView.java`. They implement filters, search, hit-testing and camera movement. Some decorative glyphs from the supplied screenshots and generated scenery may remain embedded in the terrain image; these are **not actual interactive pins**. The rendering of only real, dynamically filtered markers remains a future cleanup task.

The authoritative travel graph is `WorldAtlas.java`. `SkyrimMapCatalog.java` adds 305 reference names plotted **only to coarse grid sectors**. Missing exact coordinates, complete Skyrim+DLC place coverage and the separate Solstheim plate are **not solved** by the background reconstruction.

Do not claim this atlas or location set is fully canonical, exact, or complete until independently verified source maps and location data are added.
