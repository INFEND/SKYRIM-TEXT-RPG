package dev.infend.skyrimtext;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class NpcNameMatcherTest {
    @Test public void fullNamesAliasesAndInflectionResolveToOneNpc(){
        NpcNameMatcher m=new NpcNameMatcher(NpcProfiles.linkNames());
        List<NpcNameMatcher.Match> result=m.find("Эллисиф Прекрасная встретила Ульфрика Буревестника и Серану.");
        assertEquals(3,result.size());
        assertEquals("Элисиф Прекрасная",result.get(0).name);
        assertEquals("Ульфрик Буревестник",result.get(1).name);
        assertEquals("Серана",result.get(2).name);
        assertEquals("Эллисиф Прекрасная","Эллисиф Прекрасная встретила Ульфрика Буревестника и Серану.".substring(result.get(0).start,result.get(0).end));
    }
    @Test public void noSubstringOrDuplicateAndDynamicNames(){
        Map<String,String> names=NpcProfiles.linkNames();NpcNameMatcher.add(names,"Лира");
        NpcNameMatcher m=new NpcNameMatcher(names);
        assertEquals(0,m.find("Серанажды, Альманах").size());
        List<NpcNameMatcher.Match> hits=m.find("Лира: СЕРАНА. Серана!");
        assertEquals(3,hits.size());assertEquals("Лира",hits.get(0).name);
        assertEquals("Серана",hits.get(1).name);assertEquals("Серана",hits.get(2).name);
    }
    @Test public void cityNamesOpenCanonicalCard(){
        NpcNameMatcher m=new NpcNameMatcher(NpcProfiles.linkNames());
        assertEquals("Фолк Огнебород",m.find("Передать ром Фолку").get(0).name);
        assertEquals("Грета",m.find("Поговорить с Гретой").get(0).name);
    }
}
