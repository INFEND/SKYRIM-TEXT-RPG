"""Regression checks for the 1.0.33 jitter-free scrolling fix."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
UI=(SRC/"MainActivity.java").read_text(encoding="utf-8")
GALLERY=(SRC/"LocationGalleryView.java").read_text(encoding="utf-8")


class OverlayScrollContracts(unittest.TestCase):
    def test_header_is_overlay_not_vertical_layout_sibling(self):
        setup=UI.split("private void render(){",1)[1].split("private void renderHeader(){",1)[0]
        self.assertIn("scrollStage=new FrameLayout(this)",setup)
        self.assertIn("root.addView(scrollStage,new LinearLayout.LayoutParams(-1,0,1))",setup)
        self.assertIn("scrollStage.addView(scroll,new FrameLayout.LayoutParams(-1,-1))",setup)
        self.assertIn("scrollStage.addView(topChrome,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP))",setup)
        self.assertNotIn("root.addView(topChrome",setup)
        self.assertNotIn("root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1))",setup)

    def test_spacer_measured_before_display_and_never_removed_on_scroll(self):
        setup=UI.split("private void render(){",1)[1].split("private void renderHeader(){",1)[0]
        self.assertIn("chromeScrollSpacer=new View(this)",setup)
        self.assertIn("content.addView(chromeScrollSpacer,new LinearLayout.LayoutParams(-1,0))",setup)
        self.assertIn("chromeForRender.getMeasuredHeight()",setup)
        self.assertIn("spacerForRender.requestLayout()",setup)
        self.assertIn("return false;",setup)
        chrome=UI.split("private void setChromeVisible(boolean visible)",1)[1].split("private void renderHeader()",1)[0]
        self.assertIn("topChrome.setVisibility(View.GONE)",chrome)
        self.assertNotIn("chromeScrollSpacer",chrome)
        self.assertNotIn("scroll.scrollTo(",chrome)
        self.assertNotIn("scroll.requestLayout(",chrome)

    def test_level_notice_stays_with_collapsible_hud(self):
        level=UI.split("private void renderSkillLevelNotice()",1)[1].split("private void renderBars()",1)[0]
        self.assertIn("topChrome.addView(panel,area)",level)
        self.assertNotIn("root.addView(panel,area)",level)
        self.assertIn("if(game.started)renderBars();",UI)
        self.assertIn("setChromeVisible(true)",UI)
        self.assertIn("setChromeVisible(false)",UI)
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
