"""Regression contracts for both bounty redemption paths and schema-1 restoration."""
from pathlib import Path
import unittest

SOURCE = (Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")

class BountyBoardAccounting(unittest.TestCase):
    def test_both_paths_share_claim(self):
        self.assertIn('case "SIDE_CLAIM": claimBoard();break;', SOURCE)
        self.assertIn('else if(sideKills>=3){claimBoard();}', SOURCE)
        claim = SOURCE.split("private void claimBoard() {", 1)[1].split("private void board()", 1)[0]
        self.assertIn("if(sideQuest!=1 || sideKills<3)return;", claim)
        self.assertIn("sideQuest=0;sideKills=0;boardCompleted=nonnegativeAdd(boardCompleted,1);", claim)
        self.assertEqual(claim.count("addXp(65)"), 1)
        self.assertEqual(claim.count("nonnegativeAdd(gold,120)"), 1)

    def test_only_wild_victories_count(self):
        self.assertIn('if(!nonlethal&&"WILD".equals(battleKind)&&sideQuest==1&&sideKills<3)sideKills++;', SOURCE)

    def test_restore_bounds(self):
        self.assertIn('g.sideQuest=Math.max(0,Math.min(2,j.optInt("sideQuest",0)));', SOURCE)
        self.assertIn('g.sideKills=g.sideQuest==1?Math.max(0,Math.min(3,j.optInt("sideKills",0))):0;', SOURCE)

if __name__ == "__main__":
    unittest.main()
