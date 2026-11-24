import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/dev/infend/skyrimtext/MainActivity.java"


class CityArchitectureLayout(unittest.TestCase):
    def test_compact_default_and_persisted_icon_mode(self):
        source = MAIN.read_text(encoding="utf-8")
        self.assertIn('prefs.getBoolean(CITY_ICONS_ONLY_PREF,false)', source)
        self.assertIn('heading.setMinimumHeight(dp(48))', source)
        self.assertIn('renderCityIconRow(host,city,groups,interactive)', source)
        self.assertIn('putBoolean(CITY_ICONS_ONLY_PREF,i==1)', source)

    def test_single_row_selection_and_shared_location_actions(self):
        source = MAIN.read_text(encoding="utf-8")
        self.assertIn('panel.addView(icons,lp(-1,48))', source)
        self.assertIn('new LinearLayout.LayoutParams(0,-1,1f)', source)
        self.assertIn('button.isSelected()?-1:index', source)
        self.assertIn('city_architecture_icon_selected_', source)
        self.assertIn('cityPlaceDialog(city,index,place,interactive)', source)

    def test_city_directory_below_weather_in_both_modes(self):
        source = MAIN.read_text(encoding="utf-8")
        self.assertEqual(source.count("renderStoryLocationDirectory();"), 2)
        helper = source.split("private void renderStoryLocationDirectory()",1)[1].split("private void renderCityArchitecture(",1)[0]
        self.assertLess(helper.index("renderLocationWeather(content)"),helper.index("renderCityArchitecture(content,city,world)"))
        self.assertNotIn("renderCityArchitecture();", source)
        gallery = MAIN.with_name("LocationGalleryView.java").read_text(encoding="utf-8")
        self.assertNotIn("addInteriorLinks()",gallery)
        self.assertIn("childPlaces(String city)",gallery)
