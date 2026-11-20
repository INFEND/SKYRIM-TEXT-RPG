import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(ROOT/"MainActivity.java").read_text(encoding="utf-8")
ENGINE=(ROOT/"GameEngine.java").read_text(encoding="utf-8")
MAP=(ROOT/"SkyrimTerrainMapView.java").read_text(encoding="utf-8")

class VisualOptions(unittest.TestCase):
    def test_every_toggle_controls_rendering(self):
        for key in ("portraits","narrative","choices","reveal","stars","gear","damage","victory","chronicle","reputation","loot","trade","defeat","notice"):
            self.assertIn('{"'+key+'",',UI)
            self.assertIn('visual("'+key+'")',UI)
        self.assertNotIn('"Живая погода"',UI)
        self.assertNotIn('visual("weather")',UI)
        for removed in ("status","fog","travel"):
            self.assertNotIn('{"'+removed+'",',UI)
            self.assertIn('"'+removed+'".equals(key)',UI)
        self.assertNotIn('visual_fog',MAP)

    def test_legacy_save_remains_compatible(self):
        for key in ("visitedVisual","visualChronicle"):
            self.assertIn('.put("'+key+'"',ENGINE)
            self.assertIn('j.optJSONArray("'+key+'")',ENGINE)
        self.assertIn('visitedVisual.clear()',ENGINE)
        self.assertIn('visualChronicle.length()>=80',ENGINE)
