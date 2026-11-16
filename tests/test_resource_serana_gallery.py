from pathlib import Path
import hashlib, zipfile, unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/dev/infend/skyrimtext'
class SeranaGalleryTest(unittest.TestCase):
    def test_wiring(self):
        s=(JAVA/'SeranaGallery.java').read_text()
        m=(JAVA/'MainActivity.java').read_text()
        self.assertIn('COUNT=13',s)
        self.assertIn('media/serana/gallery.zip',s)
        self.assertIn('SeranaGallery.photo(this,1)',m)
        self.assertIn('Галерея Сераны',m)
        self.assertIn('REQUEST_SERANA_GALLERY',m)
        self.assertIn('media:serana',(ROOT/'scripts/build_resource_catalog.py').read_text())
    def test_optional_archive(self):
        p=ROOT/'app/src/main/assets/media/serana/gallery.zip'
        if not p.exists(): self.skipTest('Optional gallery not yet uploaded')
        self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),'be77a3f3aa45a80767e43c5200a4440df97109a704b22cc16da7679abfc9bfaa')
        with zipfile.ZipFile(p) as z:
            self.assertEqual(z.namelist(),[f'media/serana/{i:02d}.webp' for i in range(1,14)])
            self.assertIsNone(z.testzip())
