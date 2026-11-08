"""Dragon quest regression contracts. They do not replace Android on-device tests."""
import pathlib
import re
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"

def java(name):
    return (SRC/name).read_text(encoding="utf-8")

class DragonArcContracts(unittest.TestCase):
    def test_six_distinct_chapters_and_ten_choices_each(self):
        content=java("DragonArc.java")
        targets=content.split("DESTINATIONS={",1)[1].split("};",1)[0]
        places=re.findall(r'"([^"]+)"',targets)
        self.assertEqual(len(places),6)
        self.assertEqual(len(set(places)),6)
        self.assertEqual(places[0],"Хелген")
        self.assertEqual(places[-1],"Высокий Хротгар")
        self.assertIn("for(int i=1;i<=8;i++)",content)
        self.assertIn('s.primary("DRAGON:"+stage+":0"',content)
        self.assertIn('s.add("DRAGON:"+stage+":9"',content)
        self.assertIn('private static final String[][] OPTIONS={',content)
        block=content.split('private static final String[][] OPTIONS={',1)[1].split('private static final int[] SKILLS',1)[0]
        self.assertEqual(len(re.findall(r'^\s*"[^"\n]+\|[^"\n]+"',block,re.M)),48)
        self.assertEqual(len(re.findall(r'^\s*\{',block,re.M)),6)
        self.assertIn('NARRATIVE[stage]',content)

    def test_risky_actions_share_ui_and_roll_chance(self):
        arc=java("DragonArc.java")
        progress=java("RpgProgress.java")
        ui=java("MainActivity.java")
        self.assertIn('if(index!=3&&index!=5&&index!=7)return -1;',arc)
        self.assertIn('int check=chance(g,action)',arc)
        self.assertIn('new Random().nextInt(100)<check',arc)
        self.assertIn('if(action.startsWith("DRAGON:"))return DragonArc.chance(g,action);',progress)
        self.assertIn('game.progress.chance(game,c.action)',ui)
        self.assertIn('"Шанс успеха: "+chance+"%"',ui)
        self.assertIn('SceneChoiceMeta.chanceColor(chance)',ui)
        self.assertIn('g.progress.dragonUsed.add(action);',arc)
        self.assertIn('if(g.progress.dragonUsed.contains(action))',arc)

    def test_battle_quest_stage_and_location_gates(self):
        arc=java("DragonArc.java")
        game=java("GameEngine.java")
        self.assertIn('if(stage!=g.progress.dragonStage)return false;',arc)
        self.assertIn('if(!DESTINATIONS[stage].equals(g.progress.location))',arc)
        self.assertIn('if(stage==3)return true;',arc)
        self.assertIn('startBattle("Мирмулнир"',game)
        self.assertIn('if("DRAGON".equals(battleKind)&&progress.dragonStage==3)',game)
        self.assertIn('progress.dragonStage=4;',game)
        self.assertIn('if ("COMBAT".equals(mode)',game)
        self.assertIn('case "DRAGON": return DragonArc.scene(this);',game)
        self.assertIn('s.primary("DRAGON_OPEN"',game)
        self.assertIn('DragonArc.status(this)',game)

    def test_schema_one_legacy_saves_stay_compatible(self):
        progress=java("RpgProgress.java")
        game=java("GameEngine.java")
        for token in (
            '.put("dragonStage",dragonStage)', # In actual code uses chained put with preceding dot.
            '.put("dragonInsight",dragonInsight)',
            '.put("dragonRecent",dragonRecent)',
            '.put("dragonUsed",new JSONArray(dragonUsed))',
            'obj.optInt("dragonStage",0)',
            'obj.optJSONArray("dragonUsed")',
        ):
            self.assertIn(token,progress)
        self.assertIn('j.put("schema",1)',game)
        self.assertIn('RpgProgress.fromJson(j.optJSONObject("progress"))',game)

if __name__=="__main__":
    unittest.main()
