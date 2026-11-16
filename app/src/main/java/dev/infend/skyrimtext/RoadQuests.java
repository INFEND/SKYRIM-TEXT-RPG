package dev.infend.skyrimtext;
import java.util.*;

/** Saved, map-addressed follow-up tasks. These are compact text-RPG adaptations, not imported full side quests. */
final class RoadQuests {
    static final class Task {
        final String id,title,place,body,enemy,required,item;final int gold;
        Task(String id,String title,String place,String body,String enemy,String required,int gold,String item){this.id=id;this.title=title;this.place=place;this.body=body;this.enemy=enemy;this.required=required;this.gold=gold;this.item=item;}
        String key(){return "lead_"+id;}
        String combatKey(){return "road_cleared_"+id;}
    }
    static final Task[] TASKS={
        new Task("treasure","Спрятанный клад","Привал Хамвира","Записка приводит к каменным развалинам. Под плитой спрятан небольшой дорожный тайник.","","",80,""),
        new Task("mistwatch","Пленники Туманной заставы","Туманная застава","У крепости слышны голоса разбойников. Освободи пленников, не позволяя охране поднять тревогу.","Главарь разбойников","",90,""),
        new Task("pilgrim","Паломничество к Азуре","Святилище Азуры","Ты находишь святилище и разговариваешь с его служителями о дальнейшей судьбе Звезды Азуры.","","",15,""),
        new Task("afflicted","След Страдающих","Святилище Периайта","У святилища ты узнаёшь о заболевших и двемерских руинах Бтардамза. Это отдельная зацепка, не завершение даэдрического задания.","","",15,""),
        new Task("kynareth","У священного дерева","Вайтран","Ты расспрашиваешь служителей Кинарет о Златолисте. Они благодарят за доставленные вести.","","",20,""),
        new Task("island_hairy_hunter","Стая Морозной Луны","Ущелье Фростмуна","Ты находишь убежище стаи и знакомишься с её обычаями. Охотники не нападают без причины.","","",15,""),
        new Task("island_frossel","Добыча из Фросселя","Фроссель","След разбойника приводит в пещеру. Остатки его банды охраняют спрятанную добычу.","Разбойник Солстейма","",100,""),
        new Task("island_torkild","Родственные связи","Деревня скаалов","Ты передаёшь Вульфу Дикой Крови последние вести о Торкильде. Он благодарит за то, что узнал судьбу брата.","","Письмо Торкильда",75,""),
        new Task("island_torkild_dead","Последнее письмо Торкильда","Деревня скаалов","Вульф получает письмо брата и прощается с ним. Ему больше не нужно ждать возвращения Торкильда.","","Письмо Торкильда",75,""),
        new Task("letter_museum","Приглашение Сила Весула","Данстар","Ты находишь музей и передаёшь приглашение. Сил рассказывает о Мифическом Рассвете и частях бритвы Мерунеса.","","Приглашение в музей Данстара",15,""),
        new Task("letter_brotherhood","Чёрная ладонь","Морфал","Записка и слухи приводят к следу Тёмного Братства. Продолжение доступно в сюжетной линии Братства.","","Таинственная записка Братства",10,""),
        new Task("letter_septimus","Вести от Септимия","Винтерхолд","Ты доставляешь письмо и уточняешь путь к исследователю. Дальнейшее исследование связано с основной историей.","","Письмо Септимия",15,""),
        new Task("hakar","Тайник Хакара","Вайтран","В условленном месте находишь оставленные Стражей Рассвета припасы.","","Распоряжения Хакара",30,"Припасы Стражи Рассвета"),
        new Task("saliah","Северный тайник Салии","Данстар","Сведения позволяют найти скрытую кладовую Стражи Рассвета.","","Распоряжения Салии",30,"Припасы Стражи Рассвета"),
        new Task("lynoit","Западный тайник Линоита","Маркарт","Ты обнаруживаешь скрытую кладовую по распоряжениям погибшего дозорного.","","Распоряжения Линоита",30,"Припасы Стражи Рассвета"),
        new Task("dead_pendant","Возвращение кулона","Вайтран","Ты передаёшь найденный кулон местному караульному, чтобы вещь могла вернуться близким погибшей.","","Кулон погибшей путницы",45,""),
        new Task("island_blackbook","След Чёрной книги","Воронья Скала","В Вороньей Скале ты расспрашиваешь местных исследователей о записи безумца. Полученные сведения сохраняются для будущих поисков.","","Сведения о Чёрной книге",20,""),
        new Task("boethiah","Книга культиста","Святилище Боэтии","Ты находишь святилище и узнаёшь о культе Боэтии. Книга помогла найти место; испытание самой принцессы ещё впереди.","","Испытание Боэтии",15,""),
        new Task("dwemer_children","Переправа Глубинного Народа","Переправа Глубинного Народа","По подсказке детей ты обнаруживаешь древнюю переправу и осматриваешь двемерские развалины.","","",25,""),
        new Task("wolf_escape","След бойцовых волков","Пещера Крегслейн","След приводит к месту незаконных боёв. Оставшийся у входа бандит не хочет пропускать свидетеля.","Бандит Крегслейна","",65,""),
        new Task("headless","Привал Хамвира","Привал Хамвира","Призрачный всадник исчезает у старых могил. Ты осматриваешь захоронения и находишь забытый тайник.","","",45,"")
    };
    static Task find(String id){for(Task t:TASKS)if(t.id.equals(id))return t;return null;}
    static String title(String key){for(Task t:TASKS)if(t.key().equals(key))return t.title;return key;}
    static boolean active(GameEngine g,Task t){return "ACTIVE".equals(g.progress.questOutcome(t.key()));}
    static boolean here(GameEngine g,Task t){return t.place.equalsIgnoreCase(g.progress.location);}
    static List<Task> visible(GameEngine g){ArrayList<Task> out=new ArrayList<>();for(Task t:TASKS)if(g.progress.questOutcome(t.key())!=null)out.add(t);return out;}
    static void offer(GameEngine g,String id){Task t=find(id);if(t!=null)g.progress.setQuestOutcome(t.key(),"ACTIVE");}
    static void choices(GameEngine g,GameEngine.Scene s){for(Task t:TASKS)if(active(g,t)&&here(g,t))s.add("ROADQUEST:"+t.id,t.title,t.body);}
    static void choose(GameEngine g,String action){
        if(g.road.pending()||!("WORLD".equals(g.mode)||"RESULT".equals(g.mode)))return;
        Task t=find(action.substring("ROADQUEST:".length()));if(t==null||!active(g,t)||!here(g,t))return;
        if(!t.required.isEmpty()&&g.progress.quantity(t.required)==0){g.message="Не хватает предмета: "+t.required;g.mode="RESULT";return;}
        if(!t.enemy.isEmpty()&&!"COMPLETED".equals(g.progress.questOutcome(t.combatKey()))){g.beginRoadQuestBattle(t.id,t.enemy,t.body);return;}
        if(!t.item.isEmpty()&&!g.progress.addItem(t.item,1)){g.message="Освободи место в сумке для награды.";g.mode="RESULT";return;}
        // Evidence is retained in the inventory for the journal; completed state prevents repeated awards.
        g.gold=GameEngine.nonnegativeAdd(g.gold,t.gold);g.addXp(25);g.progress.setQuestOutcome(t.key(),"COMPLETED");
        g.message=t.body+"\n\nЗадание завершено. +"+t.gold+" септимов, +25 опыта."+(t.item.isEmpty()?"":" Получено: "+t.item+".");
        g.mode="RESULT";g.progress.sync(g);g.recordVisualEvent(t.title+": завершено");
    }
    static void victory(GameEngine g,String battleKind){
        if(!battleKind.startsWith("ROAD_QUEST:"))return;
        Task t=find(battleKind.substring("ROAD_QUEST:".length()));if(t==null||!active(g,t))return;
        g.progress.setQuestOutcome(t.combatKey(),"COMPLETED");g.message+="\n\nОпасность устранена. Заверши поручение в этой локации через журнал или историю.";
    }
    static boolean item(GameEngine g,String name){if(g.progress.quantity(name)>0)return true;return g.progress.addItem(name,1);}
    static String evidence(GameEngine g,String name){return item(g,name)?" Получено: "+name+".":" Не хватает места для предмета: "+name+".";}
    static String evidenceFor(String id){switch(id){
        case "treasure":return "Записка искателя сокровищ";
        case "hakar":return "Распоряжения Хакара";
        case "saliah":return "Распоряжения Салии";
        case "lynoit":return "Распоряжения Линоита";
        case "island_torkild":case "island_torkild_dead":return "Письмо Торкильда";
        case "letter_museum":return "Приглашение в музей Данстара";
        case "letter_brotherhood":return "Таинственная записка Братства";
        case "letter_septimus":return "Письмо Септимия";
        case "island_blackbook":return "Сведения о Чёрной книге";
        case "boethiah":return "Испытание Боэтии";
        case "dead_pendant":return "Кулон погибшей путницы";
        case "mistwatch":return "Письмо о пленных";
        default:return "";
    }}
    static String lead(GameEngine g,String id){
        String name=evidenceFor(id);
        if(!name.isEmpty()&&!item(g,name))return "Не хватает места для "+name+".";
        offer(g,id);Task t=find(id);return (name.isEmpty()?"":"Получено: "+name+". ")+(t==null?"":"Цель в журнале: "+t.place+".");
    }
}
