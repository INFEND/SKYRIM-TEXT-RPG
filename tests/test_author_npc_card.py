import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
UI = (ROOT / "app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text()

class AuthorNpcCardTests(unittest.TestCase):
    def test_portrait_not_center_cropped(self):
        card = UI.split("private void npcDetails(String name)", 1)[1].split("private void npcGallerySection", 1)[0]
        self.assertIn("refreshNpcPortrait()", card)
        frame = UI.split("private void refreshNpcPortrait()", 1)[1].split("private void closeNpcDialog()", 1)[0]
        # Adaptive portrait frames may center-crop overflow but must never distort.
        self.assertIn("npcFillPhoto((android.widget.ImageView)portrait)", frame)
        self.assertIn("npcPhotoHeight(portrait,holder.getWidth(),180,520)", frame)
        self.assertIn("image.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP)", UI)
        self.assertIn("drawable.getIntrinsicWidth()/(float)drawable.getIntrinsicHeight()", UI)
        self.assertIn('role.setTag("npc_no_self_link")', card)

    def test_gallery_photo_restored_to_full_frame(self):
        gallery = UI.split("private void npcGalleryPhoto(", 1)[1].split("private void refreshNpcPortrait()", 1)[0]
        self.assertIn("ScaleType.FIT_CENTER", gallery)
        self.assertNotIn("npcFillPhoto(image)", gallery)
        self.assertIn("panel.addView(image,lp(-1,heightDp))", gallery)
        for token in (
            "npcGalleryPhoto(panel,KatherineGallery.photo(this,index),440)",
            "npcGalleryPhoto(panel,SeranaGallery.photo(this,index),440)",
            "npcGalleryPhoto(panel,AlmaGallery.photo(this,index),400)",
            "npcGalleryPhoto(panel,ElisifGallery.photo(this,index),440)",
            "npcGalleryPhoto(panel,LiviaPortrait.photo(this),440)",
            "npcGalleryPhoto(panel,LocalCharacterGallery.photo(files[index]),380)",
        ):
            self.assertIn(token, UI)

    def test_author_sections_and_progress(self):
        self.assertIn("record.author){npcAuthorSection(name,index)", UI)
        section = UI.split("private void npcAuthorSection", 1)[1].split("private void npcLegacyDetails", 1)[0]
        for token in ("KatherineCompanion.SPELLS.length", "c.limitSpell(k.magicRank)", "KatherineCompanion.GEAR[c.gear]", "KatherineCompanion.FOCUS[c.focus]", "i.memories", "l.history", "k.memory", "a.memory", "npcAuthorKnown(name)"):
            self.assertIn(token, section)
        self.assertIn('title.setTag("npc_no_self_link")', UI)
        self.assertIn("setCustomTitle(title)", UI)

    def test_primary_gallery_and_terminal_state(self):
        for token in ('npcGalleryPrimary("alma",7,AlmaGallery.COUNT)', 'npcGalleryPrimary("katherine",1,KatherineGallery.COUNT)', 'npcGalleryChoice(panel,dialog,"alma"', 'npcGalleryChoice(panel,dialog,"katherine"', 'holder.removeAllViews()', 'interact.setEnabled(!blocked)'):
            self.assertIn(token, UI)

if __name__ == "__main__":
    unittest.main()
