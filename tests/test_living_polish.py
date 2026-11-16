import pathlib,unittest
SRC=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class LivingPolish(unittest.TestCase):
    def test_faction_rolls_are_authoritative(self):
        self.assertIn("g.rollCheck(probability)",(SRC/"FactionCampaign.java").read_text())
        self.assertIn("g.rollCheck(chance)",(SRC/"FactionStories.java").read_text())
    def test_dialogs_close_and_refresh(self):
        ui=(SRC/"MainActivity.java").read_text()
        self.assertIn("closeLivingDialog();",ui)
        self.assertIn("game.economy.invest(game,n)",ui)
        self.assertIn("game.economy.withdraw(game,n)",ui)
        self.assertIn("refresh.run();",ui)
    def test_ai_has_needs_context(self):
        self.assertIn("game.needs.penalty()", (SRC/"AiStoryClient.java").read_text())
        self.assertIn("place==null?g.progress.location:place.name",(SRC/"AiGameTurn.java").read_text())
