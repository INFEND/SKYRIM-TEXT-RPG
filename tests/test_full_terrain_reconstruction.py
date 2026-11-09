"""The 1.0.37 background must be filled, bundled and independent of live pins."""
import hashlib
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/"app/src/main/assets/maps/skyrim_user_mosaic.webp"
MAP=(ROOT/"app/src/main/java/dev/infend/skyrimtext/SkyrimTerrainMapView.java").read_text(encoding="utf-8")
UI=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")


class CompletedOfflineTerrain(unittest.TestCase):
    def test_background_contains_new_full_province_composite(self):
        data=ART.read_bytes()
        self.assertTrue(850_000<len(data)<2_500_000)
        self.assertEqual(data[:4],b"RIFF")
        self.assertEqual(data[8:12],b"WEBP")
        self.assertEqual(hashlib.sha256(data).hexdigest(),
            "2a05447a23ce0df34a4f8e4a8f79c11a5078a8f56838b55a2f25d3d473e3321f")

    def test_map_stays_interactive_with_separate_markers(self):
        self.assertIn('TERRAIN="maps/skyrim_user_mosaic.webp"',MAP)
        self.assertIn("MAP_W=3500",MAP)
        self.assertIn("MAP_H=1720",MAP)
        self.assertIn("for(WorldAtlas.Place p:WorldAtlas.all())",MAP)
        self.assertIn("catalogClusters()",MAP)
        self.assertIn("private void zoomAt(",MAP)
        self.assertIn("private void constrain()",MAP)
        self.assertIn("public void centerOnPlayer()",MAP)
        self.assertIn("public void showWholeProvince()",MAP)
        self.assertIn("openFullscreenWorldMap()",UI)
        self.assertIn("Скайрима",UI)
        self.assertIn("восстановлены приблизительно",UI)


if __name__=="__main__":
    unittest.main()
