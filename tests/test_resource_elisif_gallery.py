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
        self.assertIn('ElisifGallery.photo(this,ElisifGallery.primary(this))',m)
        self.assertIn('DEFAULT=11',s)
        self.assertIn('ElisifGallery.select(this,index)',m)
        self.assertIn('ElisifGallery.reset(this)',m)
        self.assertIn('ElisifGallery.navigate(this,index,-1)',m)
        self.assertIn('media/elisif/portrait.png',s)
        self.assertIn('"media:elisif"',m)
        self.assertIn('media:elisif',(ROOT/'scripts/build_resource_catalog.py').read_text())
    def test_optional_archive(self):
        p=ROOT/'app/src/main/assets/media/elisif/gallery.zip'
        if not p.exists(): self.skipTest('Optional gallery transfer not installed yet')
        self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),'8e6c4f75b0cd8f93b650a4b62908851e0ca1c4ae254fd12d0b6d00b47a790e1a')
        hashes=re.findall(r'"([0-9a-f]{64})"',(JAVA/'ElisifGallery.java').read_text().split('String[] SHA={',1)[1].split('};',1)[0])
        with zipfile.ZipFile(p) as z:
            self.assertEqual(z.namelist(),[f'media/elisif/{i:02d}.jpg' for i in range(1,11)])
            self.assertIsNone(z.testzip())
            self.assertEqual([hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist()],hashes)

    def test_new_original_portrait(self):
        p=ROOT/'app/src/main/assets/media/elisif/portrait.png'
        self.assertTrue(p.is_file())
        raw=p.read_bytes()
        self.assertEqual(hashlib.sha256(raw).hexdigest(),'ebccdfe23435e7412d343ad885a4ab089f0250c20f6bdee08546221ef3b2b2aa')
        self.assertEqual(raw[:8],b'\x89PNG\r\n\x1a\n')
        self.assertEqual((int.from_bytes(raw[16:20],'big'),int.from_bytes(raw[20:24],'big')),(1122,1402))
        self.assertIn('ebccdfe23435e7412d343ad885a4ab089f0250c20f6bdee08546221ef3b2b2aa',(ROOT/'scripts/build_resource_catalog.py').read_text())
