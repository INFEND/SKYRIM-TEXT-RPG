"""Location rows must match the icon on the map, with no redundant visual chrome."""
from pathlib import Path
import unittest

SRC=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
MAIN=(SRC/"MainActivity.java").read_text(encoding="utf-8")
MAP=(SRC/"SkyrimTerrainMapView.java").read_text(encoding="utf-8")


class LocationListVisualParity(unittest.TestCase):
    def test_uses_exact_same_glyph_mapping_as_map(self):
        self.assertIn("public static String iconForSite(SkyrimMapCatalog.Site site)",MAP)
        self.assertIn('pin.player?"маркер игрока":iconForSite(site)',MAP)
        self.assertIn('String glyph=SkyrimTerrainMapView.iconForSite(site)',MAIN)
        self.assertIn('new SkyrimIconView(this,glyph,0xffd5dfe1)',MAIN)

    def test_map_rows_have_single_left_icon_and_title_only(self):
        section=MAIN.split("private void mapLocationRow(",1)[1].split(
            "private void mapControl(",1)[0]
        self.assertEqual(section.count("new SkyrimIconView("),1)
        self.assertIn("tile.addView(new SkyrimIconView(",section)
        self.assertIn("tile.addView(name,nameParams)",section)
        self.assertIn("tile.setContentDescription(site.name)",section)
        self.assertNotIn('text("◇"',section)
        self.assertNotIn("site.grid",section)
        self.assertNotIn("site.type+",section)
        self.assertNotIn("route.days",section)
        self.assertNotIn("addText(info",section)
        self.assertNotIn("actionButton(",section)
        self.assertIn("name.setSingleLine(true)",section)

    def test_list_and_search_both_use_identical_location_rows(self):
        listing=MAIN.split("private void renderMap()",1)[1].split(
            "private void mapLocationRow(",1)[0]
        self.assertIn("mapLocationRow(content,site,",listing)
        self.assertNotIn('actionButton(WorldAtlas.icon(site.type)',listing)
        self.assertNotIn('String detail=site.type',listing)
        searching=MAIN.split("private void searchSkyrimAtlas(SkyrimTerrainMapView",1)[1].split(
            "/** Full atlas type filter",1)[0]
        self.assertIn("mapLocationRow(matches,site,",searching)
        self.assertNotIn('text(site.name+"\\n"+site.type',searching)


if __name__=="__main__":
    unittest.main()
