"""Regression checks for the 1.0.33 jitter-free scrolling fix."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
UI=(SRC/"MainActivity.java").read_text(encoding="utf-8")
GALLERY=(SRC/"LocationGalleryView.java").read_text(encoding="utf-8")


class OverlayScrollContracts(unittest.TestCase):
    def test_header_is_first_child_of_scroll_document(self):
        setup=UI.split("private void render(){",1)[1].split("private void renderHeader(){",1)[0]
        self.assertIn("root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1))",setup)
        self.assertIn("scroll.addView(page,new ScrollView.LayoutParams(-1,-2))",setup)
        self.assertIn("topChrome=column();page.addView(topChrome,lp(-1,-2))",setup)
        self.assertIn("page.addView(content,lp(-1,-2))",setup)
        self.assertLess(setup.index("page.addView(topChrome"),setup.index("page.addView(content"))
        self.assertNotIn("root.addView(topChrome",setup)

    def test_no_direction_based_overlay(self):
        for token in ("handleVerticalScroll", "chromeScrollSpacer", "chromeScrollTravel",
                      "topChrome.setTranslationY", "scroll.setClipBounds", "updateChromeScrollClip"):
            self.assertNotIn(token,UI)

    def test_level_notice_and_bars_scroll_with_header(self):
        level=UI.split("private void renderSkillLevelNotice()",1)[1].split("private void renderBars()",1)[0]
        self.assertIn("topChrome.addView(panel,area)",level)
        self.assertIn("if(game.started)renderBars();",UI)
        self.assertIn("resumeViewport(oldScroll,scroll,thisPage)",UI)

    def test_vertical_swipe_on_gallery_is_passed_to_page_scroller(self):
        self.assertIn("photo.setOnTouchListener(new OnTouchListener()",GALLERY)
        self.assertIn("MotionEvent.ACTION_MOVE&&!vertical",GALLERY)
        self.assertIn("dy>dp(12)&&dy>dx*1.15f",GALLERY)
        self.assertIn("requestDisallowInterceptTouchEvent(false)",GALLERY)
        self.assertIn("if(vertical)return false;",GALLERY)
        self.assertIn("detector.onTouchEvent(event)",GALLERY)
        self.assertIn("move(dx<0?1:-1)",GALLERY)

if __name__=="__main__":
    unittest.main()
