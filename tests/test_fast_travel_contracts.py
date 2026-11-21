"""Static regression contracts for optional instant map travel."""
from pathlib import Path
import unittest

BASE=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
ENGINE=(BASE/"GameEngine.java").read_text(encoding="utf-8")
UI=(BASE/"MainActivity.java").read_text(encoding="utf-8")

class FastTravelContracts(unittest.TestCase):
    def test_instant_travel_has_no_resource_charges(self):
        block=ENGINE.split("public boolean fastTravel(",1)[1].split("public boolean waitHours(",1)[0]
        self.assertIn('visitedOnly&&!visitedVisual.contains(name)',block)
        self.assertIn('visitedVisual.add(name)',block)
        self.assertIn('mode="WORLD"',block)
        self.assertNotIn("day+=",block)
        self.assertNotIn("gold-=",block)
        self.assertNotIn("stamina-=",block)
        self.assertNotIn("magicka-=",block)
    def test_settings_and_map_controls(self):
        self.assertIn('prefs.getBoolean("fast_travel_visited_only",false)',UI)
        self.assertIn('prefs.edit().putBoolean("fast_travel_visited_only",on)',UI)
        self.assertIn('visualSummaryLink(footer,"БЫСТРОЕ ПЕРЕМЕЩЕНИЕ"',UI)
        self.assertIn('setNeutralButton("Быстрое перемещение"',UI)
        self.assertIn('game.fastTravel(destination,visitedOnly)',UI)

if __name__=="__main__":
    unittest.main()
