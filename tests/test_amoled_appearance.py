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

    def test_navigation_has_no_outlines_and_fixed_icon_colors(self):
        ui=code("MainActivity.java")
        nav=ui.split("private void renderNav(){",1)[1].split("public void toast",1)[0]
        self.assertNotIn("RuneFrameDrawable",nav)
        self.assertNotIn("SkyrimRuleView",nav)
        self.assertIn("nav.setBackground(bg(0xff090909,Color.TRANSPARENT,24))",nav)
        self.assertIn("area.setBackground(bg(selected?0xff202020:Color.TRANSPARENT,Color.TRANSPARENT,16))",nav)
        self.assertIn("selected?NAV_ICON_ACTIVE:NAV_ICON_INACTIVE",nav)
        self.assertNotIn("selected?accent():0xff969da0",nav)
        self.assertIn("NAV_ICON_ACTIVE=0xfff2f2f2, NAV_ICON_INACTIVE=0xff969da0",ui)
    def test_story_icon_tint_does_not_depend_on_button_style(self):
        ui=code("MainActivity.java")
        self.assertNotIn("AmoledAppearance.buttons()==5?actionTint:accent()",ui)
        self.assertIn('chance>=0?skillGlyph(choiceSkillName(c.action)):"skills",accent());',ui)
        self.assertIn("ActionChoiceIcons.view(this,ActionChoiceIcons.resolve(option.combatAction,option.title,option.skill),skillGlyph(option.skill),accent());",ui)
        self.assertIn("ActionChoiceIcons.view(this,actionIcon,glyph,accent())",ui)
        self.assertIn("if(image==null)return new SkyrimIconView(context,fallback,tint);",code("ActionChoiceIcons.java"))
        self.assertIn("box.setBackground(new RuneFrameDrawable(PANEL,actionTint",ui)
