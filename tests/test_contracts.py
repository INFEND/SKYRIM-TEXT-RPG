"""Hourly static checks. Nightly Android compilation is a separate workflow job."""
import json
import pathlib
import re
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/dev/infend/skyrimtext"


def text(name):
    return (SRC / name).read_text(encoding="utf-8")


class GameContracts(unittest.TestCase):
    def test_ota_cert_and_legacy_metadata(self):
        gradle = (ROOT/"app/build.gradle").read_text(encoding="utf-8")
        history = json.loads((ROOT/"release_notes/history.json").read_text(encoding="utf-8"))
        code = int(re.search(r"\bversionCode\s+(\d+)",gradle).group(1))
        version = re.search(r"\bversionName\s+'([^']+)'",gradle).group(1)
        self.assertIn("applicationId 'dev.infend.skyrimtext'",gradle)
        self.assertEqual((history[-1]["versionCode"], history[-1]["versionName"]),(code,version))
        self.assertTrue((ROOT/f"release_notes/v{version}.txt").exists())
        self.assertEqual(len({h["versionCode"] for h in history}),len(history))
        update=text("UpdateManager.java")
        self.assertIn("BuildConfig.APPLICATION_ID.equals(packageName)",update)
        self.assertIn("archive.packageName",update)
        self.assertIn("getApkContentsSigners()",update)
        self.assertIn("MessageDigest.getInstance(\"SHA-256\")",update)
        generator=(ROOT/"tools/build_manifest.py").read_text(encoding="utf-8")
        self.assertIn('"packageName": "dev.xarkon.link"',generator)
        self.assertIn('"apkPackageName": "dev.infend.skyrimtext"',generator)

    def test_character_and_24_starts(self):
        src=text("RpgContent.java")
        starts=re.findall(r'^\s*"([^"]+)\|([^"]+)\|(\d+)\|([^"]+)"',src,re.M)
        self.assertGreaterEqual(len(starts),24)
        self.assertEqual(starts[0][0],"Хелген")
        self.assertEqual(len({s[0] for s in starts}),len(starts))
        self.assertIn('"Аргонианин"',src)
        self.assertIn('"Без класса"',src)
        builder=text("MainActivity.java")
        self.assertIn("initialPoints[0]+initialPoints[1]+initialPoints[2]",builder)
        self.assertIn("Распредели все 5 свободных очков",builder)
        self.assertIn("RpgContent.randomName()",builder)

    def test_honest_chances_and_no_combat_farming(self):
        engine=text("GameEngine.java")
        progress=text("RpgProgress.java")
        self.assertIn('int hitChance=progress.chance(this,"HEAVY");stamina-=10',engine)
        self.assertIn('random.nextInt(100)<hitChance',engine)
        self.assertIn('int successChance=progress.chance(this,action)',engine)
        self.assertIn('random.nextInt(100)<successChance',engine)
        self.assertIn('random.nextInt(100)<progress.chance(this,"FLEE")',engine)
        self.assertIn('if(!("WORLD".equals(g.mode)',progress)
        self.assertIn('attemptedToday.contains(action)',progress)
        self.assertIn('obj.optJSONArray("attemptedToday")',progress)
        self.assertIn("new Random().nextInt(100)<chance",progress)
        self.assertNotIn("random.nextInt(3)==0",engine)

    def test_journal_separates_quests_from_rolls_and_race_power_has_no_permanent_farm(self):
        ui=text("MainActivity.java")
        progress=text("RpgProgress.java")
        self.assertIn('"ЗАВЕРШЁННЫЕ ЗАДАНИЯ"',ui)
        self.assertIn('"ПРОВАЛЕННЫЕ ЗАДАНИЯ"',ui)
        self.assertIn('"ИСТОРИЯ УСПЕШНЫХ ПРОВЕРОК"',ui)
        self.assertIn('"ИСТОРИЯ НЕУДАЧНЫХ ПРОВЕРОК"',ui)
        self.assertNotIn("g.attack+=1;",progress)
        self.assertNotIn("g.reputation+=2;",progress)
        self.assertIn("if(activePowerDay==g.day)return false;",progress)

    def test_inventory_atlas_saves(self):
        progress=text("RpgProgress.java")
        atlas=text("WorldAtlas.java")
        game=text("GameEngine.java")
        self.assertIn("carryWeight()+item.weight*quantity>50",progress)
        self.assertIn('quantity("Септимы"',progress)
        self.assertIn("progress.location=target.name",game)
        self.assertGreaterEqual(atlas.count('put("'),75)
        self.assertNotIn("Маркарt",atlas)
        self.assertIn("p.name.equals(game.progress.location)",atlas)
        self.assertIn('put("progress",progress.toJson())',game)
        self.assertIn('RpgProgress.fromJson(j.optJSONObject("progress"))',game)
        self.assertIn("renderMap()",text("MainActivity.java"))
        self.assertIn("renderInventory()",text("MainActivity.java"))

    def test_route_planning_and_ferry_restrictions(self):
        route=text("TravelPlanner.java")
        engine=text("GameEngine.java")
        ui=text("MainActivity.java")
        self.assertIn('FERRY_FARE = 250',route)
        self.assertIn('{2,9}',route)
        self.assertIn('game.progress.carryWeight()>CARRY_LIMIT_KG',route)
        self.assertIn('game.gold<fare',route)
        self.assertIn('target.type.equals("Иной мир")',route)
        self.assertIn('TravelPlanner.plan(this,target)',engine)
        self.assertIn('choose("LOC:"+TravelPlanner.HUBS[target])',engine)
        self.assertIn('gold-=route.fare;',engine)
        self.assertIn('day+=route.days;',engine)
        self.assertIn('TravelPlanner.plan(game,place)',ui)
        self.assertNotIn('region=to;progress.location=REGIONS[to]; day++',engine)

    def test_shop_checks_weight_before_payment_and_migrates_armor(self):
        engine=text("GameEngine.java")
        progress=text("RpgProgress.java")
        catalog=text("ItemCatalog.java")
        shop=engine[engine.index('private void buy('):engine.index('private void explore()')]
        self.assertIn('double projected=progress.carryWeight()+incoming.weight',shop)
        self.assertIn('if(projected>50.00001)',shop)
        self.assertLess(shop.index('if(projected>50.00001)'),shop.index('gold-=price;'))
        self.assertIn('progress.carryWeight()+0.5<=50.00001',engine)
        self.assertIn('put(feminine[i]+" кираса"',catalog)
        self.assertIn('put(plural[i]+" перчатки"',catalog)
        self.assertIn('quantity("Стальная кираса",1)',progress)
        self.assertIn('p.inventory.remove("Стальной кираса")',progress)

    def test_combat_cannot_be_bypassed_by_world_actions(self):
        engine=text("GameEngine.java")
        choose=engine[engine.index("public void choose(String action)"):engine.index("private void buy(")]
        guard=choose.index('if ("COMBAT".equals(mode)')
        self.assertLess(guard,choose.index('if (action.startsWith("ACT:"))'))
        self.assertLess(guard,choose.index('if(action.startsWith("LOC:"))'))
        self.assertLess(guard,choose.index('switch(action)'))
        for action in ("ATTACK","HEAVY","MAGIC","DEFEND","POTION","FLEE"):
            self.assertIn('"' + action + '".equals(action)',choose[:choose.index('if (action.startsWith("ACT:"))')])
        self.assertIn('if(!mode.equals("COMBAT"))return;',engine)

    def test_embedded_user_materials(self):
        bundle=ROOT/"app/src/main/assets/lore_bundle.zip"
        self.assertTrue(bundle.is_file())
        with zipfile.ZipFile(bundle) as zipped:
            names=zipped.namelist()
            self.assertGreaterEqual(sum(p.startswith("lore/") and p.endswith(".txt") for p in names),20)
            self.assertGreaterEqual(sum(p.startswith("images/") and p.endswith(".webp") for p in names),3)
            foods=json.loads(zipped.read("catalog/user_foods.json").decode("utf-8"))
            self.assertGreaterEqual(len(foods),190)
            self.assertTrue(all(float(x.get("weight",0))>=0 for x in foods))


if __name__=="__main__":
    unittest.main()
