"""Compile the actual NPC registry and exercise its links without Android."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class NpcLinkRuntime(unittest.TestCase):
    def test_registry_compiles_and_names_resolve(self):
        javac, java = shutil.which("javac"), shutil.which("java")
        if not javac or not java:
            self.skipTest("JDK is unavailable")
        probe = r'''
package dev.infend.skyrimtext;
import java.util.*;
public class NpcLinkProbe {
  static void check(boolean condition, String label) {
    if (!condition) throw new AssertionError(label);
  }
  public static void main(String[] args) {
    Map<String,String> names=NpcProfiles.linkNames();
    check(new HashSet<>(names.values()).size()>=390,"NPC registry lost entries");
    NpcNameMatcher matcher=new NpcNameMatcher(names);
    String source="Эллисиф Прекрасная встретила Ульфрика Буревестника и Серану.";
    List<NpcNameMatcher.Match> hits=matcher.find(source);
    check(hits.size()==3,"three NPCs");
    check(hits.get(0).name.equals("Элисиф Прекрасная"),"Elisif alias");
    check(hits.get(1).name.equals("Ульфрик Буревестник"),"longest alias");
    check(hits.get(2).name.equals("Серана"),"case form");
    check(NpcProfiles.find("Сераной")==NpcProfiles.find("Серана"),"same identity");
    check(NpcProfiles.canonicalName("Йордис").equals("Йордис Дева Меча"),"canonical surname");
    check(matcher.find("Ароматы Анжелины").get(0).name.equals("Анжелина Моррар"),"canonical inflection");
    check(matcher.find("Серанажды Альманах").isEmpty(),"not substrings");
    String accented="Сера\u0301на встретила М’Айка.";
    List<NpcNameMatcher.Match> accentedHits=matcher.find(accented);
    check(accentedHits.size()==2,"stress and apostrophe");
    check(accented.substring(accentedHits.get(0).start,accentedHits.get(0).end).equals("Сера\u0301на"),"offsets");
    check(NpcProfiles.find("Капитан Алдис")!=null && NpcProfiles.find("Лоб")!=null,"city profiles registered");
    NpcNameMatcher.add(names,"Лира");
    check(new NpcNameMatcher(names).find("Лира: Серана").size()==2,"dynamic speaker");
  }
}
'''
        with tempfile.TemporaryDirectory() as folder:
            folder = pathlib.Path(folder)
            src = folder / "NpcLinkProbe.java"
            src.write_text(probe, encoding="utf-8")
            subprocess.run([javac, "-encoding", "UTF-8", "-d", str(folder), str(JAVA/"NpcProfiles.java"), str(JAVA/"NpcNameMatcher.java"), str(src)], check=True, capture_output=True, text=True, timeout=45)
            subprocess.run([java, "-cp", str(folder), "dev.infend.skyrimtext.NpcLinkProbe"], check=True, capture_output=True, text=True, timeout=20)

    def test_card_and_link_guards(self):
        ui=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        for token in ("activeNpcDialog.isShowing()&&sameNpc", "npc_no_self_link",
                      "npcAddonEnabled(name)", "targets=matcher.find(choice.title)",
                      "current.updating=true", "removeOnGlobalLayoutListener(listener)",
                      "view.setMovementMethod(current.originalMovement)", "game.sceneState.npcNames()"):
            self.assertIn(token,ui)

if __name__=="__main__":
    unittest.main()
