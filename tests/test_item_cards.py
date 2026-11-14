"""Shared equipment rendering and confirmed inventory gains."""
from pathlib import Path
import unittest
UI=(Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text()
class ItemCards(unittest.TestCase):
    def test_stats_use_catalog_and_same_slot(self):
        for token in ('item.baseStat>0','item.currentStat(game.progress)','game.progress.equippedName(item.slot())','worn.currentStat(game.progress)','Double.isNaN(delta)','game.progress.equipped(item.id)'):
            self.assertIn(token,UI)
    def test_shared_surfaces(self):
        for token in ('itemSummary(item,game.progress.quantity(item.id),active,true,item.value)','itemSummary(item,0,false,false,item.value)','itemSummary(item,0,false,true,price)','itemSummary(item,gain.getValue(),false,true,item.value)','itemDetailMetrics(view,item)','itemEffect(view,item)'):
            self.assertIn(token,UI)
    def test_rewards_are_committed_deltas_not_prose(self):
        for token in ('state.progress.inventory.keys()','after.get(id)-before.getOrDefault(id,0)','captureItemGains(before,next,!actionResolved)','captureItemGains(before,game,false)'):
            self.assertIn(token,UI)
    def test_discard_is_guarded(self):
        for token in ('canDiscardItem(item)&&game.progress.removeItem(item.id,1)','Квестовые предметы','Септимы','Выбросить предмет?'):
            self.assertIn(token,UI)
