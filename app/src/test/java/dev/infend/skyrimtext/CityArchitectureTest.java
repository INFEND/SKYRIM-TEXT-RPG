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
}
