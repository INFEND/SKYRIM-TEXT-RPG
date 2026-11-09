"""Offline sheets and geographic model are separate from the renderer."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
MAP=(JAVA/"SkyrimTerrainMapView.java").read_text()
UI=(JAVA/"MainActivity.java").read_text()

class InteractiveTerrainMap(unittest.TestCase):
    def test_both_terrain_sheets_are_offline_and_bounded(self):
        for name,maximum in (("skyrim_geographic.webp",4_000_000),("solstheim_terrain.webp",800_000)):
            data=(ROOT/"app/src/main/assets/maps"/name).read_bytes()
            self.assertGreater(len(data),100_000)
            self.assertLess(len(data),maximum)
            self.assertEqual(data[:4],b"RIFF")
            self.assertEqual(data[8:12],b"WEBP")
        self.assertIn("MAP_W=4096,MAP_H=4096",MAP)
        self.assertIn("Bitmap.Config.RGB_565",MAP)
        self.assertIn("LOADER.execute",MAP)
        self.assertNotIn("https://",MAP)

    def test_touch_zoom_pan_and_independent_camera_survive(self):
        for token in ("ScaleGestureDetector","GestureDetector","OverScroller","onDoubleTap",
                      "requestDisallowInterceptTouchEvent(true)","private void constrain()",
                      'terrain_map_v2_', 'key("zoom")','key("x")','key("y")',
                      "public void centerOnPlayer()","public void showWholeProvince()",
                      "public void setSheet(int next)","onDetachedFromWindow"):
            self.assertIn(token,MAP)

    def test_geographic_selection_uses_validated_travel(self):
        self.assertIn("WorldAtlas.find(SkyrimMapCatalog.travelDestination(site))",MAP)
        self.assertIn("callback.selected(place)",MAP)
        self.assertIn("MapPinLayout.hit(renderedPins()",MAP)
        self.assertIn("TravelPlanner.plan(game,place)",UI)
        self.assertIn('game.choose("LOC:"+place.name)',UI)
        self.assertNotIn("game.progress.location=",MAP)

    def test_sheets_search_and_list_have_consistent_catalog(self):
        for token in ('terrainMap.setSheet(0)','terrainMap.setSheet(1)',
                      "SkyrimMapCatalog.size()","site.matches(needle)",
                      "mapLocationRow(content,site", "mapListLimit",
                      "ПОКАЗАТЬ ЕЩЁ 60"):
            self.assertIn(token,UI)
        self.assertIn("SkyrimTerrainMapView.iconForSite(site)",UI)
        self.assertIn('pin.player?"маркер игрока":iconForSite(site)',MAP)
        self.assertNotIn("Положение ориентировочное, по справочной сетке",UI)
        self.assertNotIn("GRID_X",(JAVA/"SkyrimMapCatalog.java").read_text())

