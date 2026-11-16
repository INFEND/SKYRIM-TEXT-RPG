"""Prevent media bypass via Android resource directories."""
import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class CoreOnlyMedia(unittest.TestCase):
    def test_no_bulky_android_resources(self):
        for p in (ROOT/"app/src/main/res").rglob("*"):
            if not p.is_file():continue
            self.assertLessEqual(p.stat().st_size,64*1024,f"Externalize heavy Android resource {p}")
            # Approved 11-glyph navigation atlas is a core UI asset, not optional artwork.
            if p.relative_to(ROOT).as_posix() in {"app/src/main/res/drawable-nodpi/settlement_icons.webp",\
                    "app/src/main/res/drawable-nodpi/location_icons.webp"}:
                self.assertLessEqual(p.stat().st_size,8192)
                continue
            self.assertNotIn(p.suffix.lower(),{".webp",".jpg",".jpeg",".mp4",".mp3",".ogg",".zip",".gguf"},f"Externalize optional media {p}")
    def test_workflow_externalizes_before_compile(self):
        wf=(ROOT/".github/workflows/android-ota.yml").read_text()
        self.assertLess(wf.index('scripts/build_resource_catalog.py --commit "$GITHUB_SHA" --thin'),wf.index("name: Assemble debug APK"))
        self.assertNotIn("Preserve full APK",wf)
if __name__=="__main__":unittest.main()
