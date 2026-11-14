package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Random;

/** Original small faction campaigns, not a reproduction of the original quests. */
final class FactionCampaign {
    static final int TASK_COUNT=6;
    static final String[] NAMES={"Соратники","Коллегия Винтерхолда","Гильдия воров","Тёмное Братство","Имперский легион","Братья Бури","Стража Рассвета"};
    static final String[] HOME={"Вайтран","Винтерхолд","Рифтен","Фолкрит","Солитьюд","Виндхельм","Рифтен"};
    static final String[] ICONS={"weapon","magic","lock","dagger","shield","shield","crossbow"};
    static final String[][] PLACES={{"Вайтран","Ривервуд","Вайтран","Ривервуд","Фолкрит","Вайтран"},{"Винтерхолд","Данстар","Винтерхолд","Данстар","Виндхельм","Винтерхолд"},{"Рифтен","Маркарт","Рифтен","Маркарт","Вайтран","Рифтен"},{"Фолкрит","Морфал","Фолкрит","Морфал","Рифтен","Фолкрит"},{"Солитьюд","Драконий Мост","Солитьюд","Драконий Мост","Морфал","Солитьюд"},{"Виндхельм","Данстар","Виндхельм","Данстар","Винтерхолд","Виндхельм"},{"Рифтен","Айварстед","Рифтен","Айварстед","Вайтран","Рифтен"}};
    static final String[][] TASKS={
        {"Проверить боевую выучку во дворе","Обследовать следы угрозы у лесопилки","Представить отчёт о защите путников","Разобраться с угрозой каравану","Подготовить защиту лесной дороги","Принять испытание ветерана"},
        {"Исследовать нестабильные чары","Сопоставить магические записи у северной гавани","Защитить выводы перед наставниками","Исследовать руны на побережье","Найти свидетеля магической аномалии","Представить итоговое исследование"},
        {"Проверить скрытный проход","Разобраться с пропавшей перепиской","Передать сведения связному","Проверить путь тайного груза","Восстановить сеть связных","Согласовать условия с гильдией"},
        {"Проверить доверие связного","Незаметно установить судьбу пропавшего послания","Доложить о результатах наблюдения","Обнаружить ложный след","Проверить тайник связного","Подтвердить верность организации"},
        {"Проверить строевую подготовку","Провести разведку у моста","Доложить о состоянии дороги","Укрепить пост на дороге","Согласовать снабжение гарнизона","Доложить о завершении операции"},
        {"Пройти испытание выносливости","Собрать сведения о северном тракте","Представить отчёт командиру","Проверить северный дозор","Организовать доставку припасов","Представить итоговый отчёт"},
        {"Подготовиться к дозору","Исследовать следы ночных существ","Передать наблюдения дозорным","Выяснить источник ночных слухов","Подготовить безопасную стоянку","Завершить проверку дозора"}};
    static final int[][] SKILLS={{0,9,12,0,3,1},{13,15,12,15,8,13},{9,10,12,9,11,12},{9,16,12,9,10,16},{3,9,12,3,12,0},{1,9,12,1,3,12},{2,14,12,9,14,2}};
    private final int[] stage=new int[NAMES.length],attemptDay=new int[NAMES.length];
    private final Random random=new Random();
    FactionCampaign(){java.util.Arrays.fill(attemptDay,-1);}
    int stage(int f){return valid(f)?stage[f]:0;}
    boolean valid(int f){return f>=0&&f<NAMES.length;}
    String rank(int f){return new String[]{"Не вступил","Новобранец","Посвящённый","Проверенный член","Ветеран","Доверенный член","Наставник","Заслуженный член"}[stage(f)];}
    String task(int f){int s=stage(f);return s==0?"Вступить в "+NAMES[f]:s>TASK_COUNT?"Цепочка поручений завершена":TASKS[f][s-1];}
    String place(int f){int s=stage(f);return s==0?HOME[f]:s>TASK_COUNT?HOME[f]:PLACES[f][s-1];}
    int chance(GameEngine g,int f){return chance(g,f,false);}
    int chance(GameEngine g,int f,boolean talk){int s=stage(f);return s<1||s>TASK_COUNT?-1:g.progress.chance(g,2,talk?12:SKILLS[f][s-1],18+s*5+(talk?5:0));}
    String description(int f){int s=stage(f);if(s==0)return "Представься местному представителю и согласись на поручения.";if(s>TASK_COUNT)return "Все поручения выполнены. Твои заслуги записаны в журнале.";
        return s<=3?"Представитель ждёт результата: "+task(f)+". Можно действовать с помощью профильного навыка или убеждения.":"Новая ступень доверия. "+task(f)+". Поручение требует самостоятельной проверки, а результат будет сохранён.";}

