import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(ROOT/"MainActivity.java").read_text(encoding="utf-8")
ENGINE=(ROOT/"GameEngine.java").read_text(encoding="utf-8")
MAP=(ROOT/"SkyrimTerrainMapView.java").read_text(encoding="utf-8")

class VisualOptions(unittest.TestCase):
    def test_every_toggle_controls_rendering(self):
        for key in ("weather","status","portraits","narrative","choices","reveal","stars","gear","damage","victory","travel","chronicle","reputation","loot","trade","defeat","notice"):
            self.assertIn('{"'+key+'",',UI)
            self.assertIn('visual("'+key+'")',UI)
        self.assertIn('{"fog",',UI)
        self.assertIn('prefs.getBoolean("visual_fog",true)',MAP)

    def test_legacy_save_remains_compatible(self):
        for key in ("visitedVisual","visualChronicle"):
            self.assertIn('.put("'+key+'"',ENGINE)
            self.assertIn('j.optJSONArray("'+key+'")',ENGINE)
        self.assertIn('visitedVisual.clear()',ENGINE)
        self.assertIn('visualChronicle.length()>=80',ENGINE)
