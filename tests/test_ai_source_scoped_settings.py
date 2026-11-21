"""The settings page is scoped to the selected AI backend, not all providers."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
UI=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")
SCOPED=UI.split("    private void renderSettings(){",1)[1].split(
    'foldSettingsSection("ai","ИИ-ПРИКЛЮЧЕНИЕ И МОДЕЛИ",aiStart)',1)[0]
ROOT_JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class SourceScopedSettings(unittest.TestCase):
    def test_provider_resolves_one_active_group(self):
        self.assertIn("String provider=aiProviderMode()",SCOPED)
        self.assertIn("String effectiveProvider=resolvedAiProvider()",SCOPED)
        local=SCOPED.index('if("local".equals(effectiveProvider))')
        chat=SCOPED.index('}else if("chatgpt".equals(effectiveProvider))')
        api=SCOPED.index('}else if("api".equals(effectiveProvider))')
        empty=SCOPED.index('}else{',api)
        self.assertLess(local,chat)
        self.assertLess(chat,api)
        self.assertLess(api,empty)
        self.assertIn('actionButton("Локальная модель | GGUF"',SCOPED[local:chat])
        self.assertNotIn("aiKeyDialog",SCOPED[local:chat])
        self.assertNotIn("chatGptModelDialog",SCOPED[local:chat])
        self.assertIn('actionButton("Модель | Работа/Codex"',SCOPED[chat:api])
        self.assertNotIn("selectLocalModel",SCOPED[chat:api])
        self.assertNotIn("aiKeyDialog",SCOPED[chat:api])
        self.assertIn('actionButton("Модель | OpenAI API"',SCOPED[api:empty])
        self.assertNotIn("selectLocalModel",SCOPED[api:empty])
        self.assertNotIn("chatGptModelDialog",SCOPED[api:empty])
        self.assertIn("Ни один источник ИИ не настроен.",SCOPED[empty:])

    def test_exactly_one_account_action(self):
        chat=SCOPED.split('}else if("chatgpt".equals(effectiveProvider))',1)[1].split(
            '}else if("api".equals(effectiveProvider))',1)[0]
        self.assertIn("boolean signedIn=chatGptOAuth.hasSignedInAccount()",chat)
        self.assertIn("boolean authorized=chatGptOAuth.hasAuthorizedPlan()",chat)
        self.assertIn("if(authorized&&!accountActionBusy)",chat)
        self.assertIn("if(signedIn)",chat)
        self.assertIn('signedIn?"Выйти из ChatGPT":"Войти в ChatGPT"',chat)
        self.assertIn("signedIn?this::logoutChatGpt:this::loginChatGpt",chat)
        self.assertEqual(chat.count("actionButton(accountTitle,accountDetail,false,accountAction)"),1)
        self.assertIn("accountRow.setEnabled(!accountActionBusy)",chat)
        self.assertIn("accountRow.setClickable(!accountActionBusy)",chat)
        self.assertIn("chatGptOAuth.signOut(",UI)
        self.assertIn("cancelAiForSourceChange()",UI)
        self.assertNotIn('actionButton("Вход ChatGPT',SCOPED)

    def test_no_bullet_separators_in_current_user_interface(self):
        for name in ("MainActivity.java","GameEngine.java","TravelPlanner.java",
                     "WorldAtlas.java"):
            source=(ROOT_JAVA/name).read_text(encoding="utf-8")
            # Legacy separators may occur only as input to the display normalizer.
            source=source.replace('str.replace(" • "," | ")','str.replace("LEGACY_SEPARATOR"," | ")')
            self.assertNotIn("•",source,name)
        typography=(ROOT_JAVA/"SceneTypography.java").read_text(encoding="utf-8")
        self.assertIn("String text=displaySeparators(raw).replace",typography)
        self.assertIn("SceneTypography.displaySeparators(str)",UI)
        # Identifiers in old save data are deliberately not rewritten:
        old=(ROOT_JAVA/"RpgProgress.java").read_text(encoding="utf-8")
        self.assertIn("Пепельные печати • день ",old)


if __name__=="__main__":
    unittest.main()
