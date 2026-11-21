"""The live local GGUF stream must not rewind text or silently rerun a scene."""
import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=(ROOT/"app/src/main/java/dev/infend/skyrimtext/LocalAiClient.kt").read_text(encoding="utf-8")
WORKER=(ROOT/"engine/src/main/java/dev/infend/skyrimtext/LocalAiInferenceService.kt").read_text(encoding="utf-8")

class MonotonicLocalStream(unittest.TestCase):
    def test_rewind_causes_error_not_unnoticed_second_generation(self):
        self.assertIn("val rewound=tokens+2<lastTokens || partial.length+64<lastPartial.length",SRC)
        self.assertIn("if(rewound)",SRC)
        self.assertIn("failGeneration(id",SRC)
        self.assertIn("partial.length>=lastPartial.length",SRC)
        self.assertIn("listener.onText(partial)",SRC)
        self.assertNotIn("токены появятся по мере готовности",WORKER)

if __name__=="__main__":
    unittest.main()
