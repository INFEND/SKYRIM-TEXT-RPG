package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class ActionChoiceIconsTest {
 @Test public void actionsAndSpecificLabels(){
  assertEquals(283,ActionChoiceIcons.TOTAL);
  assertEquals(1,ActionChoiceIcons.resolve("TALK","Поговорить с местными",""));
  assertEquals(29,ActionChoiceIcons.resolve("HEAVY","Сильный удар",""));
  assertEquals(48,ActionChoiceIcons.resolve("FLEE","Отступить",""));
  assertEquals(125,ActionChoiceIcons.resolve("","Открыть карту",""));
  assertEquals(222,ActionChoiceIcons.resolve("","Взять задание",""));
  assertEquals(162,ActionChoiceIcons.resolve("","Вскрыть замок",""));
  assertEquals(127,ActionChoiceIcons.resolve("REGION:4","Вайтран",""));
  assertEquals(94,ActionChoiceIcons.resolve("","Неизвестный вариант","Алхимия"));
  assertEquals(0,ActionChoiceIcons.resolve("","Неизвестный вариант",""));
 }
}
