import pathlib, re, unittest, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/"app/src/main/assets"

class TitleArtTest(unittest.TestCase):
    def test_all_title_frames_are_valid_and_consistent(self):
        import ast
        script=(ROOT/"scripts/build_resource_catalog.py").read_text(encoding="utf-8")
        tree=ast.parse(script)
        frames=next(ast.literal_eval(node.value) for node in tree.body if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=="TITLE_ART_FRAMES" for t in node.targets))
        java=(ROOT/"app/src/main/java/dev/infend/skyrimtext/TitleArtView.java").read_text(encoding="utf-8")
        def values(key):
            return re.findall(r'"([^"]+)"',re.search(key+r'=\{([^}]+)\}',java).group(1))
        self.assertEqual(list(zip(values("SLUGS"),values("FRAMES"))),frames)
        self.assertEqual(len(values("NAMES")),len(frames))
        for slug,frame in frames:
            with zipfile.ZipFile(ASSETS/"gallery"/(slug+".zip")) as z:
                raw=z.read(frame)
            self.assertEqual(raw[:4],b"RIFF")
            self.assertEqual(raw[8:12],b"WEBP")
            self.assertTrue(16000<len(raw)<2000000)
        self.assertIn("bundle_title_art()",script[script.index("def main():"):])
        activity=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")
        title=activity.split("private void renderTitle(){",1)[1].split("private void renderNewGame(){",1)[0]
        self.assertIn("new TitleArtView",title)
        self.assertNotIn("NordicArtView.TITLE",title)
