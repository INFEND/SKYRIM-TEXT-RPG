"""User-supplied Whiterun artwork is routed without changing the legacy six-frame gallery."""
import hashlib
import importlib.util
import json
import pathlib
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SOURCE = ROOT / "app/src/main/java/dev/infend/skyrimtext/LocationGalleryView.java"
spec = importlib.util.spec_from_file_location("catalog", ROOT / "scripts/build_resource_catalog.py")
catalog = importlib.util.module_from_spec(spec)
spec.loader.exec_module(catalog)

class WhiterunArtwork(unittest.TestCase):
    def test_new_frames_and_signed_mapping(self):
        gallery = next(e for e in catalog.gallery_registry() if e["slug"] == "whiterun")
        self.assertEqual(gallery["captions"][:2], ["Главные ворота", "Главная улица"])
        self.assertEqual(gallery["cityPlaces"][:2], ["Главные ворота", "Главная улица"])
        self.assertEqual(gallery["archives"], ["media/whiterun/replacement.zip"] * 2 + ["whiterun.zip"] * 4)
        with zipfile.ZipFile(ASSETS / "media/whiterun/replacement.zip") as z:
            self.assertIsNone(z.testzip())
            self.assertEqual(set(z.namelist()), {"01.webp", "02.webp"})
            for n in (1,2):
                data = z.read(f"{n:02d}.webp")
                self.assertEqual(data[:4], b"RIFF")
                self.assertEqual(data[8:12], b"WEBP")
                self.assertEqual(hashlib.sha256(data).hexdigest(), gallery["artwork"]["frames"][n-1]["sha256"])

    def test_gallery_reads_exact_catalog_archive(self):
        source = SOURCE.read_text(encoding="utf-8")
        self.assertIn('return archive.startsWith("media/")?archive:"gallery/"+archive;', source)
        self.assertEqual(source.count("archivePath(slug,"), 4)
        self.assertIn('{"Главные ворота","Главная улица","Рынок"', source)
        self.assertIn('?"media:whiterun":"gallery:"+slug', source)

if __name__ == "__main__":
    unittest.main()
