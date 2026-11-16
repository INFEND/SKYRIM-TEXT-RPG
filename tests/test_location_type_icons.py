"""Approved location glyphs and map/list shared mapping."""
from pathlib import Path
import hashlib
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class LocationTypeIconTest(unittest.TestCase):
    def test_atlas(self):
        raw=(ROOT/"app/src/main/res/drawable-nodpi/location_icons.webp").read_bytes()
        self.assertEqual(len(raw),3568)
        self.assertEqual(hashlib.sha256(raw).hexdigest(),"ac5f4cce08d88e4a7fb66aa23db4b3771e3a0b1d8ae75bb64e21f09be2cf8d3b")
        self.assertEqual(raw[:4],b"RIFF")
        self.assertEqual(raw[8:16],b"WEBPVP8L")
        header=int.from_bytes(raw[21:25],"little")
        self.assertEqual((header&0x3fff)+1,288)
        self.assertEqual(((header>>14)&0x3fff)+1,192)
        self.assertTrue((header>>28)&1)
    def test_six_distinct_slots_and_types(self):
        src=(SRC/"LocationTypeIcons.java").read_text(encoding="utf-8")
        self.assertEqual(dict(re.findall(r'case PREFIX\+"([a-z]+)":return (\d+);',src)),{"fort":"0","farm":"1","camp":"2","giantcamp":"3","cave":"4","stones":"5"})
        for name in ("Форт","Ферма","Лагерь","Лагерь великанов","Пещера","Камень"):
            self.assertIn('case "'+name+'"',src)
        self.assertIn('site.sheet==0?PREFIX+"stones":null',src)
        settlement=(SRC/"SettlementIcons.java").read_text(encoding="utf-8")
        self.assertIn("LocationTypeIcons.keyForSite(site)",settlement)
        self.assertIn("LocationTypeIcons.draw(context,canvas,key,width,height)",settlement)

if __name__=="__main__":unittest.main()
