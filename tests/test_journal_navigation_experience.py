from pathlib import Path
import unittest
SRC=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(SRC/"MainActivity.java").read_text()
ENGINE=(SRC/"GameEngine.java").read_text()
class JournalNavigationExperience(unittest.TestCase):
    def test_codex_is_journal_tab(self):
        self.assertIn('final int[] screens={4,3,6};',UI)
        self.assertIn('{"ЗАДАНИЯ","СТАТИСТИКА","КОДЕКС"}',UI)
        self.assertNotIn('"СИСТЕМА"',UI)
        nav=UI.split('private void renderNav(){',1)[1]
        self.assertIn('int[] screens={0,1,2,3,4,5};',nav)
        self.assertIn('nav.setPadding(dp(10),dp(7),dp(10),dp(8))',nav)
        self.assertIn('new LinearLayout.LayoutParams(0,-1,1)',nav)
        self.assertIn('lp(-1,31)',nav)
    def test_weather_particles_removed_not_static_climate(self):
        gallery=UI.split('private View sceneGallery(',1)[1].split('private void stateEffects()',1)[0]
        self.assertNotIn('VisualEffectsView',gallery)
        self.assertNotIn('WorldWeather',gallery)
        state=UI.split('private void stateEffects()',1)[1].split('private static String npcStatusLabel',1)[0]
        self.assertNotIn('WorldWeather',state)
        self.assertNotIn('Сильный холод',state)
        self.assertIn('renderLocationWeather(content)',UI)
    def test_single_level_threshold(self):
        self.assertIn('Math.max(1,Math.min(100,level))*100',ENGINE)
        self.assertIn('while(level<100 && xp>=experienceRequired()){xp-=experienceRequired();',ENGINE)
        self.assertIn('int next=game.experienceRequired();',UI)
        self.assertIn('earned+=GameEngine.experienceRequired(i)',UI)
        self.assertNotIn('level*80',UI)
        self.assertNotIn('level*80',ENGINE)
