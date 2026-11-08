"""Prevent HTTP 400 regressions with ChatGPT subscriber direct Responses route."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class ChatGptRequestContract(unittest.TestCase):
    def test_subscription_request_body_uses_supported_fields(self):
        source=(SRC/"AiStoryClient.java").read_text(encoding="utf-8")
        self.assertIn('new JSONObject().put("role","user").put("content",input)',source)
        self.assertIn('payload.put("input", inputMessages)',source)
        self.assertIn('payload.put("stream", true)',source)
        self.assertIn('payload.put("store", false)',source)
        self.assertIn('if(!chatGptPlan)payload.put("max_output_tokens",2200)',source)
        self.assertNotIn('payload.put("input", input)',source)
        self.assertIn('payload.put("instructions", RULES)',source)
        self.assertIn('formatHttpFailure(conn,status,chatGptPlan)',source)

    def test_right_auth_mode_and_no_invisible_payment_fallback(self):
        source=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('aiClient.generate(token,selectedModel,true,game,history,request,responseListener)',source)
        self.assertIn('aiClient.generate(key,selectedModel,false,game,history,request,responseListener)',source)
        self.assertIn('if(chatgpt)',source)
        self.assertIn('game.choose(c.action)',source)

    def test_safe_server_diagnostics(self):
        source=(SRC/"AiStoryClient.java").read_text(encoding="utf-8")
        self.assertIn('conn.getErrorStream()',source)
        self.assertIn('error.optString("param","")',source)
        self.assertIn('error.optString("code","")',source)
        self.assertIn('conn.getHeaderField("x-request-id")',source)
        self.assertIn('readError(conn.getErrorStream(),8192)',source)
        self.assertIn('safeServerText(diagnostic)',source)
        self.assertNotIn('System.out.println(token)',source)
if __name__=="__main__": unittest.main()
