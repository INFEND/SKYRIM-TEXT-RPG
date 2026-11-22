"""Regression contracts for honest progress in item and location-image generation."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext"
VIEW = (ROOT / "GenerationStatusView.java").read_text(encoding="utf-8")
ITEM = (ROOT / "ItemWorkshopAi.java").read_text(encoding="utf-8")
MAIN = (ROOT / "MainActivity.java").read_text(encoding="utf-8")
IMAGES = (ROOT / "ImageApiClient.java").read_text(encoding="utf-8")
EDITOR = (ROOT / "WorkImageEditor.java").read_text(encoding="utf-8")


class GenerationProgress(unittest.TestCase):
    def test_shared_widget_tells_time_not_fake_percent(self):
        self.assertIn("SystemClock.elapsedRealtime()", VIEW)
        self.assertIn("ui.postDelayed(this, 1000)", VIEW)
        self.assertIn("Этап ", VIEW)
        self.assertIn("прогресс времени неизвестен", VIEW)
        self.assertIn("void dispose()", VIEW)
        self.assertNotIn('setProgress(100)', VIEW)

    def test_item_streaming_reports_real_text_and_phases(self):
        self.assertIn("interface Progress", ITEM)
        self.assertIn('response.output_text.delta', ITEM)
        self.assertIn('"Получено "+result.length()+" символов от ИИ"', ITEM)
        self.assertIn('report(progress,4,"Проверка ответа"', ITEM)
        self.assertIn("ItemWorkshopAi.draft(token,model,request,", MAIN)
        self.assertIn("proposalProgress.start(1,4", MAIN)
        self.assertIn("proposalProgress.report(stage,total,title,details)", MAIN)
        self.assertIn("proposalProgress.finish(true,", MAIN)
        self.assertIn("proposalProgress.finish(false,", MAIN)
        self.assertIn("proposalProgress.dispose()", MAIN)

    def test_api_images_report_server_wait_download_and_verification(self):
        self.assertIn("interface Progress", IMAGES)
        self.assertIn("static void request(Context context", IMAGES)
        self.assertIn("request(context, prompt, model, quality, size, count, reference, null, callback)", IMAGES)
        self.assertIn("readLimitedResponse(conn.getInputStream()", IMAGES)
        self.assertIn("output.size() / 1048576.0", IMAGES)
        for stage in ('"Проверка запроса"', '"Подготовка исходника"',
                      '"Генерация на сервере"', '"Загрузка ответа"',
                      '"Проверка изображений"'):
            self.assertIn(stage, IMAGES)
        self.assertIn("GenerationStatusView progress = new GenerationStatusView(host, accent)", EDITOR)
        self.assertIn("progress.start(1, 5", EDITOR)
        self.assertIn("progress::report", EDITOR)
        self.assertIn('setNegativeButton("Скрыть", null)', EDITOR)
        self.assertIn("progress.dispose()", EDITOR)


if __name__ == "__main__":
    unittest.main()
