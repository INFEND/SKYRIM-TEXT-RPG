import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class RemainingMechanics(unittest.TestCase):
    def test_extended_trade_travel_talents(self):
        g=(ROOT/"GameEngine.java").read_text();m=(ROOT/"MerchantLedger.java").read_text();t=(ROOT/"TalentBranches.java").read_text()
        self.assertIn('!merchantExtras().contains(id)',g)
        self.assertIn('!progress.addItem(id,1)',g)
        self.assertIn('RoadEvents.resolve(this,route)',g)
        self.assertIn('capacity(region,cycle,i)',m)
        self.assertIn('for(int value:rank)values.put(value)',t)
        self.assertIn('r<3',t)
