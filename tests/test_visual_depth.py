import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class VisualDepth(unittest.TestCase):
    def test_saved_talents_and_trade(self):
        p=(ROOT/"RpgProgress.java").read_text()
        g=(ROOT/"GameEngine.java").read_text()
        self.assertIn('put("studyPerks",new JSONArray(studyPerks))',p)
        self.assertIn('if(!canBuyTalent(skill))return false',p)
        self.assertIn('if(!canSell(item))return false',g)
        self.assertIn('!progress.equipped(item.id)',g)
        self.assertIn('!progress.removeItem(id,1)',g)
    def test_real_status_and_route(self):
        g=(ROOT/"GameEngine.java").read_text()
        ui=(ROOT/"MainActivity.java").read_text()
        for key in ("poisonTurns","frostTurns","holdReputation","battleTurns","battleDealt","battleTaken"):
            self.assertIn('put("'+key+'"',g)
        self.assertIn('lastTaken=Math.min(hp,incoming+poisoned)',g)
        self.assertIn('TravelPlanner.plan(g,place).stops',ui)
        self.assertIn('npcStatusByName(name)',ui)
        self.assertIn('game.progress.canBuyTalent(index)',ui)
