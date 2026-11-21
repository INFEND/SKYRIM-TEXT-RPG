package dev.infend.skyrimtext;

import org.junit.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.*;

public final class BestiaryCatalogTest {
    @Test public void entriesAreCompleteUniqueAndCategorized(){
        List<BestiaryCatalog.Entry> entries=BestiaryCatalog.all();
        assertTrue("Expected a substantial bestiary",entries.size()>=125);
        assertTrue(BestiaryCatalog.canonicalCount()>95);
        assertEquals(3,BestiaryCatalog.gameCount());
        assertTrue(BestiaryCatalog.groups().size()>=9);
        HashSet<String> names=new HashSet<>();
        int authors=0,game=0,canonical=0;
        for(BestiaryCatalog.Entry e:entries){
            assertTrue("Duplicate "+e.name,names.add(e.name.toLowerCase(Locale.ROOT).replace('ё','е')));
            for(String field:new String[]{e.name,e.category,e.origin,e.habitat,e.appearance,e.behavior,
                e.strengths,e.weaknesses,e.abilities,e.threat,e.loot,e.legend})
                assertFalse(e.name+" has an empty field",field.trim().isEmpty());
            assertTrue(e.fullDescription().contains(e.legend));
            if(BestiaryCatalog.AUTHOR.equals(e.origin))authors++;
            else if(BestiaryCatalog.GAME.equals(e.origin))game++;
            else canonical++;
        }
        assertEquals(BestiaryCatalog.canonicalCount(),canonical);
        assertEquals(BestiaryCatalog.gameCount(),game);
        assertEquals(entries.size(),canonical+authors+game);
        assertEquals(23,authors);
    }

    @Test public void encountersAndAliasesOpenTheCorrectEntry(){
        assertEquals("Снежный волк",BestiaryCatalog.find(" СНЕЖНЫЙ ВОЛК ").name);
        assertEquals("Морозный паук",BestiaryCatalog.find("морозный паук").name);
        assertEquals("Волк",BestiaryCatalog.find("Волки").name);
        assertEquals("Скелет",BestiaryCatalog.find("Скелеты").name);
        assertEquals("Корус",BestiaryCatalog.find("Корусов").name);
        assertEquals("Пепельное порождение",BestiaryCatalog.find("Порождения пепла").name);
        assertEquals("Пещерный охотник",BestiaryCatalog.find("Пещерный охотник").name);
        assertEquals("Ледяной призрак",BestiaryCatalog.find("Ледяной призрак").name);
        assertEquals("Мирмулнир",BestiaryCatalog.find("Мирмулнир").name);
        assertEquals("Инсектоиды",BestiaryCatalog.find("Кикимора").type);
        assertEquals("Трупоеды",BestiaryCatalog.find("Гуль").type);
        assertNull(BestiaryCatalog.find("Неизвестное существо"));
    }

    @Test public void filterSearchesHabitatAndAbilitiesWithoutLosingEntries(){
        assertEquals(BestiaryCatalog.all().size(),BestiaryCatalog.filter("",BestiaryCatalog.ALL).size());
        assertEquals(23,BestiaryCatalog.filter("","Авторские существа").size());
        assertTrue(BestiaryCatalog.filter("Морфал","Авторские существа").stream()
            .anyMatch(e->e.name.equals("Мёртвый утопленник")));
        assertTrue(BestiaryCatalog.filter("яд",BestiaryCatalog.ALL).stream()
            .anyMatch(e->e.name.equals("Морозный паук")));
        assertTrue(BestiaryCatalog.filter("нетч","Солстейм").size()>=4);
        assertTrue(BestiaryCatalog.filter("КИКИМОРА",BestiaryCatalog.ALL).size()>=3);
        assertTrue(BestiaryCatalog.filter("несуществующий запрос",BestiaryCatalog.ALL).isEmpty());
    }
}
