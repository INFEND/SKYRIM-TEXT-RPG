"""Regression protection for large ChatGPT model catalog responses."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext/ChatGptOAuth.java"

class ModelCatalogStreaming(unittest.TestCase):
    def test_streaming_parser_and_bounded_download(self):
        source=SRC.read_text(encoding="utf-8")
        self.assertIn("readModelCatalog(conn.getInputStream())",source)
        self.assertIn("new JsonReader(",source)
        self.assertIn("new LimitedInputStream(input,64L*1024*1024)",source)
        self.assertIn('if(!"list".equals(visibility)',source)
        self.assertIn('if(result.size()>=100)return result',source)
        model_handler=source.split("void loadModels(ModelListener listener)",1)[1].split("void signOut(StatusListener callback)",1)[0]
        self.assertNotIn("new JSONObject(read(conn.getInputStream()))",model_handler)
    def test_oauth_metadata_remains_separate_and_small(self):
        source=SRC.read_text(encoding="utf-8")
        self.assertIn("if(out.size()+count>512000)",source)
        self.assertIn("verifyIdToken",source)
        self.assertIn("chatgpt.tokens.use.direct",source)
if __name__=="__main__":
    unittest.main()
