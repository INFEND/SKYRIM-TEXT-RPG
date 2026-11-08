"""UI state: all generated screens preserve scroll and do not rerender on spinner init."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java")
FONT=(ROOT/"app/src/main/java/dev/infend/skyrimtext/SovngardeTypeface.java")
SCENE=(ROOT/"app/src/main/java/dev/infend/skyrimtext/SceneTypography.java")
WORK=(ROOT/".github/workflows/android-ota.yml")
class FontAndScrollContracts(unittest.TestCase):
    def test_vertical_and_horizontal_offsets_restored_before_first_frame(self):
        x=MAIN.read_text(encoding="utf-8")
        self.assertIn("rememberViewport();",x)
        self.assertIn("PageScroll oldScroll=scrollCache.get(thisPage);",x)
        self.assertIn("findHorizontal(target,found)",x)
        self.assertIn("getScrollY()",x)
        self.assertIn("getScrollX()",x)
        self.assertIn("target.getViewTreeObserver().addOnPreDrawListener(",x)
        self.assertIn("target.scrollTo(0,Math.min",x)
        self.assertIn("scroll.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS)",x)
        self.assertNotIn("scroll.scrollTo(0,0)",x)
    def test_spinner_does_not_rebuild_screen_during_initial_layout(self):
        x=MAIN.read_text(encoding="utf-8")
        self.assertIn("final int[] previous={initial};",x)
        self.assertIn("if(pos==previous[0])return;",x)
        self.assertIn("previous[0]=pos;",x)
    def test_bundled_font_applied_to_every_native_text_view(self):
        manager=FONT.read_text(encoding="utf-8")
        main=MAIN.read_text(encoding="utf-8")
        workflow=WORK.read_text(encoding="utf-8")
        for file in ("Sovngarde Light.ttf","Sovngarde Bold.ttf"):
            self.assertIn(file,manager)
            self.assertIn(file,workflow)
        self.assertIn("SovngardeTypeface.applyTree(root)",main)
        self.assertIn("SovngardeTypeface.face(this,bold)",main)
        self.assertIn("sha256sum --check --strict",workflow)
        self.assertIn("Verify Sovngarde TTF and license inside debug APK",workflow)
    def test_speaker_separate_and_narrative_italic_without_quote_line(self):
        x=SCENE.read_text(encoding="utf-8")
        self.assertIn("new StyleSpan(Typeface.ITALIC)",x)
        self.assertIn("new StyleSpan(Typeface.BOLD)",x)
        self.assertNotIn("new QuoteSpan(",x)
        self.assertIn('line.startsWith(">")',x)
        self.assertIn('result.append("\\n")',x)
if __name__=="__main__":
    unittest.main()
