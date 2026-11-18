import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class AccordionContract(unittest.TestCase):
    def test_all_legacy_accordion_transitions_removed(self):
        main = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertNotIn("beginDelayedTransition", main)
        self.assertNotIn("new android.transition.AutoTransition", main)
        self.assertIn("entries.setExpanded(open,animations)", main)
        self.assertIn("entries.setExpanded(next,animations)", main)
        self.assertEqual(main.count("body.setExpanded(expanded,animations)"), 2)
        self.assertIn("details.setExpanded(valid,animations", main)

    def test_motion_has_no_fade_and_handles_reversal_and_detach(self):
        motion = (JAVA / "AccordionLayout.java").read_text(encoding="utf-8")
        self.assertIn("motion != null ? targetOpen", motion)
        self.assertIn("old.removeAllListeners()", motion)
        self.assertIn("old.removeAllUpdateListeners()", motion)
        self.assertIn("animatedHeight >= 0 ? animatedHeight", motion)
        self.assertIn("canvas.clipRect(0, 0, getWidth(), getHeight())", motion)
        self.assertIn("onDetachedFromWindow()", motion)
        self.assertIn("ValueAnimator.areAnimatorsEnabled()", motion)
        self.assertNotIn("setAlpha(", motion)

    def test_gallery_respects_same_animation_preference(self):
        gallery = (JAVA / "LocationGalleryView.java").read_text(encoding="utf-8")
        self.assertIn('getBoolean("animations",true)', gallery)
        self.assertIn("galleryBody.setExpanded(!collapsed,animate)", gallery)
