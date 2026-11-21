import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class LocalCharacterArtworkTest(unittest.TestCase):
    def test_replacement_preserves_original_bytes_and_old_on_invalid_input(self):
        s = (JAVA / "LocalCharacterGallery.java").read_text()
        section = s[s.index("static synchronized File replace("):s.index("static synchronized boolean remove(")]
        self.assertIn("out.write(b,0,n)", section)
        self.assertNotIn(".compress(", section)
        self.assertLess(section.index("Bitmap preview=photo(tmp)"), section.index("old.delete()"))
        self.assertLess(section.index("tmp.renameTo(dest)"), section.index("old.delete()"))
        self.assertIn("if(dest.exists())throw", section)
    def test_local_portrait_and_picker(self):
        m = (JAVA / "MainActivity.java").read_text()
        self.assertIn("REQUEST_ITACHI_REPLACE_ART", m)
        self.assertIn("LocalCharacterGallery.replace(this,target,uri)", m)
        self.assertIn("LocalCharacterGallery.selected(this)", m)
        self.assertIn("LocalCharacterGallery.select(this,isPrimary?null:files[index])", m)
        self.assertIn("itachi_replace_target", m)
        self.assertIn("Управление фотографиями Итачи", m)
