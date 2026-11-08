"""Global Sovngarde font, Unicode Cyrillic and stable scroll after tapping."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"app/src/main/java/dev/infend/skyrimtext"
WORKFLOW=(ROOT/".github/workflows/android-ota.yml").read_text(encoding="utf-8")
def code(name):
    return (SOURCE/name).read_text(encoding="utf-8")

class GlobalFontScroll(unittest.TestCase):
    def test_both_exact_user_supplied_sovngarde_fonts_are_embedded(self):
        self.assertIn("Bundle exact user-supplied Sovngarde fonts",WORKFLOW)
        self.assertIn("e9fa97b3c312eb948f3808f5c2d1f053555d1a48",WORKFLOW)
        for fragment in (
            "8a080bba62534ca37bdd3137e933f9215dbf18e8a070495e4ae782ea7c3b9851",
            "a4abb4f2cf49f7e52d87f45d59b1fe2592d464c94f049094f66520072317c2ef",
            "beb729413bd1bb04bfecbfd530eb0b899777fdd57472c8496a26e2cd53ef8bd5",
            "sha256sum --check --strict",
            'grep -F "assets/fonts/Sovngarde Light.ttf"',
            'grep -F "assets/fonts/Sovngarde Bold.ttf"',
            'grep -F "assets/fonts/OFL.txt"',
        ):
            self.assertIn(fragment,WORKFLOW)

    def test_common_widgets_gallery_and_dialogs_use_sovngarde(self):
        font=code("SovngardeTypeface.java")
        main=code("MainActivity.java")
        dialog=code("RuneDialogBuilder.java")
        gallery=code("LocationGalleryView.java")
        for name in ("Sovngarde Light.ttf","Sovngarde Bold.ttf"):
            self.assertIn(name,font)
        self.assertIn("Typeface.createFromAsset",font)
        self.assertIn("SovngardeTypeface.face(this,bold)",main)
        self.assertIn("SovngardeTypeface.applyTree(root)",main)
        self.assertIn("SovngardeTypeface.applyTree(window.getDecorView())",dialog)
        self.assertIn("SovngardeTypeface.face(getContext(),false)",gallery)
        self.assertIn("SovngardeTypeface.face(context,true)",dialog)

    def test_all_tab_and_horizontal_scroll_positions_are_preserved(self):
        main=code("MainActivity.java")
        for value in ("PageScroll","rememberViewport()","resumeViewport(oldScroll,scroll,thisPage)",
                      "HorizontalScrollView","findHorizontal(target,horizontal)" ):
            if value=="findHorizontal(target,horizontal)":
                self.assertIn("findHorizontal(target,found)",main)
            else:self.assertIn(value,main)
        self.assertNotIn("scroll.scrollTo(0,0)",main)
        self.assertIn('scrollCache.put(renderedPage,new PageScroll',main)
        self.assertIn('h.scrollTo(Math.min(Math.max(0,previous.horizontal.get(i)),limit),0)',main)

    def test_dialogue_speaker_line_and_narration_italic_without_rule(self):
        renderer=code("SceneTypography.java")
        ai=code("AiStoryClient.java")
        self.assertIn("new StyleSpan(Typeface.ITALIC)",renderer)
        self.assertIn("new StyleSpan(Typeface.BOLD)",renderer)
        self.assertNotIn("new QuoteSpan(",renderer)
        self.assertIn('line.substring(1).trim()',renderer)
        self.assertIn("**— Его реплика**",ai)

if __name__=="__main__":
    unittest.main()
