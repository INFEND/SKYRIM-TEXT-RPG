"""Regression contracts for compact Skyrim workshop actions and per-category visibility."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
UI=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")

class WorkshopVisibilityUI(unittest.TestCase):
    def test_buttons_are_content_sized_and_fixed_height(self):
        self.assertIn('private void workshopAction(LinearLayout host,String category',UI)
        self.assertIn('line.addView(button,lp(-2,38))',UI)
        self.assertIn('host.addView(line,params)',UI)
        self.assertIn('button.setBackground(new RuneFrameDrawable(',UI)
        self.assertNotIn('ChatGPT Work · описание можно изменить',UI)

    def test_npc_card_uses_compact_action(self):
        card=UI.split('private void npcDetails(String name){',1)[1].split('private void npcGallerySection',1)[0]
        self.assertIn('workshopAction(panel,"npc"',card)
        self.assertNotIn('livingLink(panel,"СОЗДАТЬ / ЗАМЕНИТЬ ИЗОБРАЖЕНИЕ"',card)
        self.assertIn('refreshNpcPortrait()',card)

    def test_workshop_visibility_settings_only_no_style_switch(self):
        for key in ('workshop_buttons_visible','workshop_visible_location',
                    'workshop_visible_city','workshop_visible_npc','workshop_visible_item'):
            self.assertIn('"'+key+'"',UI)
        self.assertIn('foldAddonSection("addon_workshop"',UI)
        self.assertIn('prefs.getBoolean("workshop_buttons_visible",true)',UI)
        self.assertIn('prefs.getBoolean("workshop_visible_"+category,true)',UI)
        self.assertNotIn('workshop_compact_button_option',UI)
        self.assertIn('workshopAction(content,"item","✦  Мастерская предметов  ›"',UI)

    def test_all_artwork_entrypoints_are_guarded(self):
        self.assertIn('workshopAction(panel,"city"',UI)
        self.assertIn('workshopAction(wrapper,"location"',UI)
        self.assertIn('workshopAction(view,"item"',UI)
        self.assertIn('workshopAction(content,"location"',UI)
        self.assertIn('workButton.setVisibility(workshopVisible("item")',UI)

if __name__=="__main__":
    unittest.main()
