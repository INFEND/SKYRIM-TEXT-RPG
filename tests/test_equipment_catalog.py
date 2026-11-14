"""Catalog categories, stable save identifiers, and explicit equipment stats."""
import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
CAT=(ROOT/"ItemCatalog.java").read_text()
UI=(ROOT/"MainActivity.java").read_text()
class EquipmentCatalog(unittest.TestCase):
    def test_categories_and_gold(self):
        self.assertIn('put("Септимы","","◈",0,1',CAT)
        self.assertIn('if(!item.category.isEmpty())',CAT)
        self.assertIn('put("Клинок «Кай»","Оружие"',CAT)
        self.assertIn('put("Арбалет «Рик»","Оружие"',CAT)
        self.assertIn('put("Эликсир Жизни","Зелья"',CAT)
        for name in ("Валюта","Пользовательское оружие","Пользовательские предметы"):
            self.assertNotIn('"'+name+'"',CAT)
        self.assertIn('quantity("Септимы",Math.max(0,g.gold))',(ROOT/"RpgProgress.java").read_text())
    def test_base_stats_and_ui(self):
        self.assertIn('{7,9,25},{8,11,30},{4,2,10}',CAT)
        self.assertIn('{31,35,275}',CAT)
        self.assertIn('(baseStat+progress.temperBonus(this))*(1+progress.skillLevel(statSkill)',CAT)
        self.assertIn('progress.combatPerkRank(statSkill)',CAT)
        self.assertIn('baseStat<0',CAT)
        self.assertIn('itemSummary(focused,game.progress.quantity(focused.id),true,false,focused.value)',UI)
        self.assertIn('item.currentStat(game.progress)',UI)
        self.assertIn('itemDetailMetrics(view,item)',UI)
        self.assertIn('itemMetric("ВЕС","bag",itemNumber(item.weight)',UI)
        self.assertIn('itemMetric("ЦЕНА","septims",String.valueOf(item.value)',UI)
if __name__=="__main__":unittest.main()
