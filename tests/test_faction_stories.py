import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class FactionStoriesTest(unittest.TestCase):
    def test_story_battle_and_save(self):
        s=(ROOT/"FactionStories.java").read_text();g=(ROOT/"GameEngine.java").read_text();m=(ROOT/"MainActivity.java").read_text()
        self.assertIn('if(phase[f]==2){index[f]++',s)
        self.assertIn('g.beginFactionBattle(f,c.enemy',s)
        self.assertIn('factionStories.victory(factionBattle)',g)
        self.assertIn('FactionStories.fromJson',g)
        self.assertIn('started(5)||g.factions.stage(5)>0',s)
        self.assertIn('factionStoryDialog(f)',m)
        self.assertIn('"FACTION".equals(game.battleKind)',m)