    String block(GameEngine g,int f){
        if(!valid(f)||!g.started)return "Нет доступного поручения";
        if("COMBAT".equals(g.mode))return "Сначала заверши бой";
        if(stage[f]>TASK_COUNT)return "Все поручения завершены";
        if(stage[f]==0&&((f==4&&stage[5]>0)||(f==5&&stage[4]>0)))return "Ты уже присягнул противоположной стороне";
        if(!place(f).equalsIgnoreCase(g.progress.location))return "Нужно прибыть: "+place(f);
        if(stage[f]>0&&attemptDay[f]==g.day)return "Следующая попытка в другой игровой день";
        if(stage[f]>0&&g.stamina<10)return "Нужно 10 выносливости";
        return "";
    }
    boolean perform(GameEngine g,int f){return perform(g,f,false);}
    boolean perform(GameEngine g,int f,boolean talk){
        if(!block(g,f).isEmpty())return false;
        if(stage[f]==0){stage[f]=1;g.progress.setQuestOutcome("faction_"+f,"ACTIVE");g.message="Ты вступаешь в организацию «"+NAMES[f]+"». Первое поручение: "+task(f);g.recordVisualEvent(g.message);return true;}
        int step=stage[f],skill=talk?12:SKILLS[f][step-1],probability=chance(g,f,talk);
        attemptDay[f]=g.day;g.stamina-=10;
        if(random.nextInt(100)>=probability){g.message="Поручение не удалось: "+TASKS[f][step-1]+". Повторить можно в другой день.";g.recordVisualEvent(g.message);return true;}
        stage[f]++;int reward=20+step*15;g.gold=(int)Math.min(Integer.MAX_VALUE,(long)g.gold+reward);
        g.reputation++;g.holdReputation[g.region]=Math.min(100,g.holdReputation[g.region]+1);
        g.progress.skillExperience(g,skill,25+step*15);
        if(stage[f]==4){g.progress.setQuestOutcome("faction_"+f,"COMPLETED");g.progress.setQuestOutcome("faction_veteran_"+f,"ACTIVE");}
        if(step>=4)g.progress.setQuestOutcome("faction_veteran_"+f,"ACTIVE");
        if(stage[f]>TASK_COUNT)g.progress.setQuestOutcome("faction_veteran_"+f,"COMPLETED");
        g.progress.sync(g);g.message="Выполнено: "+TASKS[f][step-1]+". "+NAMES[f]+" | "+rank(f)+" | +"+reward+" септимов.";
        g.recordVisualEvent(g.message);return true;
    }
    JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("stage",new JSONArray(stage)).put("attemptDay",new JSONArray(attemptDay));}catch(Exception ignored){}return o;}
    static FactionCampaign fromJson(JSONObject o){
        FactionCampaign f=new FactionCampaign();if(o==null)return f;JSONArray s=o.optJSONArray("stage"),d=o.optJSONArray("attemptDay");
        for(int i=0;i<NAMES.length;i++){if(s!=null)f.stage[i]=Math.max(0,Math.min(TASK_COUNT+1,s.optInt(i,0)));if(d!=null)f.attemptDay[i]=Math.max(-1,d.optInt(i,-1));}
        if(f.stage[4]>0&&f.stage[5]>0)f.stage[5]=0;
        return f;
    }
}
