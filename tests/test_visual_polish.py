import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class VisualPolish(unittest.TestCase):
    def test_reveal_after_links_and_skip(self):
        s=(ROOT/"MainActivity.java").read_text()
        self.assertIn('makeItemsClickable(body,body.getText().toString());\\n        revealNarrative(narrative,body,s.body);'.replace('\\n','\n'),s)
        self.assertIn('ПОКАЗАТЬ ВЕСЬ ТЕКСТ',s)
        self.assertIn('styled.removeSpan(masks.remove(0))',s)
    def test_effects_queue_and_gear(self):
        s=(ROOT/"MainActivity.java").read_text()
        v=(ROOT/"VisualEffectsView.java").read_text()
        for effect in ('ash','dust','embers'):
            self.assertIn('"'+effect+'"',s);self.assertIn('"'+effect+'"',v)
        self.assertIn('noticeQueue.addLast(message)',s)
        self.assertIn('if(noticeShowing||noticeQueue.isEmpty()',s)
        self.assertIn('slot.equals(item.slot())',s)
        self.assertIn('chronicleFilter!=0&&kind!=chronicleFilter',s)
