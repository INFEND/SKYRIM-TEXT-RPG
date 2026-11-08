"""Ensure reasoning effort is selectable, persisted and sent on both AI paths."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class AiReasoningEffortTests(unittest.TestCase):
    def test_selection_and_auto_fallback(self):
        java=(SRC/"AiReasoningEffort.java").read_text(encoding="utf-8")
        for level in ("auto","low","medium","high","xhigh","max"):
            self.assertIn('"' + level + '"',java)
        self.assertIn('DEFAULT.equals(normalize(value))',java)
        self.assertIn('networkReadTimeoutMs',java)

    def test_payload_has_actual_reasoning_not_prompt_injection(self):
        src=(SRC/"AiStoryClient.java").read_text(encoding="utf-8")
        self.assertIn('new JSONObject().put("effort",selectedEffort)',src)
        self.assertIn('payload.put("reasoning",',src)
        self.assertIn('if(!AiReasoningEffort.isAutomatic(selectedEffort))',src)
        self.assertIn('AiReasoningEffort.apiOutputLimit(selectedEffort)',src)
        self.assertIn('AiReasoningEffort.networkReadTimeoutMs(selectedEffort)',src)
        self.assertIn('if(!chatGptPlan)',src)
        self.assertIn('payload.put("input", inputMessages)',src)
        self.assertIn('payload.put("store", false)',src)

    def test_ui_persists_choice_and_uses_correct_provider(self):
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('"ai_reasoning_effort"',ui)
        self.assertIn('this::aiReasoningEffortDialog',ui)
        self.assertIn('aiClient.generate(token,selectedModel,true,reasoningEffort,game,history,request,responseListener)',ui)
        self.assertIn('aiClient.generate(key,selectedModel,false,reasoningEffort,game,history,request,responseListener)',ui)
        self.assertIn('game.choose(c.action)',ui)
if __name__=="__main__":
    unittest.main()
