package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class CityArchitectureTest {
 @Test public void allNineCapitalsAndRavenRockHaveFiveNonemptyGroups(){
  String[] cities={"Вайтран","Рифтен","Виндхельм","Солитьюд","Маркарт","Фолкрит","Винтерхолд","Данстар","Морфал","Воронья Скала"};
  assertEquals(5,CityArchitecture.CATEGORIES.length);
  int total=0;
  for(String city:cities){
   java.util.List<java.util.List<String>> groups=CityArchitecture.groups(city);
   assertEquals(city,5,groups.size());
   java.util.HashSet<String> names=new java.util.HashSet<>();
   for(java.util.List<String> group:groups){assertFalse(city,group.isEmpty());for(String name:group){assertFalse(name.trim().isEmpty());assertTrue(city+": "+name,names.add(name));total++;}}
  }
  assertEquals(264,total);assertTrue(CityArchitecture.groups("Хелген").isEmpty());
 }
 @Test public void downloadedInteriorsMergeWithoutDuplicateAliases(){
  java.util.List<java.util.List<String>> groups=CityArchitecture.merge("Вайтран",
   java.util.Arrays.asList("«Дом воительницы»","Новая лавка","Новый храм"),
   (first,second)->first.equals("Кузница «Дом воительницы»")&&second.equals("«Дом воительницы»"));
  assertFalse(groups.get(3).contains("«Дом воительницы»"));
  assertTrue(groups.get(3).contains("Кузница «Дом воительницы»"));
  assertTrue(groups.get(3).contains("Новая лавка"));
  assertTrue(groups.get(4).contains("Новый храм"));
  assertFalse(CityArchitecture.groups("Вайтран").get(3).contains("Новая лавка"));
  java.util.List<java.util.List<String>> village=CityArchitecture.merge("Ривервуд",
    java.util.Arrays.asList("Кузница Алвора","Таверна «Спящий великан»"),(a,b)->false);
  assertEquals(5,village.size());
  assertEquals(2,village.get(3).size());
 }
}
