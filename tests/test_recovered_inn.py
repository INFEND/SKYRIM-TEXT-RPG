import hashlib,json,pathlib,unittest,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
class RecoveredInn(unittest.TestCase):
    def test_exact_original_frame_if_published(self):
        path=ROOT/"app/src/main/assets/gallery/sleeping_giant_inn.zip"
        registry=json.loads((ROOT/"docs/gallery_registry.json").read_text())
        entry=next((e for e in registry if e["slug"]=="sleeping_giant_inn"),None)
        self.assertEqual(path.exists(),entry is not None)
        if entry is None:return
        self.assertEqual(entry["parent"],"Ривервуд")
        self.assertEqual(len(entry["captions"]),1)
        with zipfile.ZipFile(path) as z:
            self.assertEqual(z.namelist(),["01.webp"])
            data=z.read("01.webp")
        self.assertEqual(hashlib.sha256(data).hexdigest(),"63e9f5879607fd7f9529f5262b2cad086cfdc902ec0fb008889e50422ef1f7d8")
    def test_children_are_separate(self):
        s=(ROOT/"app/src/main/java/dev/infend/skyrimtext/LocationGalleryView.java").read_text()
        self.assertIn("addInteriorLinks()",s)
        self.assertIn("openInterior(d.name)",s)
