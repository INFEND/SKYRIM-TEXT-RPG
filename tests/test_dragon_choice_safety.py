"""Regression contracts for location-gated, exhaustion-safe dragon decisions."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ARC = (ROOT / "app/src/main/java/dev/infend/skyrimtext/DragonArc.java").read_text(encoding="utf-8")

class DragonChoiceSafety(unittest.TestCase):
    def test_location_checked_before_spending_unique_choice(self):
        location = 'if(!DESTINATIONS[stage].equals(g.progress.location)){\n            g.progress.dragonRecent='
        self.assertIn(location, ARC)
        self.assertLess(ARC.index(location), ARC.index('g.progress.dragonUsed.add(action);'))

    def test_exhaustion_checked_before_spending_unique_choice(self):
        self.assertIn('if((index==3||index==5||index==7)&&g.stamina<7)', ARC)
        self.assertLess(ARC.index('if((index==3||index==5||index==7)&&g.stamina<7)'), ARC.index('g.progress.dragonUsed.add(action);'))

    def test_probability_hidden_when_attempt_is_unavailable(self):
        self.assertIn('if(!DESTINATIONS[stage].equals(g.progress.location)||g.stamina<7)return -1;', ARC)

    def test_save_schema_remains_one(self):
        engine = (ROOT / "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")
        self.assertIn('j.put("schema",1)', engine)
        self.assertIn('if(j.optInt("schema")!=1)return g;', engine)

if __name__ == "__main__":
    unittest.main()
