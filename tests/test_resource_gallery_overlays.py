"""Regression guard: gallery overlay archives must never break the signed resource catalog."""
import collections
import importlib.util
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
spec=importlib.util.spec_from_file_location("catalog",ROOT/"scripts/build_resource_catalog.py")
catalog=importlib.util.module_from_spec(spec)
spec.loader.exec_module(catalog)

class ResourceGalleryOverlayTest(unittest.TestCase):
    def test_real_catalog_archive_references(self):
        data=catalog.build("a"*40,thin=True)
        data["galleries"]=catalog.gallery_registry()
        catalog.validate_archive_references(data)
        entries={entry["path"]:entry for entry in data["files"]}
        references=collections.Counter(
            archive if archive.startswith("media/") else "gallery/"+archive
            for gallery in data["galleries"] for archive in gallery["archives"]
        )
        self.assertEqual(references["gallery/whiterun.zip"],4)
        self.assertEqual(references["media/whiterun/replacement.zip"],2)
        self.assertEqual(entries["gallery/whiterun.zip"]["units"],6)
        self.assertEqual(entries["media/whiterun/replacement.zip"]["units"],2)

    def test_android_accepts_overlays_without_invalidating_fonts(self):
        src=(JAVA/"ResourceStore.java").read_text(encoding="utf-8")
        self.assertIn('archive.startsWith("media/")?archive:"gallery/"+archive',src)
        self.assertIn('frame.getValue()>archive.units',src)
        self.assertIn('count>e.units',src)
        self.assertIn('e.units=count;',src)
        font=(JAVA/"SovngardeTypeface.java").read_text(encoding="utf-8")
        self.assertIn('ResourceStore.localFile(context,path)',font)
        self.assertIn('ResourceStore.generation()',font)

if __name__=="__main__":
    unittest.main()
