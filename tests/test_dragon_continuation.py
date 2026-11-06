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
        self.assertIn('j.put("schema",1)',game)
if __name__=="__main__": unittest.main()
