"""Location dialog layout and navigation safety contracts."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SOURCE=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")
DIALOG=SOURCE.split("private void locationDialog(WorldAtlas.Place place){",1)[1].split("// Shared item presentation:",1)[0]

class LocationDialogTest(unittest.TestCase):
    def test_safe_bounds_and_fixed_footer(self):
        self.assertIn("getWindowVisibleDisplayFrame(safe)",DIALOG)
        self.assertIn("WindowInsets.Type.systemBars()",DIALOG)
        self.assertIn("WindowInsets.Type.displayCutout()",DIALOG)
        self.assertIn("height-dp(32)",DIALOG)
        self.assertIn("window.setGravity(Gravity.CENTER)",DIALOG)
        self.assertIn("panel.addView(scroller,new LinearLayout.LayoutParams(-1,0,1))",DIALOG)
        self.assertIn("panel.addView(footer,lp(-1,-2))",DIALOG)
    def test_route_order_and_matching_actions(self):
        self.assertNotIn("place.type",DIALOG)
        self.assertNotIn("place.region",DIALOG)
        self.assertLess(DIALOG.index('"Отправление"'),DIALOG.index('"МАРШРУТ НА КАРТЕ"'))
        self.assertLess(DIALOG.index('"Прибытие"'),DIALOG.index('"МАРШРУТ НА КАРТЕ"'))
        self.assertLess(DIALOG.index('"МАРШРУТ НА КАРТЕ"'),DIALOG.index('"НАЧАТЬ ПЕРЕХОД"'))
        self.assertLess(DIALOG.index('"НАЧАТЬ ПЕРЕХОД"'),DIALOG.index('"ЗАКРЫТЬ"'))
        self.assertEqual(DIALOG.count("visualSummaryLink(footer,"),4)
        self.assertIn('visualSummaryLink(footer,"БЫСТРОЕ ПЕРЕМЕЩЕНИЕ"',DIALOG)
        self.assertIn("route.allowed&&route.days>0",DIALOG)
        self.assertIn('game.choose("LOC:"+place.name)',DIALOG)
        self.assertIn("route.summary()",DIALOG)
