from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(ROOT/"MainActivity.java").read_text()
class ItemDetailsCompact(unittest.TestCase):
    def test_conditional_comparison_and_potions(self):
        for token in ('if(changed){change.setGravity','game.healingAmount()','game.staminaPotionAmount()','if("Зелье магии".equals(item.id))return 35','Math.min(restoration,missing)','Справочный эффект:','В наличии:','customPanel().setView(itemScroll)'):
            self.assertIn(token,UI)
    def test_single_custom_frame(self):
        self.assertIn('customPanel?new android.graphics.drawable.ColorDrawable',(ROOT/"RuneDialogBuilder.java").read_text())
        self.assertIn('case "health":',(ROOT/"ItemStatGlyph.java").read_text())
