"""Post-Hrothgar story contracts, compatible with schema-1 saves."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
def source(name):
    return (SRC/name).read_text(encoding="utf-8")

class DragonContinuationContracts(unittest.TestCase):
    def test_two_distinct_chapters_and_ten_choices(self):
        c=source("DragonContinuation.java")
        self.assertIn('"Письмо под пеплом"',c)
        self.assertIn('"Совет за закрытыми дверями"',c)
        self.assertIn('"След пепельного гонца"',c)
        self.assertIn('"Виндхельм"',c)
        self.assertIn('for(int i=1;i<=8;i++)',c)
        self.assertIn('s.primary("DRAGON_NEXT:"+stage+":0"',c)
        self.assertIn('s.add("DRAGON_NEXT:"+stage+":9"',c)
        self.assertIn('if(g.progress.dragonNextUsed.contains(action))return;',c)
        self.assertIn('g.progress.dragonNextUsed.add(action)',c)
    def test_risk_and_location(self):
        c=source("DragonContinuation.java")
        self.assertIn('if(!PLACES[stage].equals(g.progress.location))',c)
        self.assertIn('if(risky&&g.stamina<7)',c)
        self.assertIn('new Random().nextInt(100)<probability',c)
        self.assertIn('int probability=chance(g,action)',c)
    def test_evidence_gate_and_skill_rewards(self):
        c=source("DragonContinuation.java")
        self.assertIn("evidenceCount(g,stage)<3",c)
        self.assertIn("SKILLS[stage][choice-1],12+stage*4",c)
        self.assertIn("SKILLS[stage][choice-1],8",c)
        self.assertIn("g.progress.completed.add(TITLES[stage]",c)
        self.assertIn("if(g.progress.dragonNextUsed.contains(action))return;",c)
    def test_quest_objectives_and_branching_conclusions(self):
        c=source("DragonContinuation.java")
        self.assertIn("public static String questObjective(GameEngine g)",c)
        self.assertIn("evidenceCount(g,stage)+\"/3\"",c)
        self.assertIn("private static String lead(GameEngine g)",c)
        self.assertIn("if(seal&&dates)",c)
        self.assertIn("if(courier&&scout)",c)
        self.assertIn("!g.progress.dragonNextFailed.contains",c)
        self.assertIn("Дальнейшая экспедиция пока не реализована.",c)
    def test_windhelm_actions_reachable_and_saveable(self):
        c=source("DragonContinuation.java")
        self.assertIn('action.matches("DRAGON_NEXT:[012]:[0-9]")',c)
        self.assertNotIn('action.matches("DRAGON_NEXT:[01]:[0-9]")',c)
        self.assertIn('g.progress.dragonNextStage++',c)
        self.assertIn('if(stage==2)g.progress.recent=',c)
        self.assertIn('g.progress.dragonNextUsed.add(action)',c)
        self.assertIn('g.progress.dragonNextFailed.add(action)',c)

    def test_windhelm_verdicts_require_proven_evidence(self):
        c=source("DragonContinuation.java")
        self.assertIn("private static String windhelmVerdict(GameEngine g)",c)
        self.assertIn("ledger&&seals&&caravan",c)
        self.assertIn("workers&&family",c)
        self.assertIn("chest&&seals",c)
        self.assertIn("!g.progress.dragonNextFailed.contains(key)",c)
        self.assertIn('lead(g)+" "+windhelmVerdict(g)',c)
        self.assertIn('g.progress.recent="Квест «Драконий след: пепельный гонец» завершён. "+lead(g)+" "+windhelmVerdict(g)',c)

    def test_integration_and_legacy(self):
        arc=source("DragonArc.java")
        game=source("GameEngine.java")
        p=source("RpgProgress.java")
        self.assertIn('DragonContinuation.scene(g)',arc)
        self.assertIn('DragonContinuation.status(g)',arc)
        self.assertIn('DragonContinuation.perform(this,action)',game)
        self.assertIn('DragonContinuation.chance(g,action)',p)
        for field in ('dragonNextStage','dragonNextRecent','dragonNextUsed'):
            self.assertIn('.put("'+field+'"',p)
        self.assertIn('obj.optInt("dragonNextStage",0)',p)
        self.assertIn('Math.min(3,obj.optInt("dragonNextStage",0))',p)
        self.assertIn('DRAGON_NEXT:[012]:[1-8]',p)
        self.assertIn('DRAGON_NEXT:[012]:[357]',p)
        self.assertIn('j.put("schema",1)',game)
if __name__=="__main__": unittest.main()
