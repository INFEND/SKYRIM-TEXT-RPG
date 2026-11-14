import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class FactionsTalents(unittest.TestCase):
    def test_saved_progress_and_guards(self):
        f=(ROOT/"FactionCampaign.java").read_text();g=(ROOT/"GameEngine.java").read_text();p=(ROOT/"RpgProgress.java").read_text()
        self.assertIn('stage[f]>TASK_COUNT',f)
        self.assertIn('TASK_COUNT=6',f)
        self.assertIn('attemptDay[f]==g.day',f)
        self.assertIn('f==4&&stage[5]>0',f)
        self.assertIn('g.factions=FactionCampaign.fromJson',g)
        self.assertIn('TalentBranches.fromJson',p)
        self.assertIn('branches.chanceBonus(skillIndex)',p)
