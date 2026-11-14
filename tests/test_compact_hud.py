"""Compact HUD and location climate must reflect actual saved game state."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(ROOT/"MainActivity.java").read_text()
WEATHER=(ROOT/"WorldWeather.java").read_text()
class CompactHud(unittest.TestCase):
    def test_stats_and_experience(self):
        for token in ('game.hp,game.maxHp','game.stamina,game.maxStamina','game.magicka,game.maxMagicka',
                      'game.level*80-game.xp','game.progress.carryWeight()','String.valueOf(game.gold)',
                      'game.level>=100','hudBar(stats,"xp"','bar.setProgress(target,true)'):
            self.assertIn(token,UI)
    def test_gallery_weather_is_current_location_and_offline(self):
        self.assertEqual(UI.count('renderLocationWeather(content);'),2)
        self.assertIn('renderLocationWeather(overview);',UI)
        self.assertIn('if(place.name.equalsIgnoreCase(game.progress.location))renderLocationWeather(wrapper);',UI)
        self.assertIn('WorldWeather.at(game.region,game.progress.location,game.day,game.hour)',UI)
        for token in ('Math.floorMod','h/6','BASE[r]','timeGlyph()','formattedTemperature()'):
            self.assertIn(token,WEATHER)
        self.assertNotIn('Random',WEATHER)
        self.assertNotIn('http',WEATHER)
