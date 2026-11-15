from pathlib import Path
import unittest
SRC=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
def code(name): return (SRC/name).read_text(encoding="utf-8")

class AmoledAppearanceTest(unittest.TestCase):
    def test_seven_styles_and_independent_persisted_preferences(self):
        a=code("AmoledAppearance.java")
        for name in ("Обсидиан","Золотые капсулы","Северные руны","Тёмное стекло","Имперский минимализм","Сюжетные действия","Плавающие кнопки"):
            self.assertIn(name,a)
        for key in ("amoled_button_style","amoled_window_style","amoled_button_motion","amoled_button_haptic"):
            self.assertIn(key,a)
        self.assertIn("windows<0?buttons:windows",a)
        self.assertIn("putInt(key,value).apply()",a)
    def test_render_and_dialogs_use_selected_style(self):
        ui=code("MainActivity.java")
        for token in ("AmoledAppearance.load(this)","amoledSettings();","AmoledAppearance.window(0xff080808,accent())","root.setBackgroundColor(BG)","navSize.setMargins","AmoledAppearance.storyTint(c.action,c.title","AmoledAppearance.storyTint(option.combatAction,option.title"):
            self.assertIn(token,ui)
        self.assertIn("AmoledAppearance.window",code("RuneDialogBuilder.java"))
        self.assertIn("AmoledAppearance.window",code("OtaProgressDialog.java"))
        self.assertIn("drawRoundRect",code("RuneFrameDrawable.java"))
    def test_press_animations_do_not_replace_touch_or_click_callbacks(self):
        a=code("AmoledAppearance.java")
        for token in ("StateListAnimator","RippleDrawable","scale?.96f:1f","HapticFeedbackConstants.KEYBOARD_TAP"):
            self.assertIn(token,a)
        self.assertNotIn("setOnTouchListener",a)
        self.assertNotIn("setOnClickListener",a)
        self.assertIn("AmoledAppearance.applyTree(root)",code("SovngardeTypeface.java"))
