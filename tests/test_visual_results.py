import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class VisualResults(unittest.TestCase):
    def test_results_and_combined_status(self):
        s=(ROOT/"MainActivity.java").read_text();g=(ROOT/"GameEngine.java").read_text()
        self.assertIn('dialog.setContentView(stage);dialog.show()',s)
        self.assertIn('battleSummaryPending=false;boolean defeated',s)
        self.assertIn('lastBlocked=Math.max(0',g)
        self.assertIn('lastHeavyHit="HEAVY".equals(action)&&lastDealt>0',g)
        self.assertIn('new VisualEffectsView.EdgeDrawable(colors)',s)
        self.assertIn('Уже добавлено в инвентарь',s)
        self.assertIn('RadialGradient', (ROOT/"SkyrimTerrainMapView.java").read_text())
