package dev.infend.skyrimtext;

import java.util.Random;

/** A new, independent continuation after the six preserved DragonArc chapters. */
public final class DragonContinuation {
    private static final String[] PLACES={"Ривервуд","Вайтран","Виндхельм"};
    private static final String[] TITLES={"Письмо под пеплом","Совет за закрытыми дверями","След пепельного гонца"};
    private static final String[] STORIES={
        "Дорога вниз с Глотки Мира кажется длиннее подъёма. У реки ещё стоит запах сырой древесины. В Ривервуде хранительница лесопилки показывает письмо, найденное в подкладке плаща пропавшего гонца. Печать сорвана, но на бумаге остались следы чёрного воска. В письме упоминаются наблюдатели у старых курганов. Жители спорят: предупредить стражу или сначала спасти людей, которые могли попасть в ловушку. Решение оставит след в твоей репутации.",
        "В Вайтране на столе советников лежат три донесения. В одном указаны следы на дороге к древнему погребению, во втором перечислены исчезнувшие обозы, третье испачкано золой и обрывается на середине строки. Стража хочет закрыть ворота, торговцы требуют открытой дороги, а придворный маг ищет закономерность в датах. Нельзя помочь всем сразу. Нужно решить, чьё свидетельство станет основой следующего похода.",
        "У ворот Виндхельма ветер гонит по камням серую золу. В портовом журнале исчезли записи о ночном судне, а в старом складе обнаружен сундук с печатью погребального культа. Стражники требуют арестов, моряки боятся потерять работу, и лишь несколько свидетелей готовы говорить. Собери доказательства прежде, чем решить судьбу подозреваемых."
    };
    private static final String[][] OPTIONS={
        {"Сверить почерк гонца|Сравнить письмо с записями на лесопилке.",
         "Предупредить семьи пропавших|Сообщить правду прежде, чем разойдутся слухи.",
         "Пройти по следу в овраге|Рискнуть здоровьем на осыпающемся склоне.",
         "Расспросить караванщика|Узнать, кто последним видел гонца.",
         "Расшифровать восковую печать|Попытаться прочесть скрытый знак.",
         "Укрепить переправу|Помочь жителям сохранить путь для эвакуации.",
         "Проверить ночной дозор|Выйти к опасному наблюдательному посту.",
         "Составить карту свидетельств|Свести имена и даты в одну запись."},
        {"Сопоставить даты налётов|Найти повторяющийся порядок в донесениях.",
         "Выслушать купцов|Узнать, какие дороги ещё открыты.",
         "Осмотреть сгоревший обоз|Проверить опасные следы на окраине.",
         "Передать записи придворному магу|Получить заключение о древних знаках.",
         "Проверить подлинность приказа|Рискнуть обвинением в подлоге.",
         "Организовать помощь беженцам|Подготовить припасы для пострадавших.",
         "Проследить за тайным посланником|Попытаться выяснить источник утечки.",
         "Выбрать маршрут разведки|Составить план следующего похода."},
        {"Изучить портовый журнал|Проверить пропавшие записи о судне.",
         "Выслушать докеров|Собрать показания рабочих пристани.",
         "Проникнуть в заброшенный склад|Проверить опасное убежище контрабандистов.",
         "Сверить торговые печати|Выявить поддельные разрешения.",
         "Вскрыть древний сундук|Рискнуть столкнуться с защитными чарами.",
         "Помочь семье пропавшего моряка|Найти свидетелей без угроз.",
         "Проследить за ночным караваном|Выяснить, куда увозят найденные реликвии.",
         "Составить досье для стражи|Подготовить доказательства к разбирательству."}
    };
    private static final int[][] SKILLS={{12,12,9,12,15,2,9,9},{15,12,9,13,12,14,9,9},{15,12,9,13,15,14,9,12}};
    private DragonContinuation(){}
    private static int evidenceCount(GameEngine g,int stage){
        int n=0;
        for(int i=1;i<=8;i++)if(g.progress.dragonNextUsed.contains("DRAGON_NEXT:"+stage+":"+i) &&
                !g.progress.dragonNextFailed.contains("DRAGON_NEXT:"+stage+":"+i))n++;
        return n;
    }
    /** Quest objective derived from existing schema-1 flags; no migration required. */
    private static String lead(GameEngine g){
        boolean courier=g.progress.dragonNextUsed.contains("DRAGON_NEXT:0:4");
        boolean seal=g.progress.dragonNextUsed.contains("DRAGON_NEXT:0:5")
            && !g.progress.dragonNextFailed.contains("DRAGON_NEXT:0:5");
        boolean dates=g.progress.dragonNextUsed.contains("DRAGON_NEXT:1:1");
        boolean scout=g.progress.dragonNextUsed.contains("DRAGON_NEXT:1:7")
            && !g.progress.dragonNextFailed.contains("DRAGON_NEXT:1:7");
        if(seal&&dates)return "Древняя печать и даты налётов указывают на пробуждение курганов.";
        if(courier&&scout)return "Показания караванщика и слежка раскрыли путь тайного посланника.";
        if(scout)return "След посланника ведёт к забытым погребениям.";
        if(dates)return "Даты нападений образуют тревожную последовательность.";
        return "Свидетельства собраны, но источник угрозы пока не установлен.";
    }
    public static String questObjective(GameEngine g){
        int stage=g.progress.dragonNextStage;
        if(stage>=3)return "Завершено: расследование пепельного следа. "+lead(g);
        return "Активно: "+TITLES[stage]+" • "+PLACES[stage]
            +" • свидетельства "+evidenceCount(g,stage)+"/3";
    }
    public static String status(GameEngine g){
        int stage=g.progress.dragonNextStage;
        return stage>=3?"Драконий след • пепельный след раскрыт":
            TITLES[Math.max(0,stage)]+" • "+PLACES[Math.max(0,stage)];
    }
    public static GameEngine.Scene scene(GameEngine g){
        int stage=g.progress.dragonNextStage;
        if(stage>=3)return new GameEngine.Scene("ДРАКОНИЙ СЛЕД • ПРОДОЛЖЕНИЕ","Пепельный след",
            "Расследования в Ривервуде, Вайтране и Виндхельме завершены. "+lead(g)+" Доказательства переданы стражникам. Дальнейшая экспедиция пока не реализована.")
            .primary("GO_WORLD","Вернуться в мир","Продолжить путешествие");
        boolean here=PLACES[stage].equals(g.progress.location);
        GameEngine.Scene s=new GameEngine.Scene("ДРАКОНИЙ СЛЕД • ГЛАВА "+(stage+7),TITLES[stage],
            STORIES[stage]+"\n\nЦЕЛЬ КВЕСТА: "+questObjective(g)+"\n\nМЕСТО: "+PLACES[stage]+(here?" • ты здесь":" • требуется путь")+
            (g.progress.dragonNextRecent.isEmpty()?"":"\n\nПОСЛЕДСТВИЕ\n"+g.progress.dragonNextRecent));
        s.primary("DRAGON_NEXT:"+stage+":0",here?"Завершить расследование":"Проложить путь в "+PLACES[stage],
            here?"Собери три свидетельства ("+evidenceCount(g,stage)+"/3)":"Открыть маршруты");
        for(int i=1;i<=8;i++){
            String action="DRAGON_NEXT:"+stage+":"+i;
            String[] pair=OPTIONS[stage][i-1].split("\\|",2);
            boolean used=g.progress.dragonNextUsed.contains(action);
            s.add(action,(used?"✓ ":"")+pair[0],used?"Уже выполнено":here?pair[1]:"Сначала доберись до места");
        }
        return s.add("DRAGON_NEXT:"+stage+":9","Вернуться в мир","Продолжить расследование позже");
    }
    public static int chance(GameEngine g,String action){
        if(!"DRAGON".equals(g.mode)||action==null||!action.matches("DRAGON_NEXT:[012]:[0-9]"))return -1;
        String[] p=action.split(":");int stage=Integer.parseInt(p[1]),choice=Integer.parseInt(p[2]);
        if(stage!=g.progress.dragonNextStage||g.progress.dragonNextUsed.contains(action))return -1;
        if(!PLACES[stage].equals(g.progress.location)||(choice==3||choice==5||choice==7)&&g.stamina<7)return -1;
        if(choice!=3&&choice!=5&&choice!=7)return -1;
        return g.progress.chance(g,choice%3,new int[]{9,15,12}[choice==3?0:choice==5?1:2],29+stage*5);
    }
    public static void perform(GameEngine g,String action){
        if(!"DRAGON".equals(g.mode)||action==null||!action.matches("DRAGON_NEXT:[01]:[0-9]"))return;
        String[] p=action.split(":");int stage=Integer.parseInt(p[1]),choice=Integer.parseInt(p[2]);
        if(stage!=g.progress.dragonNextStage)return;
        if(choice==9){g.mode="WORLD";return;}
        if(!PLACES[stage].equals(g.progress.location)){
            g.mode="TRAVEL";g.message="Доберись до "+PLACES[stage]+", чтобы продолжить расследование.";return;
        }
        if(choice==0){
            if(evidenceCount(g,stage)<3){g.progress.dragonNextRecent="Нужно три свидетельства: "+evidenceCount(g,stage)+"/3.";return;}
            g.progress.dragonNextStage++;
            g.addXp(30+stage*15);
            g.progress.dragonNextRecent=stage==0?"Ривервудские свидетельства переданы советникам Вайтрана.":stage==1?"Совет завершён. Следующая цель: Виндхельм. "+lead(g):"Стража Виндхельма получила досье. "+lead(g);
            g.progress.completed.add(TITLES[stage]+" • день "+g.day);
            if(stage==2)g.progress.recent="Квест «Драконий след: пепельный гонец» завершён. "+lead(g);
            return;
        }
        if(g.progress.dragonNextUsed.contains(action))return;
        boolean risky=choice==3||choice==5||choice==7;
        if(risky&&g.stamina<7){g.progress.dragonNextRecent="Слишком мало сил для опасной попытки.";return;}
        int probability=chance(g,action);
        g.progress.dragonNextUsed.add(action);g.day++;
        String[] pair=OPTIONS[stage][choice-1].split("\\|",2);
        if(risky){
            g.stamina=Math.max(0,g.stamina-7);
            if(new Random().nextInt(100)<probability){
                g.progress.dragonInsight=Math.min(15,g.progress.dragonInsight+2);
                g.progress.skillExperience(g,SKILLS[stage][choice-1],12+stage*4);
                g.progress.dragonNextRecent="Успех: "+pair[0]+". "+pair[1]+" Получены полезные свидетельства.";
            }else{
                g.progress.dragonNextFailed.add(action);
                g.hp=Math.max(1,g.hp-(4+stage*2));
                g.progress.dragonNextRecent="Неудача: "+pair[0]+". Свидетельство не получено, попытка исчерпана. Пришлось отступить с ранами.";
            }
        }else{
            if(choice==2||choice==6)g.reputation=Math.min(50,g.reputation+2);
            else if(choice==4||choice==8)g.progress.skillExperience(g,SKILLS[stage][choice-1],8);
            else g.progress.dragonInsight=Math.min(15,g.progress.dragonInsight+1);
            g.progress.dragonNextRecent=pair[0]+". "+pair[1]+" Выбор записан и не может быть повторён.";
        }
    }
}
