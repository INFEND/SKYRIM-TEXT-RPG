"""Regression checks for the approved city and settlement replacement."""
import hashlib
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class SettlementIconsTest(unittest.TestCase):
    def test_approved_atlas_is_compact_and_exact(self):
        raw=(ROOT/"app/src/main/res/drawable-nodpi/settlement_icons.webp").read_bytes()
        self.assertEqual(len(raw),5720)
        self.assertEqual(hashlib.sha256(raw).hexdigest(),"471b9c70466f559736edfbafb115c851aaadfa693ed0ff230a2cd1e80962a615")
        self.assertEqual(raw[:4],b"RIFF")
        self.assertEqual(raw[8:16],b"WEBPVP8L")
        header=int.from_bytes(raw[21:25],"little")
        self.assertEqual((header&0x3fff)+1,384)
        self.assertEqual(((header>>14)&0x3fff)+1,288)
        self.assertTrue((header>>28)&1)

    def test_nine_unique_capitals_and_two_generic_markers(self):
        src=(SRC/"SettlementIcons.java").read_text(encoding="utf-8")
        cities={"Solitude":"solitude","Dawnstar":"dawnstar","Winterhold":"winterhold",
                "Windhelm":"windhelm","Markarth":"markarth","Morthal":"morthal",
                "Whiterun":"whiterun","Riften":"riften","Falkreath":"falkreath"}
        for name,key in cities.items():
            self.assertIn('case "'+name+'": return PREFIX+"'+key+'";',src)
        slots=re.findall(r'case PREFIX\+"([a-z]+)":return (\d+);',src)
        self.assertEqual(len(slots),11)
        self.assertEqual(sorted(int(i) for _,i in slots),list(range(11)))
        self.assertEqual(dict(slots)["town"],"9")
        self.assertEqual(dict(slots)["settlement"],"10")

    def test_map_and_list_share_the_new_glyphs(self):
        mapping=(SRC/"SkyrimTerrainMapView.java").read_text(encoding="utf-8")
        atlas=(SRC/"SkyrimIconAtlas.java").read_text(encoding="utf-8")
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("SettlementIcons.keyForSite(site)",mapping)
        self.assertIn("SettlementIcons.draw(context,canvas,key,width,height)",atlas)
        self.assertIn("String glyph=SkyrimTerrainMapView.iconForSite(site)",ui)
        self.assertIn('pin.player?"маркер игрока":iconForSite(site)',mapping)

if __name__=="__main__":
    unittest.main()
