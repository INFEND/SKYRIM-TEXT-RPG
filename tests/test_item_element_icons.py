from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class ItemElementIcons(unittest.TestCase):
    def test_effect_style_is_used(self):
        ui=(ROOT/"MainActivity.java").read_text()
        self.assertIn("ItemEffectStyle.classify(effect,item.name)",ui)
        self.assertIn("style.glyph,style.color",ui)
        self.assertIn("ItemEffectStyle.damageEnchantment(e)",ui)
    def test_icons(self):
        glyph=(ROOT/"ItemStatGlyph.java").read_text()
        for name in ("fire","frost","shock","poison","sun","soul","absorb_health","absorb_magic","absorb_stamina"):
            self.assertIn('case "'+name+'"',glyph)
