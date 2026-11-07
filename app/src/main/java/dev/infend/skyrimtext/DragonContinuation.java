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
         "Составить карту свидетельств|Свести имена и даты в одну запись.",
         "Составить список укрытий|Подготовить безопасные места для семей, которым угрожает погоня.",
         "Проверить запасы переправы|Оценить припасы для эвакуации и предупредить о нехватке."},
        {"Сопоставить даты налётов|Найти повторяющийся порядок в донесениях.",
         "Выслушать купцов|Узнать, какие дороги ещё открыты.",
         "Осмотреть сгоревший обоз|Проверить опасные следы на окраине.",
         "Передать записи придворному магу|Получить заключение о древних знаках.",
         "Проверить подлинность приказа|Рискнуть обвинением в подлоге.",
         "Организовать помощь беженцам|Подготовить припасы для пострадавших.",
         "Проследить за тайным посланником|Попытаться выяснить источник утечки.",
         "Выбрать маршрут разведки|Составить план следующего похода.",
         "Согласовать охрану обозов|Убедить стражу выделить сопровождение на опасном тракте.",
         "Отметить безопасные ночлеги|Подготовить маршрут для людей, покидающих опасные деревни."},
        {"Изучить портовый журнал|Проверить пропавшие записи о судне.",
         "Выслушать докеров|Собрать показания рабочих пристани.",
         "Проникнуть в заброшенный склад|Проверить опасное убежище контрабандистов.",
         "Сверить торговые печати|Выявить поддельные разрешения.",
         "Вскрыть древний сундук|Рискнуть столкнуться с защитными чарами.",
         "Помочь семье пропавшего моряка|Найти свидетелей без угроз.",
         "Проследить за ночным караваном|Выяснить, куда увозят найденные реликвии.",
         "Составить досье для стражи|Подготовить доказательства к разбирательству.",
         "Устроить встречу свидетелей|Свести показания моряков и торговцев без публичных обвинений.",
         "Передать предупреждение капитанам|Помочь судам избежать подозрительного ночного маршрута."}
    };
    private static final int[][] SKILLS={{12,12,9,12,15,2,9,9,14,12},{15,12,9,13,12,14,9,9,12,14},{15,12,9,13,15,14,9,12,12,14}};
    private DragonContinuation(){}
    private static String questId(int stage){return "dragon_ash_"+stage;}
    private static void activateQuest(GameEngine g,int stage){
        if(stage>=0&&stage<TITLES.length)g.progress.setQuestOutcome(questId(stage),"ACTIVE");
    }
    private static int evidenceCount(GameEngine g,int stage){
        int n=0;
        for(int i=1;i<=OPTIONS[stage].length;i++)if(g.progress.dragonNextUsed.contains("DRAGON_NEXT:"+stage+":"+i) &&
                !g.progress.dragonNextFailed.contains("DRAGON_NEXT:"+stage+":"+i))n++;
        return n;
    }
    private static int failedCount(GameEngine g,int stage){
        int n=0;
        for(int i=3;i<=7;i+=2)if(g.progress.dragonNextFailed.contains("DRAGON_NEXT:"+stage+":"+i))n++;
        return n;
    }
    /** At least one dangerous lead must be proven, not merely attempted. */
    private static boolean hasRiskProof(GameEngine g,int stage){
        return proved(g,stage,3)||proved(g,stage,5)||proved(g,stage,7);
    }
    /** A chapter can fail only when all ten unique leads are exhausted. */
    private static boolean exhausted(GameEngine g,int stage){
        for(int i=1;i<=OPTIONS[stage].length;i++)
            if(!g.progress.dragonNextUsed.contains("DRAGON_NEXT:"+stage+":"+i))return false;
        return evidenceCount(g,stage)<3 || !hasRiskProof(g,stage);
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
    /** Windhelm verdict depends on proven, not merely attempted, evidence. */
    private static String windhelmVerdict(GameEngine g){
        boolean ledger=proved(g,2,1),workers=proved(g,2,2);
        boolean seals=proved(g,2,4),chest=proved(g,2,5);
        boolean family=proved(g,2,6),caravan=proved(g,2,7);
        if(ledger&&seals&&caravan)return "Поддельные разрешения и след каравана раскрыли сеть перевозчиков реликвий. Стража получила маршрут для перехвата.";
        if(workers&&family)return "Показания докеров и семьи моряка помогли защитить свидетелей. Следствие продолжится без массовых арестов.";
        if(chest&&seals)return "Сундук и торговые печати связали груз с древним культом. Реликвии переданы под охрану.";
        return "Досье передано страже, однако часть участников и маршрутов осталась неизвестной.";
    }
    private static boolean proved(GameEngine g,int stage,int choice){
        String key="DRAGON_NEXT:"+stage+":"+choice;
        return g.progress.dragonNextUsed.contains(key)&&!g.progress.dragonNextFailed.contains(key);
    }
    /** Report terminal failures without claiming that every chapter was successful. */
    private static int failedChapters(GameEngine g){
        int count=0;
        for(int i=0;i<TITLES.length;i++)
            if("FAILED".equals(g.progress.questOutcomes.get(questId(i))))count++;
        return count;
    }
    private static String questSummary(GameEngine g){
        StringBuilder summary=new StringBuilder();
        for(int i=0;i<TITLES.length;i++){
            if(i>0)summary.append("; ");
            String outcome=g.progress.questOutcomes.get(questId(i));
            summary.append(TITLES[i]).append(": ")
                .append("FAILED".equals(outcome)?"провалено":
                    "COMPLETED".equals(outcome)?"выполнено":"без окончательного результата");
        }
        return summary.append(".").toString();
    }
    public static String questObjective(GameEngine g){
        int stage=g.progress.dragonNextStage;
        if(stage>=3)return "Итог пепельного следа: "+questSummary(g)+" "+lead(g)+" "+windhelmVerdict(g);
        return "Активно: "+TITLES[stage]+" • "+PLACES[stage]
            +" • свидетельства "+evidenceCount(g,stage)+"/3 • опасная зацепка "+(hasRiskProof(g,stage)?"доказана":"не доказана")+" • неудачные вылазки "+failedCount(g,stage);
    }
    public static String status(GameEngine g){
        int stage=g.progress.dragonNextStage;
        return stage>=3?"Драконий след • расследования завершены ("+failedChapters(g)+" провалено)":
            TITLES[Math.max(0,stage)]+" • "+PLACES[Math.max(0,stage)];
    }
    public static GameEngine.Scene scene(GameEngine g){
        int stage=g.progress.dragonNextStage;
        if(stage>=3)return new GameEngine.Scene("ДРАКОНИЙ СЛЕД • ПРОДОЛЖЕНИЕ","Пепельный след",
            "Расследования в Ривервуде, Вайтране и Виндхельме завершены. "+questSummary(g)+" "+lead(g)+" "+windhelmVerdict(g)+" Дальнейшая экспедиция пока не реализована.")
            .primary("GO_WORLD","Вернуться в мир","Продолжить путешествие");
        boolean here=PLACES[stage].equals(g.progress.location);
        GameEngine.Scene s=new GameEngine.Scene("ДРАКОНИЙ СЛЕД • ГЛАВА "+(stage+7),TITLES[stage],
            STORIES[stage]+"\n\nЦЕЛЬ КВЕСТА: "+questObjective(g)+"\n\nМЕСТО: "+PLACES[stage]+(here?" • ты здесь":" • требуется путь")+
            (g.progress.dragonNextRecent.isEmpty()?"":"\n\nПОСЛЕДСТВИЕ\n"+g.progress.dragonNextRecent));
        s.primary("DRAGON_NEXT:"+stage+":0",here?"Завершить расследование":"Проложить путь в "+PLACES[stage],
            here?(exhausted(g,stage)?"Все зацепки исчерпаны: зафиксировать провал": "Собери три свидетельства ("+evidenceCount(g,stage)+"/3) и докажи одну опасную зацепку"):"Открыть маршруты");
        for(int i=1;i<=OPTIONS[stage].length;i++){
            String action="DRAGON_NEXT:"+stage+":"+i;
            String[] pair=OPTIONS[stage][i-1].split("\\|",2);
            boolean used=g.progress.dragonNextUsed.contains(action);
            boolean failed=g.progress.dragonNextFailed.contains(action);
            s.add(action,(failed?"✕ ":used?"✓ ":"")+pair[0],failed?"Попытка провалена, свидетельство не получено":used?"Свидетельство получено, действие завершено":here?pair[1]:"Сначала доберись до места");
        }
        return s.add("DRAGON_NEXT:"+stage+":99","Вернуться в мир","Продолжить расследование позже");
    }
    public static int chance(GameEngine g,String action){
        if(!"DRAGON".equals(g.mode)||action==null||!action.matches("DRAGON_NEXT:[012]:(?:[0-9]|10|99)"))return -1;
        String[] p=action.split(":");int stage=Integer.parseInt(p[1]),choice=Integer.parseInt(p[2]);
        if(stage!=g.progress.dragonNextStage||g.progress.dragonNextUsed.contains(action))return -1;
        if(!PLACES[stage].equals(g.progress.location)||(choice==3||choice==5||choice==7)&&g.stamina<7)return -1;
        if(choice!=3&&choice!=5&&choice!=7)return -1;
        return g.progress.chance(g,choice%3,SKILLS[stage][choice-1],29+stage*5);
    }
    public static void perform(GameEngine g,String action){
        if(!"DRAGON".equals(g.mode)||action==null||!action.matches("DRAGON_NEXT:[012]:(?:[0-9]|10|99)"))return;
        String[] p=action.split(":");int stage=Integer.parseInt(p[1]),choice=Integer.parseInt(p[2]);
        if(stage!=g.progress.dragonNextStage)return;
        if(choice==99){g.mode="WORLD";return;}
        activateQuest(g,stage);
        if(!PLACES[stage].equals(g.progress.location)){
            g.mode="TRAVEL";g.message="Доберись до "+PLACES[stage]+", чтобы продолжить расследование.";return;
        }
        if(choice==0){
            if(evidenceCount(g,stage)<3 || !hasRiskProof(g,stage)){
                if(!exhausted(g,stage)){g.progress.dragonNextRecent="Нужно три свидетельства ("+evidenceCount(g,stage)+"/3) и хотя бы одна успешно проверенная опасная зацепка.";return;}
                // Terminal failure is recorded once; advancing prevents a soft lock.
                g.progress.setQuestOutcome(questId(stage),"FAILED");
                g.progress.dragonNextStage++;
                activateQuest(g,g.progress.dragonNextStage);
                g.reputation=Math.max(-50,g.reputation-3);
                g.progress.dragonNextRecent="Расследование «"+TITLES[stage]+"» провалено: все десять зацепок исчерпаны, не удалось подтвердить обязательную опасную зацепку или собрать три свидетельства. Доверие жителей упало (-3 репутации).";
                g.progress.recent=g.progress.dragonNextRecent;
                return;
            }
            g.progress.setQuestOutcome(questId(stage),"COMPLETED");
            g.progress.dragonNextStage++;
            activateQuest(g,g.progress.dragonNextStage);
            g.addXp(30+stage*15);
            g.progress.dragonNextRecent=stage==0?"Ривервудские свидетельства переданы советникам Вайтрана.":stage==1?"Совет завершён. Следующая цель: Виндхельм. "+lead(g):"Стража Виндхельма получила досье. "+lead(g)+" "+windhelmVerdict(g);
            g.progress.completed.add(TITLES[stage]+" • день "+g.day);
            if(stage==2){
                // Apply the proven Windhelm outcome once, at quest completion only.
                // These consequences use existing schema-1 flags and attributes.
                if(proved(g,2,1)&&proved(g,2,4)&&proved(g,2,7)){
                    g.reputation=Math.min(50,g.reputation+5);
                    g.progress.dragonNextRecent+=" Перехват перевозчиков укрепил доверие городской стражи (+5 репутации).";
                }else if(proved(g,2,2)&&proved(g,2,6)){
                    g.reputation=Math.min(50,g.reputation+4);
                    g.progress.dragonNextRecent+=" Защищённые свидетели благодарят тебя (+4 репутации).";
                }else if(proved(g,2,5)&&proved(g,2,4)){
                    g.progress.dragonInsight=Math.min(15,g.progress.dragonInsight+3);
                    g.progress.dragonNextRecent+=" Исследование реликвий углубило понимание драконьих знаков (+3 прозрения).";
                }else{
                    g.reputation=Math.min(50,g.reputation+1);
                    g.progress.dragonNextRecent+=" Следствие осталось неполным, но город оценил помощь (+1 репутации).";
                }
                g.progress.recent="Квест «Драконий след: пепельный гонец» завершён. "+g.progress.dragonNextRecent;
            }
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
            if(choice==2||choice==6||choice==10||choice==11)g.reputation=Math.min(50,g.reputation+2);
            else if(choice==4||choice==8)g.progress.skillExperience(g,SKILLS[stage][choice-1],8);
            else g.progress.dragonInsight=Math.min(15,g.progress.dragonInsight+1);
            g.progress.dragonNextRecent=pair[0]+". "+pair[1]+" Выбор записан и не может быть повторён.";
        }
    }
}
