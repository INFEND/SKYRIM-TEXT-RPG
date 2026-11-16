import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ENGINE=ROOT/"engine/src/main/java/dev/infend/skyrimtext"
class LocalAiRecovery(unittest.TestCase):
    def test_one_recovery_and_fresh_request_id(self):
        c=(APP/"LocalAiClient.kt").read_text()
        for s in ('if(recoveryUsed||original==null', 'recoveryUsed=true', 'val retryId=synchronized(guard){++generation}', 'data.putString("resumeText",lastPartial)', 'data.putInt("resumeTokens",lastTokens)', 'Context.BIND_IMPORTANT', 'armWatchdog(retryId)', 'requestData=null'):
            self.assertIn(s,c)
    def test_resume_preserves_output_budget_and_save_gate(self):
        w=(ENGINE/"LocalAiInferenceService.kt").read_text()
        for s in ('"n_ctx" to 8192', '"n_batch" to (if (recovery) 32 else 128)', '"n_predict" to (limit - previousTokens)', 'tokenCount = previousTokens', 'if (resume.isNotEmpty()) resume', 'count > previousTokens', 'getHistoricalProcessExitReasons'):
            self.assertIn(s,w)
        ui=(APP/"MainActivity.java").read_text()
        self.assertIn("AiGameTurn completed=AiGameTurn.parse(answer)",ui)
        self.assertIn("if(!game.toJson().toString().equals(saved))",ui)
