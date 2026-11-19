import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
UI = (ROOT / "app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text()

class AuthorNpcCardTests(unittest.TestCase):
    def test_portrait_not_center_cropped(self):
        card = UI.split("private void npcDetails(String name)", 1)[1].split("private void npcGallerySection", 1)[0]
        self.assertIn("ScaleType.FIT_CENTER", card)
        self.assertNotIn("ScaleType.CENTER_CROP", card)
        self.assertIn('role.setTag("npc_no_self_link")', card)

    def test_author_sections_and_progress(self):
        self.assertIn("record.author){npcAuthorSection(name,index)", UI)
        section = UI.split("private void npcAuthorSection", 1)[1].split("private void npcLegacyDetails", 1)[0]
        for token in ("KatherineCompanion.SPELLS.length", "c.limitSpell(k.magicRank)", "KatherineCompanion.GEAR[c.gear]", "KatherineCompanion.FOCUS[c.focus]", "i.memories", "l.history", "k.memory", "a.memory", "npcAuthorKnown(name)"):
            self.assertIn(token, section)
        self.assertIn('title.setTag("npc_no_self_link")', UI)
        self.assertIn("setCustomTitle(title)", UI)

if __name__ == "__main__":
    unittest.main()
