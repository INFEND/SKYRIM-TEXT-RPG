package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Exercise the real save parser and new-game initialization, including legacy data. */
public class CharacterIdentityTest {
    private GameEngine create(String race,String gender,int portrait){
        GameEngine game=new GameEngine();
        game.start("Элин",race,"Без класса",0,new int[]{2,1,2},gender,portrait);
        return game;
    }
    @Test public void allSixtyAppearancesRoundTripThroughSaves()throws Exception{
        Set<String> paths=new HashSet<>();
        for(String race:RpgContent.RACES)for(String gender:CharacterIdentity.GENDERS){
            for(int portrait=1;portrait<=3;portrait++){
                GameEngine original=create(race,gender,portrait);
                original.gold=391;original.day=8;
                GameEngine restored=GameEngine.fromJson(original.toJson().toString());
                assertTrue(restored.started);
                assertEquals("Элин",restored.name);assertEquals(race,restored.race);
                assertEquals(gender,restored.gender);assertEquals(portrait,restored.portrait);
                assertEquals(391,restored.gold);assertEquals(8,restored.day);
                assertTrue(paths.add(CharacterIdentity.portraitPath(race,gender,portrait)));
            }
        }
        assertEquals(60,paths.size());
    }
    @Test public void oldSchemaOneSaveRetainsProgressWithoutAppearanceFields()throws Exception{
        GameEngine original=create("Данмер",CharacterIdentity.FEMALE,3);
        original.gold=777;original.level=7;original.day=21;
        JSONObject legacy=original.toJson();legacy.remove("gender");legacy.remove("portrait");
        GameEngine restored=GameEngine.fromJson(legacy.toString());
        assertTrue(restored.started);assertEquals("Данмер",restored.race);
        assertEquals(777,restored.gold);assertEquals(7,restored.level);assertEquals(21,restored.day);
        assertEquals(CharacterIdentity.MALE,restored.gender);assertEquals(1,restored.portrait);
    }
    @Test public void invalidImportedAppearanceIsNormalizedWithoutDiscardingSave()throws Exception{
        JSONObject saved=create("Орк",CharacterIdentity.FEMALE,2).toJson();
        saved.put("gender","invalid").put("portrait",999).put("gold",654);
        GameEngine restored=GameEngine.fromJson(saved.toString());
        assertEquals(654,restored.gold);assertEquals(CharacterIdentity.MALE,restored.gender);
        assertEquals(3,restored.portrait);
        saved.put("gender",JSONObject.NULL).put("portrait",-20);
        restored=GameEngine.fromJson(saved.toString());
        assertEquals(CharacterIdentity.MALE,restored.gender);assertEquals(1,restored.portrait);
    }
    @Test public void genderAndPortraitDoNotAlterStartingAttributes(){
        for(String race:RpgContent.RACES){
            GameEngine male=create(race,CharacterIdentity.MALE,1);
            GameEngine female=create(race,CharacterIdentity.FEMALE,3);
            assertEquals(male.maxHp,female.maxHp);assertEquals(male.maxMagicka,female.maxMagicka);
            assertEquals(male.maxStamina,female.maxStamina);assertEquals(male.gold,female.gold);
            assertEquals(male.attack,female.attack);assertEquals(male.armor,female.armor);
        }
    }
    @Test public void restartingWithLegacyOverloadResetsPreviousAppearance(){
        GameEngine game=create("Каджит",CharacterIdentity.FEMALE,3);
        game.start("Фарен","Норд","Воин");
        assertEquals(CharacterIdentity.MALE,game.gender);assertEquals(1,game.portrait);
        assertEquals("portraits/nord_male_1.webp",
            CharacterIdentity.portraitPath(game.race,game.gender,game.portrait));
    }
    @Test public void newGameRejectsOutOfRangeAppearance(){
        GameEngine game=create("Аргонианин",null,-1);
        assertEquals(CharacterIdentity.MALE,game.gender);assertEquals(1,game.portrait);
        game=create("Аргонианин",CharacterIdentity.FEMALE,999);
        assertEquals(3,game.portrait);
    }
}
