import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
def source(s):return (SRC/s).read_text(encoding="utf-8")
class AiLockedFreeActionContract(unittest.TestCase):
    def test_lightweight_probability_endpoint(self):
        client=source("AiStoryClient.java")
        turn=source("AiGameTurn.java")
        self.assertIn("boolean generateChance(",client)
        self.assertIn("AI_CHANCE_RULES",client)
        self.assertIn("chanceOnly?AI_CHANCE_RULES:AI_GAME_RULES",client)
        self.assertIn("parseChance(String response)",turn)
        self.assertIn('obj.has("customChance")',turn)
    def test_callbacks_released_before_new_ai_request(self):
        client=source("AiStoryClient.java")
        segment=client[client.index("} finally {"):client.index("} finally {")+850]
        self.assertLess(segment.index("running = false;"),segment.index("listener.onComplete(finished)"))
        self.assertIn("if(cancelled||sequence!=id)return;",segment)
    def test_custom_roll_survives_retry_and_process_death(self):
        view=source("MainActivity.java")
        for fragment in ("pendingAiAction()","storeAiPending(customAction,fixedChance,die)",
                         "clearAiPending();","lockedFreeInstruction",
                         'prefs.getInt(AI_PENDING_DIE,-1)',"aiClient.generateChance(token",
                         "aiClient.generateChance(key"):
            self.assertIn(fragment,view)
        self.assertIn("int fixedChance=AiGameTurn.parseChance(answer)",view)
        self.assertIn("if(value.equals(pendingAiAction()))",view)
        self.assertIn("if(expectedChance!=-2&&completed.customChance!=expectedChance)",view)
    def test_regular_ten_buttons_preserved(self):
        ui=source("MainActivity.java")
        self.assertIn("for(int i=0;i<game.aiTurn.choices.size();i++)",ui)
        self.assertIn("renderAiChoice(i+1,option)",ui)
        self.assertIn('addText(content,"СВОЙ ВАРИАНТ ОТВЕТА"',ui)
if __name__=="__main__":unittest.main()
