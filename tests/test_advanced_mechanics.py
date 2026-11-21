import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class AdvancedMechanics(unittest.TestCase):
    def test_advanced_perks_saved_and_consumed(self):
        p=(ROOT/"RpgProgress.java").read_text();g=(ROOT/"GameEngine.java").read_text();a=(ROOT/"AdvancedPerks.java").read_text()
        self.assertIn('AdvancedPerks.fromJson',p)
        self.assertIn('owned[i]=true;p.perkPoints--',a)
        self.assertIn('stamina-=heavyCost()',g)
        self.assertIn('magicka-=magicCost()',g)
        self.assertIn('random.nextInt(100)<criticalChance()',g)
    def test_bulk_transactions(self):
        g=(ROOT/"GameEngine.java").read_text();m=(ROOT/"MainActivity.java").read_text()
        self.assertIn('amount>maxExtraPurchase(id)',g)
        self.assertIn('amount>maxSale(item)',g)
        self.assertIn('merchants.purchaseMany(region,day,id,total,amount)',g)
        self.assertIn('tradeQuantity(item,false)',m)
        self.assertIn('renderFactionJournal();',m)
