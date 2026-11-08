"""The 48 original icons from the supplied archive are embedded as one transparent atlas."""
from pathlib import Path
import unittest
import hashlib
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ASSET=ROOT/"app/src/main/assets/icons/skyrim_icons_48.webp"

class UserIconAtlasContracts(unittest.TestCase):
    def test_exact_image_is_present_compact_and_transparent(self):
        atlas=ASSET.read_bytes()
        self.assertEqual(hashlib.sha256(atlas).hexdigest(),
                         "ed824d0b38eaed47544c8050185f0657f6ddbad649189905e4c3ec71614c5bf3")
        self.assertLess(len(atlas),235000)
        self.assertEqual(atlas[:4],b"RIFF")
        self.assertEqual(atlas[8:12],b"WEBP")
        self.assertEqual(atlas[12:16],b"VP8X")
        self.assertTrue(atlas[20]&0x10)  # VP8X alpha flag
        self.assertEqual(1+int.from_bytes(atlas[24:27],"little"),1024)
        self.assertEqual(1+int.from_bytes(atlas[27:30],"little"),768)

    def test_all_48_named_images_are_addressable(self):
        icons=(SRC/"SkyrimIconView.java").read_text(encoding="utf-8")
        self.assertIn("TOTAL=48",icons)
        self.assertIn('ASSET="icons/skyrim_icons_48.webp"',icons)
        keys=icons.split("private static final String[] KEYS={",1)[1].split("};",1)[0]
        import re
        names=re.findall(r'"([a-z]+)"',keys)
        self.assertEqual(len(names),48)
        self.assertEqual(len(set(names)),48)
        self.assertEqual(names[:8],["story","map","bag","codex","hero","journal","settings","dragon"])
        self.assertEqual(names[-1],"favorites")
        self.assertIn("canvas.drawBitmap(b,crop,dest,paint)",icons)
        self.assertIn("Color.TRANSPARENT",icons)

    def test_inventory_uses_specific_icons_and_all_tabs_are_replaced(self):
        activity=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("SkyrimIconView.iconForLabel(title)",activity)
        self.assertIn("SkyrimIconView.iconForItem(item.name,item.category)",activity)
        self.assertIn("SkyrimIconView.iconForItem(focused.name,focused.category)",activity)
        self.assertIn('group.equals("Все")?"bag":iconOf(group)',activity)
        for name in ('"story"','"map"','"bag"','"codex"','"hero"','"journal"','"settings"'):
            self.assertIn(name,activity)

if __name__=="__main__":
    unittest.main()
