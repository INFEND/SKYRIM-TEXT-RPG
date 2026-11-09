import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/dev/infend/skyrimtext'

class LocalAiContract(unittest.TestCase):
    def test_local_mode_routes_to_native_bridge(self):
        main = (SRC / 'MainActivity.java').read_text(encoding='utf-8')
        self.assertIn('resolvedAiProvider()', main)
        self.assertIn('"local".equals(source)', main)
        self.assertIn('localAiClient.generate(', main)
        self.assertIn('LocalAiModelStore.path(prefs)', main)
        self.assertIn('Intent.ACTION_OPEN_DOCUMENT', main)
        self.assertIn('localModelReady()', main)
        self.assertIn('if(localAiClient!=null)localAiClient.cancel()', main)

    def test_offline_mode_has_no_network_dependency(self):
        client = (SRC / 'LocalAiClient.kt').read_text(encoding='utf-8')
        worker = (SRC / 'LocalAiInferenceService.kt').read_text(encoding='utf-8')
        manifest = (ROOT / 'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
        self.assertIn('Llama.loadModel(', worker)
        self.assertIn('Llama.complete(', worker)
        self.assertIn('Llama.releaseModel(', worker)
        self.assertIn('android:process=":llm"', manifest)
        self.assertIn('class LocalAiInferenceService', worker)
        self.assertIn('contextSize=4096', worker)
        self.assertIn('Process.killProcess(Process.myPid())', worker)
        self.assertIn('context.bindService(', client)
        self.assertIn('onServiceDisconnected', client)
        self.assertIn('onBindingDied', client)
        self.assertNotIn('HttpURLConnection', client)
        self.assertNotIn('api.openai.com', client)
        self.assertIn('id!=generation', client)
        ai=(SRC / 'AiStoryClient.java').read_text(encoding='utf-8')
        self.assertIn('localUserPrompt(', ai)
        self.assertIn('crop(b.toString(),1300)', ai)
        self.assertNotIn('contextAi(game,conciseHistory,command)', ai)
        store = (SRC / 'LocalAiModelStore.java').read_text(encoding='utf-8')
        self.assertIn('getExternalFilesDir("models")', store)
        self.assertIn('StandardCopyOption.REPLACE_EXISTING', store)
        self.assertIn('FileOutputStream', store)
        self.assertIn('buffer[0]', store)

    def test_original_save_and_remote_paths_survive(self):
        main=(SRC / 'MainActivity.java').read_text(encoding='utf-8')
        self.assertIn('aiClient.generateTurn(token,model,true',main)
        self.assertIn('aiClient.generateTurn(key,model,false',main)
        self.assertIn('completed.apply(next,actionResolved)',main)
        self.assertIn('next.aiTurn=completed',main)
        build=(ROOT/'app/build.gradle').read_text()
        self.assertIn('llama-android:0.1.1',build)

if __name__=='__main__':
    unittest.main()
