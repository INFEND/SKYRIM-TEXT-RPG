from pathlib import Path
import hashlib, re, unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/dev/infend/skyrimtext'
class NpcAppearancesTest(unittest.TestCase):
    def test_wiring(self):
        s=(JAVA/'NpcAppearance.java').read_text()
        m=(JAVA/'MainActivity.java').read_text()
        for slug,name in [('astrid','астрид'),('vex','векс'),('karliah','карлия'),('nocturnal','ноктюрнал'),('elisif','элисиф')]:
            self.assertIn('"'+slug+'"',s)
            self.assertIn('case "'+name+'"',s)
        self.assertIn('NpcAppearance.photo(this,name)',m)
        self.assertIn('NpcAppearance.install(this,uri)',m)
        self.assertIn('media:npc',(ROOT/'scripts/build_resource_catalog.py').read_text())
    def test_optional_assets(self):
        s=(JAVA/'NpcAppearance.java').read_text()
        hashes=re.findall(r'"([0-9a-f]{64})"',s)
        self.assertEqual(len(hashes),5)
        for slug,digest in zip(('astrid','vex','karliah','nocturnal','elisif'),hashes):
            p=ROOT/'app/src/main/assets/media/npc'/(slug+'.webp')
            if p.exists():self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),digest)
