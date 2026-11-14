import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class NpcCritical(unittest.TestCase):
    def test_profiles_and_confirmed_speakers(self):
        s=(ROOT/"NpcProfiles.java").read_text();ui=(ROOT/"MainActivity.java").read_text()
        self.assertGreaterEqual(s.count('        add("'),25)
        self.assertIn('NpcProfiles.speakers(raw,game.name)',ui)
        self.assertIn('profile.dragon',ui)
        self.assertIn('ResourceStore.has(this,path)',ui)
    def test_critical_is_real_damage(self):
        s=(ROOT/"GameEngine.java").read_text()
        self.assertIn('random.nextInt(100)<8',s)
        self.assertIn('damage*1.5',s)
        self.assertIn('lastHeavyHit=lastMiss=lastCritical=false',s)
