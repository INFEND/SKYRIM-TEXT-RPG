"""AI takeover contract: one director, ten editable choices, free action, saved effects."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
def code(name):return (JAVA/name).read_text(encoding="utf-8")

class AiGameDirectorContracts(unittest.TestCase):
    def test_ai_mode_hides_offline_story_before_reading_scene(self):
        ui=code("MainActivity.java")
        start=ui.index("private void renderStory(){")
        body=ui[start:start+300]
        self.assertIn('if(prefs.getBoolean("ai_enabled",false)){renderAiGame();return;}',body)
        self.assertLess(body.index("renderAiGame();return;"),body.index("GameEngine.Scene s=game.scene()"))
        # AI storyteller replaces offline prose, but must preserve live RPG stats.
        self.assertIn("if(game.started)renderBars();",ui)
        self.assertIn("handleVerticalScroll(y,oldY)",ui)

    def test_exactly_ten_ai_options_and_user_input_below(self):
        bridge=code("AiGameTurn.java")
        ui=code("MainActivity.java")
        self.assertIn("options==null||options.length()!=10",bridge) if False else None
        self.assertIn("array==null||array.length()!=10",bridge)
        self.assertIn("for(int i=0;i<game.aiTurn.choices.size();i++)",ui)
        self.assertIn("renderAiChoice(i+1,option)",ui)
        begin=ui.index("private void renderAiGame(){")
        sub=ui[begin:ui.index("private void renderAiChoice(",begin)]
        self.assertLess(sub.index("renderAiChoice(i+1,option)"),sub.index('СВОЙ ВАРИАНТ ОТВЕТА'))
        self.assertIn('own.setHint("Опиши, что говоришь или делаешь...")',sub)
        self.assertIn('requestAiTurn("ОЦЕНКА СВОБОДНОГО ДЕЙСТВИЯ:',sub)

    def test_model_picks_chances_but_engine_rolls_and_commits(self):
        bridge=code("AiGameTurn.java")
        ai=code("AiStoryClient.java")
        ui=code("MainActivity.java")
        self.assertIn("DICE.nextInt(100)+1",bridge)
        self.assertIn("chance<0||roll<=chance",bridge)
        self.assertIn("probability(c,\"chance\")",bridge)
        self.assertIn("AiGameTurn.success(option.chance,die)",ui)
        self.assertIn("AiGameTurn completed=AiGameTurn.parse(answer)",ui)
        self.assertIn("GameEngine next=GameEngine.fromJson(saved)",ui)
        self.assertIn("completed.apply(next,actionResolved)",ui)
        self.assertIn("next.aiTurn=completed",ui)
        self.assertIn("game=next",ui)
        self.assertIn('if(aiControlled)payload.put("instructions",chanceOnly?AI_CHANCE_RULES:AI_GAME_RULES)',ai)
        self.assertIn('payload.put("stream", true)',ai)

    def test_original_rpg_and_saved_ai_mode_coexist(self):
        engine=code("GameEngine.java")
        ui=code("MainActivity.java")
        for snippet in ('public AiGameTurn aiTurn;', 'j.optJSONObject("aiTurn")',
                        'aiTurn==null?JSONObject.NULL:aiTurn.save()', 'g.progress.sync(g)'):
            self.assertIn(snippet,engine)
        self.assertIn("game.choose(c.action)",ui)
        self.assertIn("renderAiStoryPanel()",ui)

    def test_ai_can_manage_stats_items_enemies_map_and_quests(self):
        bridge=code("AiGameTurn.java")
        for field in ("hpDelta","maxHpDelta","staminaDelta","maxStaminaDelta",
                      "magickaDelta","maxMagickaDelta","goldDelta","xpDelta",
                      "dayDelta","reputationDelta","attackDelta","armorDelta",
                      "potionsDelta","manaPotionsDelta","questStageDelta",
                      "dragonStageDelta"):
            self.assertIn(field,bridge)
        self.assertIn('e.optJSONArray("inventory")',bridge)
        self.assertIn('e.optJSONObject("enemy")',bridge)
        self.assertIn("WorldAtlas.find(loc)",bridge)
        self.assertIn("g.progress.skillExperience(g,",bridge)
        self.assertIn("g.aiJournal=quest+",bridge)

    def test_json_recovery_is_string_aware_and_never_splices_objects(self):
        bridge=code("AiGameTurn.java")
        self.assertIn("extractObject(response)",bridge)
        self.assertIn("boolean quoted=false,escaped=false",bridge)
        self.assertIn('candidate.has("scene")&&candidate.has("choices")',bridge)
        self.assertIn("response.substring(start,i+1)",bridge)
        self.assertNotIn("response.substring(start,end+1)",bridge)

    def test_invalid_late_quest_cannot_partially_apply_inventory(self):
        bridge=code("AiGameTurn.java")
        validation=bridge.index('String questStatus=e.optString("questStatus","ACTIVE")')
        commit=bridge.index('for(String name:inventoryChanges.keySet())')
        enemy=bridge.index('if(enemy!=null){',commit)
        self.assertLess(validation,commit)
        self.assertLess(commit,enemy)
        self.assertIn('inventoryChanges.putAll(changes)',bridge)
        self.assertNotIn('for(String name:changes.keySet())g.progress.quantity',bridge)

    def test_custom_action_is_two_phase_and_probability_is_locked(self):
        ui=code("MainActivity.java")
        bridge=code("AiGameTurn.java")
        client=code("AiStoryClient.java")
        first=ui.index('requestAiTurn("ОЦЕНКА СВОБОДНОГО ДЕЙСТВИЯ:')
        handler=ui.index('if(proposalOnly){',first)
        fixed=ui.index('AiGameTurn.parseChance(answer)',handler)
        roll=ui.index('AiGameTurn.roll()',fixed)
        lock=ui.index('storeAiPending(customAction,fixedChance,die)',roll)
        second=ui.index('requestAiTurn(lockedFreeInstruction(customAction,fixedChance,die)',lock)
        commit=ui.index('completed.apply(next,actionResolved)',second)
        self.assertLess(first,handler)
        self.assertLess(handler,fixed)
        self.assertLess(fixed,roll)
        self.assertLess(roll,lock)
        self.assertLess(lock,second)
        self.assertLess(second,commit)
        self.assertIn('if(proposalOnly)return;',ui)
        self.assertIn('completed.customChance!=expectedChance',ui)
        self.assertIn('if(!game.toJson().toString().equals(saved))',ui)
        self.assertIn('if(compound.equals(pendingAiAction()))',ui)
        self.assertIn('if(aiControlled)payload.put("instructions",chanceOnly?AI_CHANCE_RULES:AI_GAME_RULES)',client)
        self.assertIn('parseChance(String response)',bridge)
        self.assertIn('aiTurnBusy=false;aiTurnPreview="";',ui[handler:second])

    def test_ai_patch_rejects_malformed_enemy_and_collection_shapes(self):
        bridge=code("AiGameTurn.java")
        self.assertIn('requiredEnemyInt(enemy,"hp",0,50000)',bridge)
        self.assertIn('requiredEnemyInt(enemy,"maxHp",health,50000)',bridge)
        self.assertIn('requiredEnemyInt(enemy,"damage",0,1000)',bridge)
        self.assertIn('Double.isFinite(value)',bridge)
        self.assertIn('!(e.opt("inventory") instanceof JSONArray)',bridge)
        self.assertIn('!(e.opt("skills") instanceof JSONArray)',bridge)
        self.assertIn('!(e.opt("enemy") instanceof JSONObject)',bridge)
        self.assertIn('weight>50.0001 || weight < -0.0001',bridge)
        validate=bridge.index('int health=requiredEnemyInt(enemy,"hp",0,50000)')
        commit=bridge.index('for(String name:inventoryChanges.keySet())')
        self.assertLess(validate,commit)

    def test_story_stream_is_not_a_json_dump(self):
        client=code("AiStoryClient.java")
        ui=code("MainActivity.java")
        bridge=code("AiGameTurn.java")
        self.assertIn("AiGameTurn.previewScene(accumulated)",ui)
        self.assertIn('partial.indexOf("\\"scene\\"")',bridge)
        self.assertIn("aiTurnText.setText(SceneTypography.render(visible,accent()))",ui)
        self.assertIn("boolean generateTurn(",client)

if __name__=="__main__":
    unittest.main()
