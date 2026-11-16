import hashlib
import pathlib
import re
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SRC = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class KatherineGalleryContract(unittest.TestCase):
    def test_exact_original_gallery(self):
        archive = ASSETS / "media/katherine/gallery.zip"
        self.assertEqual(hashlib.sha256(archive.read_bytes()).hexdigest(),
                         "d68789d37c7dff7ffd8d517169b1ee974df65f56c815ef6131263e9eb6f68fa8")
        source = (SRC / "KatherineGallery.java").read_text(encoding="utf-8")
        hashes = re.findall(r'"([0-9a-f]{64})"', source)
        self.assertEqual(len(hashes), 10)
        with zipfile.ZipFile(archive) as z:
            self.assertEqual(set(z.namelist()), {f"media/katherine/{i:02}.webp" for i in range(1, 11)})
            self.assertIsNone(z.testzip())
            for i, digest in enumerate(hashes, 1):
                self.assertEqual(hashlib.sha256(z.read(f"media/katherine/{i:02}.webp")).hexdigest(), digest)
    def test_gallery_has_direct_signed_download_and_recovery(self):
        source = (SRC / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('this::render,"media:katherine"', source)
        self.assertIn('setPositiveButton("Скачать"', source)
        self.assertIn('setPositiveButton("Восстановить"', source)
        self.assertIn('ResourceStore.open(c,REMOTE)', (SRC / "KatherineGallery.java").read_text(encoding="utf-8"))

if __name__ == "__main__":
    unittest.main()
