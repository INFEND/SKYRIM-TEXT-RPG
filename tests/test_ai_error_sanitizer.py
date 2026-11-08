import pathlib
import unittest
SRC=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext/AiStoryClient.java"
class ServerErrorSanitization(unittest.TestCase):
    def test_error_codes_not_accidentally_removed(self):
        s=SRC.read_text()
        block=s.split('private static String safeServerText(String raw)',1)[1].split('private static String context(',1)[0]
        self.assertIn("replace('\\\\n',' ')",block.replace("replace('\\n',' ')", "replace('\\\\n',' ')"))
        self.assertNotIn('replaceAll("[\\\\\\\\x00-',block)
        self.assertIn("sk-[a-z0-9_-]{8,}",block)
        self.assertIn('return sanitized.substring',block)
    def test_supported_chatgpt_payload_preserved(self):
        s=SRC.read_text()
        self.assertIn('if(!chatGptPlan)',s)
        self.assertIn('payload.put("max_output_tokens",AiReasoningEffort.apiOutputLimit(selectedEffort))',s)
        self.assertIn('payload.put("input", inputMessages)',s)
if __name__=='__main__':unittest.main()
