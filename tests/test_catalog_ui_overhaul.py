"""UI guardrails for inventory/category icons and compact Skyrim catalog."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(ROOT/"MainActivity.java").read_text(encoding="utf-8")
ICONS=(ROOT/"SkyrimIconView.java").read_text(encoding="utf-8")
META=(ROOT/"SceneChoiceMeta.java").read_text(encoding="utf-8")


class CatalogUiOverhaul(unittest.TestCase):
    def test_choice_values_are_the_only_bold_substrings(self):
        self.assertIn('String display=label+": "+value;',UI)
        self.assertIn('int start=display.length()-value.length()',UI)
        self.assertIn("new android.text.style.StyleSpan(Typeface.BOLD),start,display.length()",UI)
        self.assertIn('addChoiceStat(prose,"НАВЫК",SceneChoiceMeta.skillName(c.action)',UI)
        self.assertIn('addChoiceStat(prose,"ШАНС УСПЕХА",chance+"%"',UI)
        self.assertIn('addChoiceStat(detail,"НАВЫК",option.skill.isEmpty()',UI)
        self.assertIn('addChoiceStat(detail,"ШАНС УСПЕХА",option.chance+"%"',UI)
        self.assertIn('public static String skillName(String action)',META)

    def test_inventory_subtitle_replaces_old_copy(self):
        self.assertIn('section("СУМКА ПУТНИКА","Инвентарь","Предметы и снаряжение")',UI)
        self.assertNotIn('Оружие, доспехи, книги и другие находки.',UI)

    def test_icon_mappings_use_real_item_glyphs(self):
        for name,glyph in (
            ("валюта","septims"),("еда","food"),("напитки","drinks"),
            ("зелья","potion"),("пользовательское оружие","customweapon"),
            ("пользовательские предметы","customitems"),
        ):
            self.assertIn('case "'+name+'":',ICONS)
            self.assertIn('return "'+glyph+'";',ICONS)
        for match in ('септим','отмыч','факел','амулет','зель'):
            self.assertIn(match,ICONS)
        self.assertIn('SkyrimIconView.iconForCategory(group)',UI)
        self.assertIn('SkyrimIconView.iconForItem(item.name,item.category)',UI)

    def test_catalog_uses_compact_expandable_rows_and_no_list_weight(self):
        catalog=UI.split('private void showItemCatalog()',1)[1].split('private void showLibrary()',1)[0]
        self.assertIn('java.util.LinkedHashMap<String,java.util.ArrayList<ItemCatalog.Item>> groups',catalog)
        self.assertIn('ScrollView scrollCatalog=new ScrollView(this)',catalog)
        self.assertIn('header.setOnClickListener(v->{',catalog)
        self.assertIn('android.transition.AutoTransition().setDuration(200)',catalog)
        self.assertIn('itemRow.setMinimumHeight(dp(46))',catalog)
        self.assertIn('itemName.setMaxLines(2)',catalog)
        self.assertIn('lp(25,25)',catalog)
        self.assertIn('SkyrimIconView.iconForItem(item.name,item.category)',catalog)
        self.assertNotIn('item.icon',catalog)
        self.assertNotIn('item.weight',catalog)
        self.assertNotIn('simple_list_item_1',catalog)
        self.assertIn('if(searching&&!item.name.toLowerCase',catalog)


if __name__=="__main__":
    unittest.main()
