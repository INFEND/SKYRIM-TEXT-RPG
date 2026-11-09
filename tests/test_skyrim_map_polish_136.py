"""Shared pin layout prevents a different tap target from the one drawn."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
MAP=(SRC/"SkyrimTerrainMapView.java").read_text()
LAYOUT=(SRC/"MapPinLayout.java").read_text()
UI=(SRC/"MainActivity.java").read_text()

class TerrainMapPolish(unittest.TestCase):
    def test_render_and_hit_test_share_one_visible_snapshot(self):
        self.assertIn("shown=renderedPins()",MAP)
        self.assertIn("MapPinLayout.hit(renderedPins()",MAP)
        self.assertIn("hidden.contains(site.type)",LAYOUT)
        self.assertIn("site.sheet!=sheet",LAYOUT)
        self.assertIn("region>=0&&site.region!=region",LAYOUT)
        self.assertIn("pins=null",MAP)
        self.assertNotIn("game.progress.location=",MAP)

    def test_focus_switches_sheet_and_preserves_world_point(self):
        focus=MAP[MAP.index("public void centerOn(SkyrimMapCatalog.Site target)"):MAP.index("private void persist()")]
        self.assertIn("setSheet(target.sheet)",focus)
        self.assertIn("target.x*scale",focus)
        self.assertIn("target.y*scale",focus)
        self.assertIn("constrain();changed();persist();",focus)
        self.assertIn("No grid-cell positions", (SRC/"SkyrimMapCatalog.java").read_text())

    def test_search_pages_every_match_without_duplicate_story_list(self):
        search=UI[UI.index("private void searchSkyrimAtlas(SkyrimTerrainMapView"):UI.index("private void renderMapTypeFilters()")]
        self.assertIn("activeMap.centerOn(site)",search)
        self.assertIn("searchWindow[0].dismiss()",search)
        self.assertIn("limit[0]+=60",search)
        self.assertNotIn("for(WorldAtlas.Place",search)
        self.assertIn('mapControl(toolbar,"Поиск",()->searchSkyrimAtlas(map))',UI)

    def test_camera_is_restored_after_fullscreen_and_orientation(self):
        self.assertIn("onDetachedFromWindow()",MAP)
        self.assertIn("public void restoreCamera()",MAP)
        self.assertIn("previousScale/Math.max(.0001f,minScale)",MAP)
        self.assertIn("render();map.saveCamera();",UI)
        self.assertIn("activeTerrainMap.restoreCamera()",UI)

