"""Reference item-card layout and optional image assets."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
UI=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text()
class ItemReference(unittest.TestCase):
    def test_reference_layout(self):
        for token in ('itemPicture(item,54)','itemPicture(item,94)','new ItemStatGlyph','SovngardeTypeface.styled(this,Typeface.ITALIC)','Typeface.ITALIC','item.weight-worn.weight','item.value-worn.value','actions.addView(equip','actions.addView(discard','itemDialog(item);'):
            self.assertIn(token,UI)
        self.assertNotIn('NATIVE_ITEM_FONT',UI)
        item_text=UI.split('private TextView itemText(',1)[1].split('private android.graphics.drawable.GradientDrawable',1)[0]
        self.assertIn('text(value,size,color,bold)',item_text)
        self.assertNotIn('setTypeface',item_text)
    def test_art_externalized(self):
        for n in ('ebony_sword','ebony_armor','steel_dagger','daedric_sword'):
            self.assertTrue((ROOT/'app/src/main/assets/media/items'/ (n+'.webp')).is_file())
        self.assertIn('ResourceStore.open(this,path)',UI)
