from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(ROOT/"MainActivity.java").read_text()
class UnifiedItemDialog(unittest.TestCase):
    def test_category_and_subtype_in_dialog_header(self):
        dialog=UI.split("private void itemDialog(",1)[1].split("private void showItemCatalog(",1)[0]
        self.assertIn("itemType(item)+(subtype.isEmpty()",dialog)
        self.assertIn("itemEffectPreview(view,item)",dialog)
    def test_dynamic_rows(self):
        for token in ("ItemEffectRow.parse(item.effect)","ЭФФЕКТЫ И СВОЙСТВА","effect.duration","effect.value","ItemEffectStyle.group(effect)","setSecondaryProgress"):
            self.assertIn(token,UI)
