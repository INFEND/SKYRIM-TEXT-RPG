import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class FactionStoriesTest(unittest.TestCase):
    def test_story_battle_and_save(self):
        s=(ROOT/"FactionStories.java").read_text();g=(ROOT/"GameEngine.java").read_text();m=(ROOT/"MainActivity.java").read_text()
        # Chapter completion may resolve story decisions before moving the index.
        # Verify the transition and resets within the actual completion branch,
        # rather than requiring index++ to be the very first statement.
        advance=s.split('boolean advance(GameEngine g,int f){',1)[1].split('void victory(int f)',1)[0]
        completion=advance.split('if(phase[f]==2){',1)[1].split('int chance=chance(g,f);',1)[0]
        self.assertRegex(completion, r'index\[f\]\+\+;\s*phase\[f\]=0;')
        self.assertIn('attemptDay[f]=-1',completion)
        self.assertIn('preparation[f]=0',completion)
        self.assertIn('approach[f]=0',completion)
        self.assertIn('g.progress.skillExperience',completion)
        self.assertIn('g.addXp(',completion)
        self.assertIn('if(completed(f))g.progress.setQuestOutcome',completion)
        # Decisions must be resolved before the chapter index changes.
        before_transition=completion.split('index[f]++',1)[0]
        self.assertIn('if(f==3&&index[f]==8&&cicero==0)decide(g,f,0)',before_transition)
        self.assertIn('if(f==0&&index[f]==2&&beast==0)beast=1',before_transition)
        self.assertIn('if(f==0&&index[f]==5&&hasDecision(f))decide(g,f,0)',before_transition)
        self.assertIn('beastDecision=true',s)
        for key in ('cicero','beast','beastDecision'):
            self.assertIn('.put("'+key+'",',s)
            self.assertRegex(s, r'opt(?:Int|Boolean)\("'+key+r'",')
        self.assertIn('g.beginFactionBattle(f,c.enemy',s)
        self.assertIn('factionStories.victory(factionBattle)',g)
        self.assertIn('FactionStories.fromJson',g)
        self.assertIn('started(5)||g.factions.stage(5)>0',s)
        self.assertIn('factionStoryDialog(f)',m)
        gate=g.split("public boolean nativeSceneRequired(){",1)[1].split("public AiGameTurn aiTurn;",1)[0]
        self.assertIn('"FACTION".equals(battleKind)',gate)
        self.assertIn("game.nativeSceneRequired()",m)
        self.assertNotIn('{"За гранью смерти","Каирн Душ"',s)
        self.assertNotIn('{"Прикосновение к небу","Забытая долина"',s)
