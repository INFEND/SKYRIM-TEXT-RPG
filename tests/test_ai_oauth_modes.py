import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
class ChatGptConnectionTests(unittest.TestCase):
    def test_official_oauth(self):
        code=(SRC/"ChatGptOAuth.java").read_text()
        for part in ("dynamic_agent_client","ext_agent_host_id","code_challenge_method","127.0.0.1","nonce","state","code_verifier","verifyIdToken","chatgpt.tokens.use.direct"):
            self.assertIn(part,code)
        self.assertNotIn("backend-api",code)
    def test_secure_credentials(self):
        code=(SRC/"AiOAuthVault.java").read_text()
        self.assertIn("AndroidKeyStore",code)
        self.assertIn("AES/GCM/NoPadding",code)
    def test_model_choice(self):
        catalog=(SRC/"AiModelCatalog.java").read_text()
        for model in ("gpt-6-luna","gpt-6.1-terra","gpt-6.1-sol","gpt-6-astra"):
            self.assertIn(model,catalog)
        client=(SRC/"AiStoryClient.java").read_text()
        self.assertIn('payload.put("model", model)',client)
        self.assertIn('"response.completed".equals(type)',client)
    def test_ui_preserves_game_actions(self):
        ui=(SRC/"MainActivity.java").read_text()
        self.assertIn("this::apiModelDialog",ui)
        self.assertIn("this::loginChatGpt",ui)
        self.assertIn("this::chatGptModelDialog",ui)
        self.assertIn("game.choose(c.action)",ui)
if __name__=="__main__":unittest.main()
