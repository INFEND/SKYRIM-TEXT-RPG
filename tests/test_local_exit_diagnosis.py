import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ENGINE=ROOT/"engine/src/main/java/dev/infend/skyrimtext"
class LocalExitDiagnosis(unittest.TestCase):
    def test_no_speculative_memory_error_or_arbitrary_ram_gate(self):
        c=(APP/"LocalAiClient.kt").read_text()
        self.assertNotIn("Возможны нехватка памяти",c)
        self.assertNotIn("info.availMem<",c)
        self.assertIn('candidate?.getBoolean("found")==true',c)
        self.assertIn("if(tokens==0&&path.isNotEmpty())",c)
        self.assertIn("if(lowMemory)2 else 1",c)
    def test_diagnosis_matches_current_pid_and_request(self):
        p=(ENGINE/"EngineDiagnosticsProvider.java").read_text()
        self.assertIn("e.getPid()!=pid||e.getTimestamp()<since",p)
        self.assertIn("REASON_LOW_MEMORY",p)
        self.assertIn("REASON_CRASH_NATIVE",p)
        self.assertIn("checkSignatures(Binder.getCallingUid(),Process.myUid())",p)
    def test_worker_compatibility_profile_and_budget(self):
        w=(ENGINE/"LocalAiInferenceService.kt").read_text()
        self.assertIn("contextCapacity=if(recovery)6144 else 8192",w)
        self.assertIn('"use_mmap" to mmap',w)
        self.assertIn("engine.tokenize(context,direct + prefix)",w)
        self.assertIn('"n_predict" to outputBudget',w)
        self.assertIn('putInt("pid", Process.myPid())',w)
        self.assertIn("nativeEngine.isGGUF(uri)",w)
