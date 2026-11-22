"""Regression contracts for PocketOrca, prose readability and transparent checks."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
UI=(SRC/"MainActivity.java").read_text(encoding="utf-8")
POCKET=(SRC/"PocketOrcaClient.java").read_text(encoding="utf-8")
PROGRESS=(SRC/"RpgProgress.java").read_text(encoding="utf-8")


class PocketOrcaStoryPolish(unittest.TestCase):
    def test_retired_provider_is_absent_and_old_selection_is_migrated(self):
        self.assertFalse((SRC/"AiPacaClient.java").exists())
        self.assertFalse((SRC/"AiPacaKeyVault.java").exists())
        self.assertNotIn("aipacaClient", UI)
        self.assertNotIn("AiPaca |", UI)
        self.assertIn('if("aipaca".equals(mode))', UI)
        self.assertIn('prefs.edit().putString("ai_provider","pocketorca")', UI)
        self.assertIn('keys.deleteEntry("skyrim_aipaca_api_v1")', UI)
        self.assertIn('getSharedPreferences("skyrim_aipaca_vault",MODE_PRIVATE).edit().clear()', UI)

    def test_pocketorca_builds_prompts_off_main_thread_and_streams_safely(self):
        generate=POCKET.split("synchronized boolean generate(",1)[1].split("private void execute(",1)[0]
        self.assertIn("worker.execute(() -> {", generate)
        self.assertIn("AiStoryClient.localUserPrompt(", generate)
        self.assertIn("catch (RuntimeException failure)",generate)
        self.assertIn("POCKETORCA_STYLE",POCKET)
        self.assertIn('if (now - lastUpdate >= 300L)',POCKET)
        self.assertIn('catch(RuntimeException uiError)', POCKET)
        self.assertIn('new URL(baseUrl() + route)',POCKET)
        self.assertIn('Proxy.NO_PROXY',POCKET)
        self.assertIn('setInstanceFollowRedirects(false)',POCKET)

    def test_no_roll_choices_are_visibly_100_percent_without_reroll(self):
        self.assertNotIn('"БЕЗ ПРОВЕРКИ"',UI)
        self.assertIn('addChoiceStat(prose,"ШАНС УСПЕХА","100%"',UI)
        self.assertIn('addChoiceStat(detail,"ШАНС УСПЕХА","100%"',UI)
        self.assertIn('instruction.append("шанс 100%, бросок не требуется.")',UI)
        self.assertIn('int die=option.chance<0?-1:AiGameTurn.roll()',UI)
        self.assertIn('AiGameTurn.success(option.chance,die)',UI)

    def test_skills_show_actual_name_and_level_not_model_raw_value(self):
        self.assertIn("private int choiceSkillIndex(String skill)",UI)
        self.assertIn('game.progress.skillLevel(index)+" (уровень)"',UI)
        self.assertIn('showChoiceSkillLevel(detail,option.skill,option.combatAction)',UI)
        self.assertIn('showChoiceSkillLevel(prose,choiceSkillName(c.action),c.action)',UI)
        self.assertNotIn('addChoiceStat(detail,"НАВЫК",option.skill',UI)

    def test_chance_calculations_are_truthful(self):
        self.assertIn('private String aiChoiceCalculation(',UI)
        self.assertIn('ИИ не прислал числовую разбивку',UI)
        self.assertIn('chanceExplanation="ИГРОВОЙ РАСЧЁТ ШАНСА',PROGRESS)
        self.assertIn('ИТОГ: ',PROGRESS)
        self.assertIn('Ограничение до диапазона 0–100%',PROGRESS)
        self.assertIn('return result;',PROGRESS)

    def test_story_font_follows_game_readability_setting(self):
        self.assertEqual(UI.count('aiTurnText=text("",16,TEXT,false)'),2)
        self.assertEqual(UI.count('aiTurnText.setLineSpacing(0f,1f)'),2)
        self.assertIn('reply.setLineSpacing(0f,1f)',UI)
        self.assertIn('return Math.max(10,size+fontBonus)',UI)
        self.assertIn('AiNarrativeFormat.compact(accumulated)',UI)
        self.assertNotIn('aiTurnText=text("",20,TEXT,false)',UI)
        self.assertIn('TextView title=text(option.title,19,TEXT,true)',UI)
        self.assertIn('addText(detail,option.detail,17,MUTED,false,8)',UI)
        self.assertIn('TextView link=text("Показать расчёт  ⌄",15,MUTED,true)',UI)
        self.assertIn('prompt.setTextSize(sp(18))',UI)


if __name__=="__main__":
    unittest.main()
