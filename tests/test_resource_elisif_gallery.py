from pathlib import Path
import hashlib, re, unittest, zipfile
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/dev/infend/skyrimtext'
class ElisifGalleryTest(unittest.TestCase):
    def test_wiring(self):
        s=(JAVA/'ElisifGallery.java').read_text()
        m=(JAVA/'MainActivity.java').read_text()
        self.assertIn('COUNT=10',s)
        self.assertIn('media/elisif/gallery.zip',s)
        self.assertIn('ElisifGallery.photo(this,1)',m)
        self.assertIn('"media:elisif"',m)
        self.assertIn('media:elisif',(ROOT/'scripts/build_resource_catalog.py').read_text())
    def test_optional_archive(self):
        p=ROOT/'app/src/main/assets/media/elisif/gallery.zip'
        if not p.exists(): self.skipTest('Optional gallery transfer not installed yet')
        self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),'8e6c4f75b0cd8f93b650a4b62908851e0ca1c4ae254fd12d0b6d00b47a790e1a')
        hashes=re.findall(r'"([0-9a-f]{64})"',(JAVA/'ElisifGallery.java').read_text())
        with zipfile.ZipFile(p) as z:
            self.assertEqual(z.namelist(),[f'media/elisif/{i:02d}.jpg' for i in range(1,11)])
            self.assertIsNone(z.testzip())
            self.assertEqual([hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist()],hashes)
