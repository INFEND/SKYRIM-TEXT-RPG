"""User-supplied map montage and touch navigation must work without network."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
MAP=(JAVA/"SkyrimTerrainMapView.java").read_text(encoding="utf-8")
CATALOG=(JAVA/"SkyrimMapCatalog.java").read_text(encoding="utf-8")
UI=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
WORLD=(JAVA/"WorldAtlas.java").read_text(encoding="utf-8")
ART=ROOT/"app/src/main/assets/maps/skyrim_user_mosaic.webp"


class InteractiveTerrainMap(unittest.TestCase):
    def test_six_screenshots_make_one_offline_terrain_texture(self):
        self.assertTrue(ART.is_file(),"The map MUST be bundled in the APK")
        encoded=ART.read_bytes()
        self.assertGreater(len(encoded),100_000)
        self.assertLess(len(encoded),2_500_000)
        self.assertEqual(encoded[:4],b"RIFF")
        self.assertEqual(encoded[8:12],b"WEBP")
        self.assertIn('TERRAIN="maps/skyrim_user_mosaic.webp"',MAP)
        self.assertIn("MAP_W=3500",MAP)
        self.assertIn("MAP_H=1720",MAP)
        self.assertIn("Bitmap.Config.RGB_565",MAP)
        self.assertNotIn("https://",MAP)

    def test_map_has_touch_zoom_pan_limits_and_persistent_camera(self):
        self.assertIn("ScaleGestureDetector",MAP)
        self.assertIn("GestureDetector",MAP)
        self.assertIn("onDoubleTap(MotionEvent e)",MAP)
        self.assertIn("onScroll(MotionEvent a,MotionEvent b",MAP)
        self.assertIn("getScaleFactor()",MAP)
        self.assertIn("requestDisallowInterceptTouchEvent(true)",MAP)
        self.assertIn("private void constrain()",MAP)
        self.assertIn("minScale*11f",MAP)
        for token in ("terrain_map_zoom","terrain_map_x","terrain_map_y"):
            self.assertIn(token,MAP)
        self.assertIn("public void showWholeProvince()",MAP)
        self.assertIn("public void centerOnPlayer()",MAP)

    def test_same_game_places_and_filters_are_preserved(self):
        self.assertIn("for(WorldAtlas.Place p:WorldAtlas.all())",MAP)
        self.assertIn("game.progress.location",MAP)
        self.assertIn("!hiddenTypes.contains(p.type)",MAP)
        self.assertNotIn("game.progress.location=",MAP)
        self.assertIn("new SkyrimTerrainMapView(",UI)
        self.assertIn("mapControl(mapActions",UI)
        self.assertIn("terrainMap.zoomBy",UI)
        self.assertIn("terrainMap::showWholeProvince",UI)
        self.assertIn("terrainMap::centerOnPlayer",UI)
        self.assertIn("openFullscreenWorldMap()",UI)
        self.assertIn("this::locationDialog",UI)
        self.assertIn("TravelPlanner.plan(game,place)",UI)

    def test_extra_real_names_are_marked_as_coarse_reference_not_fake_travel(self):
        pins=re.findall(r'"([A-Za-z0-9][^"\n]*?)\|([A-L][1-8])"',CATALOG)
        self.assertGreater(len(pins),270)
        self.assertEqual(len(pins),len(set(name for name,grid in pins)))
        for name in ("Bleak Falls Barrow","Fort Greenwall","Goldenglow Estate",
                     "Nightgate Inn","Thieves Guild","Snow Veil Sanctum",
                     "Ysgramor's Tomb"):
            self.assertIn(name,CATALOG)
        self.assertIn("catalogClusters()",MAP)
        self.assertIn("catalogCallback.selected(catalogBest)",MAP)
        self.assertIn("if(hiddenTypes.contains(place.type))continue",MAP)
        self.assertIn("SkyrimMapCatalog.size()",UI)
        self.assertIn("for(SkyrimMapCatalog.Site site:SkyrimMapCatalog.all())types.add(site.type)",UI)
        self.assertIn("пока не является отдельным",UI)
        self.assertIn("private void searchSkyrimAtlas()",UI)
        self.assertIn("private void atlasSitesDialog(",UI)
        self.assertIn("private void atlasSiteDialog(",UI)
        self.assertIn("put(\"Хелген\"",WORLD)

if __name__=="__main__":
    unittest.main()
