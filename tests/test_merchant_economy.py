import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class MerchantEconomy(unittest.TestCase):
    def test_transaction_and_save_guards(self):
        g=(ROOT/"GameEngine.java").read_text();m=(ROOT/"MerchantLedger.java").read_text()
        self.assertIn('merchantGold()<price||!progress.removeItem(id,1)',g)
        self.assertIn('!merchants.purchase(region,day,item,price)',g)
        self.assertIn('MerchantLedger.fromJson(j.optJSONObject("merchants"))',g)
        self.assertIn('RESTOCK_DAYS=2',m)
        self.assertIn('s.stock[i]--',m)
        self.assertIn('s.gold-=price',m)
