package dev.infend.skyrimtext;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class NpcNameMatcherTest {
    @Test public void fullNamesAliasesAndInflectionResolveToOneNpc(){
        NpcNameMatcher m=new NpcNameMatcher(NpcProfiles.linkNames());
        String source="Эллисиф Прекрасная встретила Ульфрика Буревестника и Серану.";
        List<NpcNameMatcher.Match> result=m.find(source);
        assertEquals(3,result.size());
        assertEquals("Элисиф Прекрасная",result.get(0).name);
        assertEquals("Ульфрик Буревестник",result.get(1).name);
        assertEquals("Серана",result.get(2).name);
        assertEquals("Эллисиф Прекрасная",source.substring(result.get(0).start,result.get(0).end));
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
        assertEquals("Анжелина Моррар",m.find("Ароматы Анжелины").get(0).name);
        assertEquals("Йордис Дева Меча",NpcProfiles.canonicalName("Йордис"));
        assertSame(NpcProfiles.find("Серана"),NpcProfiles.find("Сераной"));
        assertNotNull(NpcProfiles.find("Капитан Алдис"));
        assertNotNull(NpcProfiles.find("Лоб"));
    }
    @Test public void lowercaseEverydayWordsDoNotBecomeNpcLinks(){
        Map<String,String> names=new LinkedHashMap<>();
        NpcNameMatcher.add(names,"Мир");
        NpcNameMatcher.add(names,"Серана","Серану");
        NpcNameMatcher matcher=new NpcNameMatcher(names);
        List<NpcNameMatcher.Match> hits=matcher.find("В обычном тексте мир важнее, чем легенда. Мир встретил Серану. серана здесь только слово.");
        assertEquals(2,hits.size());
        assertEquals("Мир",hits.get(0).name);
        assertEquals("Серана",hits.get(1).name);
    }
    @Test public void accentsAndApostrophesKeepOriginalTextOffsets(){
        NpcNameMatcher m=new NpcNameMatcher(NpcProfiles.linkNames());
        String source="Сера\u0301на встретила М’Айка.";
        List<NpcNameMatcher.Match> hits=m.find(source);
        assertEquals(2,hits.size());
        assertEquals("Сера\u0301на",source.substring(hits.get(0).start,hits.get(0).end));
        assertEquals("М’Айка",source.substring(hits.get(1).start,hits.get(1).end));
    }
}
