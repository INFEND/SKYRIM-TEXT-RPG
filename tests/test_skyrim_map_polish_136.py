"""Map v1.0.36: visible-only taps and true search-to-camera navigation."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
MAP=(SRC/"SkyrimTerrainMapView.java").read_text(encoding="utf-8")
CAT=(SRC/"SkyrimMapCatalog.java").read_text(encoding="utf-8")
UI=(SRC/"MainActivity.java").read_text(encoding="utf-8")


class TerrainMapPolish(unittest.TestCase):
    def test_hidden_markers_are_not_tappable(self):
        self.assertIn("private boolean drawn(WorldAtlas.Place p,float zoom)",MAP)
        self.assertIn("if(!drawn(p,zoom))continue;",MAP)
        draw=MAP.split("private void select(float x,float y)",1)[0]
        hit=MAP.split("private void select(float x,float y)",1)[1]
        self.assertIn("if(!drawn(p,zoom))continue;",draw)
        self.assertIn("if(!drawn(p,zoom))continue;",hit)
        self.assertIn("if(zoom>=2.5f||player(p)||major(p))return true;",MAP)
        self.assertIn("if(scale/Math.max(.0001f,minScale)>=1.6f)",MAP)
        self.assertNotIn("game.progress.location=",MAP)

    def test_catalog_can_focus_its_sector_without_faking_coordinates(self):
        self.assertIn("public void centerOn(SkyrimMapCatalog.Site target)",MAP)
        self.assertIn("target.x*scale",MAP)
        self.assertIn("target.y*scale",MAP)
        self.assertIn("constrain();invalidate();persist();",MAP)
        self.assertIn("public static int visibleCount(java.util.Set<String> hiddenTypes)",CAT)
        self.assertIn("SkyrimMapCatalog.visibleCount(hiddenMapTypes)",UI)

    def test_search_focuses_existing_map_and_fullscreen_can_search(self):
        self.assertIn("private void searchSkyrimAtlas(SkyrimTerrainMapView activeMap)",UI)
        self.assertIn("()->searchSkyrimAtlas(terrainMap)",UI)
        self.assertIn('mapControl(toolbar,"Поиск",()->searchSkyrimAtlas(map))',UI)
        self.assertIn("if(activeMap!=null)activeMap.centerOn(place)",UI)
        self.assertIn("if(activeMap!=null)activeMap.centerOn(site)",UI)
        self.assertIn("searchWindow[0].dismiss()",UI)
        self.assertIn("new RuneDialogBuilder(this,accent())",UI)

    def test_camera_saved_when_screen_or_dialog_is_closed(self):
        self.assertIn("@Override protected void onDetachedFromWindow()",MAP)
        self.assertIn("persist();",MAP)
        self.assertIn("terrain_map_zoom",MAP)
        self.assertIn("terrain_map_x",MAP)
        self.assertIn("terrain_map_y",MAP)


if __name__=="__main__":
    unittest.main()
