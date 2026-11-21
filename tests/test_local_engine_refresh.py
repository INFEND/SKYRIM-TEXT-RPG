"""The native Android model worker is repackaged whenever its code changes."""
import pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ENGINE=ROOT/"engine/src/main/java/dev/infend/skyrimtext"
class LocalAiEngineRefresh(unittest.TestCase):
    def test_version_and_release_metadata_are_synchronized(self):
        build=(ROOT/"engine/build.gradle").read_text(encoding="utf-8")
        module=(APP/"LocalAiEngineModule.java").read_text(encoding="utf-8")
        workflow=(ROOT/".github/workflows/android-ota.yml").read_text(encoding="utf-8")
        version=int(re.search(r"versionCode\s+(\d+)",build).group(1))
        self.assertGreaterEqual(version,6)
        self.assertIn(f"static final int VERSION={version};",module)
        self.assertIn('pathlib.Path("app/src/main/assets/engine_module.json").write_text',workflow)
    def test_large_gguf_default_mmap_and_bounded_batch(self):
        worker=(ENGINE/"LocalAiInferenceService.kt").read_text(encoding="utf-8")
        self.assertIn("size>=2L*1024*1024*1024",worker)
        self.assertIn('"n_batch" to (if (recovery) 16 else 32)',worker)
        self.assertIn('"use_mmap" to mmap',worker)
        self.assertNotIn("токены появятся по мере готовности",worker)
    def test_process_exit_diagnosis_still_keeps_manual_retry(self):
        client=(APP/"LocalAiClient.kt").read_text(encoding="utf-8")
        self.assertIn("repeat(12)",client)
        self.assertIn("Сбой повторился в сниженном режиме",client)
        self.assertIn("val next=if(lowMemory||previous>=1) 2 else 1",client)
        method=client.split("private fun recoverProcess(",1)[1].split("private fun currentGeneration(",1)[0]
        self.assertNotIn("connect(",method)
        self.assertNotIn("send(",method)
if __name__=="__main__": unittest.main()
