import hashlib
import pathlib
import re
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SRC = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class AlmaGalleryContract(unittest.TestCase):
    def test_exact_original_gallery(self):
        archive = ASSETS / "media/alma/gallery.zip"
        self.assertEqual(hashlib.sha256(archive.read_bytes()).hexdigest(),
                         "4910508a80bdcce0f01b802bad50bce41c6a0cafbf2c8a6fcfdfa0a861d25f81")
        source = (SRC / "AlmaGallery.java").read_text(encoding="utf-8")
        hashes = re.findall(r'"([0-9a-f]{64})"', source)
        self.assertEqual(len(hashes), 7)
        def name(i):
            return f"media/alma/{i:02}" + (".jpg" if i == 4 else ".webp")
        with zipfile.ZipFile(archive) as z:
            self.assertEqual(set(z.namelist()), {name(i) for i in range(1, 8)})
            self.assertIsNone(z.testzip())
            for i, digest in enumerate(hashes, 1):
                self.assertEqual(hashlib.sha256(z.read(name(i))).hexdigest(), digest)
    def test_download_and_recovery(self):
        source = (SRC / "MainActivity.java").read_text(encoding="utf-8")
        gallery = source.split("private void almaGallery(int index){", 1)[1].split("private static final int REQUEST_KATHERINE_GALLERY", 1)[0]
        self.assertIn('this::render,"media:alma"', gallery)
        self.assertIn('setPositiveButton("Скачать"', gallery)
        self.assertIn('setPositiveButton("Восстановить"', gallery)
        self.assertIn('bytes=readEntry(ResourceStore.open(c,REMOTE),index);verify(bytes,index)', (SRC / "AlmaGallery.java").read_text(encoding="utf-8"))

if __name__ == "__main__":
    unittest.main()
