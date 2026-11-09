"""Regression checks for location gallery chrome and always-visible AI status bars."""
import pathlib
import re
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
UI=(SRC/"MainActivity.java").read_text(encoding="utf-8")
GALLERY=(SRC/"LocationGalleryView.java").read_text(encoding="utf-8")


class GalleryAndAiStats(unittest.TestCase):
    def test_gallery_labels_are_places_not_counters(self):
        self.assertIn("private static final String[][] PHOTO_LABELS",GALLERY)
        self.assertIn('"Рынок"',GALLERY)
        self.assertIn('"Главные ворота"',GALLERY)
        self.assertIn("page.setText(captionFor(location,index))",GALLERY)
        self.assertNotIn('page.setText((index+1)+" / "+imageCount(location))',GALLERY)
        self.assertNotIn("Главный вид · ",GALLERY)
        self.assertNotIn("Проведи вбок или нажми на фото",GALLERY)
        self.assertEqual(GALLERY.count('{"Подход к Ривервуду"'),1)

    def test_swipe_keeps_photo_while_background_prefetches(self):
        self.assertIn("private void prefetch()",GALLERY)
        self.assertIn("PREFETCHING",GALLERY)
        self.assertIn("prefetch();",GALLERY)
        self.assertIn("Bitmap preview=CACHE.get(cacheKey(slug,number,false));",GALLERY)
        self.assertNotIn("loading.setVisibility(",GALLERY)
        self.assertNotIn("leftArrow",GALLERY)
        self.assertNotIn("rightArrow",GALLERY)
        self.assertNotIn("private TextView arrow(",GALLERY)

    def test_fullscreen_has_no_frame_or_gallery_window_actions(self):
        full=GALLERY.split("private void openFullscreen()",1)[1]
        self.assertIn("Dialog viewer=new Dialog(getContext())",full)
        self.assertIn("backdrop.setBackgroundColor(0xf206090b)",full)
        self.assertIn('close.setContentDescription("Закрыть полноэкранное фото")',full)
        self.assertIn("window.setBackgroundDrawableResource(android.R.color.transparent)",full)
        self.assertIn("params.dimAmount=.82f",full)
        self.assertIn("render[0].run()",full)
        self.assertNotIn("RuneDialogBuilder",full)
        self.assertNotIn(".setTitle(",full)
        self.assertNotIn(".setNegativeButton(",full)
        self.assertNotIn("RuneFrameDrawable",full)

    def test_ai_story_shows_health_and_all_tabs_autohide_header(self):
        self.assertIn("if(game.started)renderBars();",UI)
        self.assertNotIn('game.started && !(tab==0&&prefs.getBoolean("ai_enabled",false))',UI)
        self.assertIn("handleVerticalScroll(y,oldY)",UI)
        self.assertIn("chromeScrollTravel>dp(19)",UI)
        self.assertIn("chromeScrollTravel<-dp(11)",UI)
        self.assertIn("topChrome.addView(stats,lp(-1,-2))",UI)
        self.assertIn("topChrome.setVisibility(View.GONE)",UI)
        self.assertIn("topChrome.setVisibility(View.VISIBLE)",UI)
        for name in ('"ЗДОРОВЬЕ"','"ЗАПАС СИЛ"','"МАГИЯ"'):
            self.assertIn(name,UI)

if __name__=="__main__":
    unittest.main()
