import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class FinalMechanics(unittest.TestCase):
    def test_actual_weapon_and_perks(self):
        p=(ROOT/"RpgProgress.java").read_text();a=(ROOT/"AdvancedPerks.java").read_text();g=(ROOT/"GameEngine.java").read_text()
        self.assertIn('advanced.checkBonus(skillIndex)',p)
        self.assertIn('if(g.lastDealt>0)skillExperience(g,equippedWeaponSkill(),8)',p)
        self.assertIn('PHYSICIAN=11',a)
        self.assertIn('SUMMONER=18',a)
        self.assertIn('int restored=Math.min(healingAmount(),maxHp-hp)',g)
        self.assertIn('!progress.removeItem("Зелье запаса сил",1)',g)
    def test_faction_trust(self):
        f=(ROOT/"FactionCampaign.java").read_text()
        self.assertIn('SCENES[f][s-1]',f)
        self.assertIn('trust[f]=Math.max(-100,trust[f]-3)',f)
        self.assertIn('trust[f]=Math.min(100,trust[f]+12)',f)
        self.assertIn('put("trust",new JSONArray(trust))',f)
