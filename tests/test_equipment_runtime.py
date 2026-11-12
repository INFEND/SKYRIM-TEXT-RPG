"""Equipment save migration and combat-rank integration."""
import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class EquipmentRuntime(unittest.TestCase):
    def test_equipment_is_owned_and_serialized(self):
        p=(ROOT/"RpgProgress.java").read_text()
        for token in ('quantity(id)<1','item.slot().isEmpty()','equipmentInitialized','new JSONObject(equipped)','optJSONObject("equipped")','cleanEquipment()','weapon.twoHanded()'):
            self.assertIn(token,p)
        self.assertIn('if(quantity(id)==0)unequip(id)',p)
    def test_ranks_are_persistent_and_consumed(self):
        p=(ROOT/"RpgProgress.java").read_text()
        self.assertIn('combatPerks[skill]++;perkPoints--',p)
        self.assertIn('new JSONArray(combatPerks)',p)
        self.assertIn('optJSONArray("combatPerks")',p)
        self.assertIn('Math.min(5,perkRanks.optInt(i,0))',p)
        self.assertIn('game.progress.buyCombatPerk(index)',(ROOT/"MainActivity.java").read_text())
    def test_combat_uses_equipment_not_fixed_shop_stats(self):
        g=(ROOT/"GameEngine.java").read_text()
        self.assertIn('effectiveAttack()+random.nextInt',g)
        self.assertIn('effectiveAttack()*2',g)
        self.assertIn('1-progress.damageReduction()',g)
        self.assertIn('progress.shopEquip(target)',g)
        self.assertIn('Math.min(0.8,(equippedArmor()+25*armorPieces())*0.0012)',(ROOT/"RpgProgress.java").read_text())
if __name__=="__main__":unittest.main()
