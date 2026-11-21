"""Preserve the v1.0.37 artistic source while using geographic terrain for pins."""
import hashlib
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/"app/src/main/assets/maps/skyrim_user_mosaic.webp"
MAP=(ROOT/"app/src/main/java/dev/infend/skyrimtext/SkyrimTerrainMapView.java").read_text()

class CompletedOfflineTerrain(unittest.TestCase):
    def test_previous_artistic_source_is_preserved(self):
        data=ART.read_bytes()
        self.assertTrue(850_000<len(data)<2_500_000)
        self.assertEqual(hashlib.sha256(data).hexdigest(),
            "2a05447a23ce0df34a4f8e4a8f79c11a5078a8f56838b55a2f25d3d473e3321f")
    def test_geographic_terrain_has_independent_interactive_pins(self):
        self.assertIn('TERRAIN="maps/skyrim_geographic.webp"',MAP)
        self.assertIn("MapPinLayout.build",MAP)
        self.assertIn("SkyrimIconAtlas.drawMarker",MAP)
        self.assertIn("private void zoomAt(",MAP)
        self.assertIn("private void constrain()",MAP)
        self.assertIn("public void centerOnPlayer()",MAP)
        self.assertIn("public void showWholeProvince()",MAP)
