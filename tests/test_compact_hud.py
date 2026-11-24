"""Compact HUD and location climate must reflect actual saved game state."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(ROOT/"MainActivity.java").read_text()
WEATHER=(ROOT/"WorldWeather.java").read_text()
GALLERY=(ROOT/"LocationGalleryView.java").read_text()
class CompactHud(unittest.TestCase):
    def test_stats_and_experience(self):
        for token in ('game.hp,game.maxHp','game.stamina,game.maxStamina','game.magicka,game.maxMagicka',
                      'game.experienceRequired()','game.progress.carryWeight()','String.valueOf(game.gold)',
                      'game.level>=100','hudBar(stats,"xp"','bar.setProgress(target,true)'):
            self.assertIn(token,UI)
    def test_no_duplicate_story_snapshot_and_short_experience_label(self):
        self.assertNotIn("renderStorySnapshot",UI)
        self.assertNotIn('"Опыт / осталось"',UI)
        self.assertIn('hudMetric(details,"Опыт","experience",game.xp+" / "',UI)
        self.assertIn('game.experienceRequired()',UI)
        self.assertNotIn('ВИДЫ ЛОКАЦИИ',UI)
        self.assertIn('content.addView(sceneGallery(game.progress.location)',UI)
        self.assertIn('overview.addView(sceneGallery(game.progress.location)',UI)
    def test_borderless_gallery_without_counter(self):
        for token in ('setBackgroundColor(Color.BLACK)', 'header.addView(heading,headingParams)',
                      'controls.addView(page', 'page.setText(captionFor(location,frame))',
                      'v->move(-1)', 'v->move(1)', 'v->show(index)',
                      'updateIndicators();', 'galleryBody.setExpanded(!collapsed,animate)',
                      'viewport.setClipToOutline(true)'):
            self.assertIn(token,GALLERY)
        for token in ('new RuneFrameDrawable', 'header.addView(page', 'String.format("%02d / %02d"'):
            self.assertNotIn(token,GALLERY)
    def test_gallery_uses_equal_round_dots_in_current_accent(self):
        for token in ('for(int i=0;i<visibleImageCount();i++)',
                      'new FrameLayout.LayoutParams(dp(7),dp(7),Gravity.CENTER)',
                      'new LayoutParams(dp(16),dp(28))',
                      'marker.setShape(GradientDrawable.OVAL)',
                      'marker.setColor(active?accent:0xff363636)',
                      'hit.setSelected(active)', 'v->show(index)'):
            self.assertIn(token,GALLERY)
        for token in ('hitSize.width=dp(active?', 'dotSize.width=dp(active?',
                      'if(imageCount(location)==1)indicators.setVisibility',
                      'Панорама Вайтрана'):
            self.assertNotIn(token,GALLERY)
        self.assertIn('{"Главные ворота","Главная улица","Рынок"',GALLERY)
    def test_gallery_weather_is_current_location_and_offline(self):
        self.assertEqual(UI.count('renderStoryLocationDirectory();'),2)
        directory=UI.split('private void renderStoryLocationDirectory(){',1)[1].split('private void renderCityArchitecture(',1)[0]
        self.assertIn('renderLocationWeather(content);',directory)
        self.assertLess(directory.index('renderLocationWeather(content);'),directory.index('renderCityArchitecture(content,city,world);'))
        self.assertIn('renderLocationWeather(overview);',UI)
        self.assertIn('if(place.name.equalsIgnoreCase(game.progress.location))renderLocationWeather(wrapper);',UI)
        self.assertIn('WorldWeather.at(game.region,game.progress.location,game.day,game.hour)',UI)
        for token in ('Math.floorMod','h/6','BASE[r]','timeGlyph()','formattedTemperature()'):
            self.assertIn(token,WEATHER)
        self.assertNotIn('Random',WEATHER)
        self.assertNotIn('http',WEATHER)
