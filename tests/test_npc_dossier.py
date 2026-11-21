"""Verify every profile has a text-card route and every curated dossier maps to exactly one NPC."""
import pathlib, re, shutil, subprocess, tempfile, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
class NpcDossierTest(unittest.TestCase):
    def test_dossier_registry(self):
        if not shutil.which("javac") or not shutil.which("java"): self.skipTest("JDK unavailable")
        probe=r'''
package dev.infend.skyrimtext;
public class DossierProbe {
  public static void main(String[] args){
    if(NpcDossier.count()<65)throw new AssertionError("curated profiles missing");
    for(String name:NpcDossier.names()){
      NpcProfiles.Profile p=NpcProfiles.find(name);
      if(p==null||p.template||p.author||p.mentioned||!p.name.equals(name))throw new AssertionError(name);
      NpcDossier.Entry d=NpcDossier.find(name);
      if(d.biography.isEmpty()||d.location.isEmpty()||d.story.isEmpty())throw new AssertionError("incomplete "+name);
    }
    if(NpcDossier.find("Эллисиф")!=NpcDossier.find("Элисиф Прекрасная"))throw new AssertionError("alias");
    if(NpcDossier.find("Стражник")!=null)throw new AssertionError("generic personal data");
  }
}
'''
        with tempfile.TemporaryDirectory() as f:
            src=pathlib.Path(f)/"DossierProbe.java";src.write_text(probe,encoding="utf-8")
            files=["NpcDossier.java","NpcProfiles.java","NpcNameMatcher.java","AuthorNpcProfiles.java","LoreNpcProfiles.java"]
            subprocess.run(["javac","-encoding","UTF-8","-d",f,*[str(JAVA/x) for x in files],str(src)],check=True,capture_output=True,text=True,timeout=45)
            subprocess.run(["java","-cp",f,"dev.infend.skyrimtext.DossierProbe"],check=True,capture_output=True,text=True,timeout=20)
    def test_reference_and_save_history_separated(self):
        s=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        for key in ("NpcDossier.find(name)","СОБЫТИЯ ЭТОГО СОХРАНЕНИЯ","Место в оригинальной игре","npcNativeChoices(display)","record.template||record.mentioned","npcSectionDialog(name,index,panel)"):
            self.assertIn(key,s)
