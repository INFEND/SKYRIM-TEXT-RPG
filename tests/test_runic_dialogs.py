"""Regressions for the shared Nordic dialog and rounded AMOLED frame presentation."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
RES=ROOT/"app/src/main/res"


class RunicDialogContract(unittest.TestCase):
    def test_all_game_alerts_use_shared_themed_builder(self):
        main=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertNotIn("new AlertDialog.Builder(this)",main)
        self.assertGreaterEqual(main.count("new RuneDialogBuilder(this,accent())"),12)
        self.assertIn("super(context, R.style.RunicDialogTheme)",(SRC/"RuneDialogBuilder.java").read_text(encoding="utf-8"))

    def test_rounded_frame_not_hexagonal_notch(self):
        frame=(SRC/"RuneFrameDrawable.java").read_text(encoding="utf-8")
        self.assertIn("c.drawRoundRect(bounds,rad,rad,p)",frame)
        self.assertIn("AmoledAppearance.radius",frame)
        self.assertNotIn("c.drawRect(r,p)",frame)
        self.assertNotIn("notch=",frame)
        self.assertIn("Paint.Style.STROKE",frame)
        self.assertIn("selected?ink",frame)

    def test_system_alert_theme_is_dark_and_legible(self):
        styles=(RES/"values/styles.xml").read_text(encoding="utf-8")
        drawable=(RES/"drawable/dialog_panel.xml").read_text(encoding="utf-8")
        self.assertIn('@style/RunicDialogTheme',styles)
        self.assertIn('@drawable/dialog_panel',styles)
        self.assertIn('#F0F1EF',styles)
        self.assertIn('android:shape="rectangle"',drawable)
        self.assertIn('android:width="2dp"',drawable)
        self.assertIn('android:radius="24dp"',drawable)

    def test_ota_dialog_has_matching_frame_but_unchanged_install_flow(self):
        source=(SRC/"OtaProgressDialog.java").read_text(encoding="utf-8")
        self.assertIn("AmoledAppearance.window(panel,accent)",source)
        self.assertIn("notesScroll.setBackground(new RuneFrameDrawable(",source)
        self.assertIn("primary.setOnClickListener",source)
        self.assertIn("dialog.setOnDismissListener",source)


if __name__=="__main__":
    unittest.main()
