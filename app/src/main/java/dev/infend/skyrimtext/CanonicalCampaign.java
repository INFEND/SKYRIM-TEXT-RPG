package dev.infend.skyrimtext;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Stateful main-quest adaptation. The optional locally bundled V7 pack supplies exact dialogue. */
public final class CanonicalCampaign {
    private CanonicalCampaign(){}
    private static volatile StoryDialoguePack original;
    public static final int COUNT=33;
    private static final int PAGE=8;
    private static final class Stage {
        final String title,place,goal,intro,speaker,dialogue,primary,detail,alternate,altEffect,enemy;
        final int region,hp,damage,split;
        Stage(String title,String place,int region,String goal,String intro,String speaker,String dialogue,
              String primary,String detail,String alternate,String altEffect,String enemy,int hp,int damage,int split){
            this.title=title;this.place=place;this.region=region;this.goal=goal;this.intro=intro;
            this.speaker=speaker;this.dialogue=dialogue;this.primary=primary;this.detail=detail;
            this.alternate=alternate;this.altEffect=altEffect;this.enemy=enemy;this.hp=hp;this.damage=damage;this.split=split;
        }
    }
    private static final Stage[] STAGES={
        new Stage("На свободу","Хелген",5,"Выжить при казни","Повозка останавливается у крепости. Пленников выводят к плахе; над башнями нарастает гул крыльев.","Ралоф","Не спускай глаз с дороги. Судьба ещё может дать нам шанс.","Броситься к башне","Укрыться от огня","Спасти другого пленника","Помощь пленнику приносит доверие, но отнимает силы.","",0,0,0),
        new Stage("Побег из Хелгена","Хелген",5,"Выбраться из крепости","Камни рушатся, подземелье наполнено дымом. Ралоф ведёт тебя к выходу, но в пещере слышно дыхание медведицы.","Ралоф","Выход близко. Не поднимай шума, если можно пройти мимо зверя.","Вступить в бой","Пробиться через пещеру","Прокрасться мимо медведицы","Удачная скрытность позволяет избежать боя.","Медведица",48,7,131),
        new Stage("Перед бурей","Ривервуд",0,"Предупредить Гердур и ярла","На лесопилке кипит работа. Гердур выслушивает рассказ о драконе и тревожится за деревню.","Гердур","Передай ярлу, что деревне нужна защита. Одни мы не справимся.","Отправиться в Вайтран","Доставить предупреждение","Помочь укрепить переправу","Жители дают тебе припасы за помощь.","",0,0,0),
        new Stage("Вайтран","Драконий Предел",0,"Доложить Балгруфу о Хелгене","У ворот стража проверяет путников. В дворце ярл обсуждает безопасность своих владений.","Ярл Балгруф","Расскажи всё, что видел. Если угроза реальна, я пошлю людей на юг.","Согласиться помочь магу","Получить новое поручение","Убедить ярла выделить больше охраны","Твои слова укрепляют доверие двора.","",0,0,0),
        new Stage("Ветреный пик","Ветреный пик",0,"Добыть Драконий камень","Фаренгар отправляет тебя в древний храм. За ловушками и криптами ждёт последний хранитель скрижали.","Фаренгар","На камне обозначены древние захоронения. Принеси его, если сумеешь пройти руины.","Сразиться с хранителем","Забрать скрижаль после победы","Изучить ловушки и руны","Подготовка облегчает предстоящий бой.","Драугр-повелитель",90,10,263),
        new Stage("Дракон в небе","Западная сторожевая башня",0,"Победить Мирмулнира","Айрилет собирает стражу. У обгоревшей башни выжившие указывают в небо: дракон возвращается.","Айрилет","Держись каменных укрытий и бей, когда он опустится на землю.","Атаковать дракона","Защитить Вайтран","Организовать засаду со стражей","Стражники отвлекают дракона и ослабляют его.","Мирмулнир",145,12,341),
        new Stage("Возвращение к ярлу","Драконий Предел",0,"Узнать о призыве Седобородых","После битвы над равниной разносится голос с гор. Балгруф собирает советников и признаёт твою заслугу.","Ярл Балгруф","С гор тебя зовут те, кто знает силу Голоса. Пора узнать, что означает этот дар.","Принять титул и отправиться к Седобородым","Начать путь к Хротгару","Расспросить советников о Голосе","Узнаёшь больше о древней традиции.","",0,0,0),
        new Stage("Путь Голоса","Высокий Хротгар",0,"Пройти обучение Седобородых","Ты поднимаешься по заснеженным ступеням. Арнгейр встречает тебя в тишине монастыря.","Арнгейр","Покажи, что готов владеть Голосом осознанно. Сначала научись слушать.","Пройти испытание Голоса","Получить задание на поиск рога","Упражняться с мастерами","Тренировка укрепляет магические навыки.","",0,0,0),
        new Stage("Рог Юргена","Устенгрев",8,"Найти след рога и Дельфину","В гробнице рога нет: вместо него оставлена записка. След приводит тебя обратно в Ривервуд, к хозяйке таверны.","Дельфина","Мне важно убедиться, что ты действительно тот, кого ждут. Внизу мы поговорим без свидетелей.","Выслушать Дельфину","Узнать её замысел","Осмотреть гробницу и записку","Ты сохраняешь дополнительные сведения для журнала.","",0,0,0),
        new Stage("Клинок во тьме","Роща Кин",2,"Остановить Салокнира","Дельфина показывает древнее захоронение. Алдуин пробуждает погибшего дракона, и тот поднимается из земли.","Дельфина","Теперь мы увидим, как возвращаются драконы. Если победишь этого, я раскрою всё, что знаю.","Сразиться с Салокниром","Подтвердить силу Довакина","Выбрать укрытие и подготовиться","Подготовленная позиция уменьшает опасность.","Салокнир",165,13,637),
        new Stage("Дипломатическая неприкосновенность","Ривервуд",0,"Подготовить проникновение","Дельфина объясняет, как попасть на приём Эленвен. Малборн готов помочь пронести необходимое.","Дельфина","На приёме главное не привлекать внимания. Всё, что понадобится внутри, передай нашему помощнику заранее.","Встретиться с Малборном","Подготовиться к приёму","Продумать легенду гостя","Убедительная легенда помогает при проверке.","",0,0,0),
        new Stage("Талморское посольство","Талморское посольство",3,"Добыть документы и выбраться","В зале музыка и вино. Малборн ждёт, пока гости отвлекутся, а за кухней начинаются закрытые помещения.","Малборн","Мне удалось подготовить проход. Теперь нужен повод отвлечь охрану.","Пробиться к документам","Добыть досье и выйти","Отвлечь гостей и пройти скрытно","Успешная скрытность позволяет избежать открытого боя.","Талморский стражник",96,10,908),
        new Stage("Крыса, загнанная в угол","Рифтен",1,"Выяснить, где скрывается Эсберн","Досье ведёт к старому Клинку. На рынке Рифтена Бриньольф может подсказать дорогу в подземелья.","Бриньольф","Здесь сведения редко достаются просто так. Услуга за услугу — и, возможно, найдём твоего старика.","Узнать путь в Крысиную нору","Найти убежище Эсберна","Поговорить с местными без кражи","Ты ищешь другой способ получить сведения.","",0,0,0),
        new Stage("Эсберн","Крысиная нора",1,"Спасти Эсберна","За укреплённой дверью слышен голос старика. Талморские агенты уже идут по следу Клинка.","Эсберн","Если Дельфина ещё жива, у нас есть надежда. Но отсюда надо уходить немедленно.","Защитить Эсберна","Вывести Клинка из подземелья","Проверить безопасный путь отхода","Подготовка уменьшает угрозу преследователей.","Талморский агент",110,11,1117),
        new Stage("Стена Алдуина","Ривервуд",0,"Объединить Клинков","В тайной комнате Дельфина и Эсберн встречаются после долгих лет. Старик раскрывает местонахождение древнего храма.","Эсберн","Стена может хранить знание о поражении Алдуина. Нам нужно увидеть её своими глазами.","Отправиться к храму","Начать экспедицию","Договориться о раздельном маршруте","Раздельный путь уменьшает внимание к Клинкам.","",0,0,0),
        new Stage("Храм Небесной гавани","Храм Небесной гавани",4,"Открыть храм и изучить Стену","Акавирские механизмы закрывают проход. Эсберн замечает знаки, связанные с Драконорождённым.","Эсберн","Камни рассказывают о древней войне. Ищи знак героя и помни о печати крови.","Разгадать механизм и открыть печать","Узнать о Драконобое","Изучить символы прежде, чем идти","Снижает риск ловушек и даёт опыт исследования.","",0,0,0),
        new Stage("Возвращение рога","Высокий Хротгар",0,"Завершить испытание и открыть путь к вершине","Седобородые принимают возвращённый рог. Арнгейр объясняет, почему запретный Крик не входит в их учение.","Арнгейр","Я не владею этим Криком. Ответы ищи у того, кто живёт выше нас. Голос откроет дорогу.","Освоить Чистое небо","Подняться к Партурнаксу","Обсудить цену запретного знания","Твои вопросы открывают дополнительные сведения.","",0,0,0),
        new Stage("Глотка Мира","Глотка Мира",0,"Узнать тайну Древнего свитка","На вершине из облаков выходит Партурнакс. Он рассказывает о разрыве времени, оставшемся после древней войны.","Партурнакс","Время здесь помнит битву. Чтобы увидеть её, смертному понадобится предмет, который переживает века.","Согласиться найти свиток","Начать поиски","Расспросить о природе драконов","Ты лучше понимаешь противника.","",0,0,0),
        new Stage("Древнее знание","Высокий Хротгар",0,"Узнать, где искать свиток","Арнгейр признаёт, что знания о свитках лучше искать среди магов Винтерхолда.","Арнгейр","Коллегия хранит книги, недоступные нам. Поговори с её учёными.","Отправиться в Коллегию","Искать сведения у магов","Подготовить дорожные припасы","Пополняешь запас сил перед долгой дорогой.","",0,0,0),
        new Stage("Коллегия Винтерхолда","Коллегия Винтерхолда",6,"Получить сведения о Септимии","Фаральда охраняет мост, а Ураг гро-Шуб распоряжается Арканеумом. В книгах находится след исследователя.","Ураг гро-Шуб","Тебе нужен не просто свиток, а тот, кто изучал их природу. Попробуй отыскать Септимия.","Пройти к Арканеуму","Получить направление на север","Изучить трактаты о свитках","Получаешь опыт магического исследования.","",0,0,0),
        new Stage("Чёрный Предел","Чёрный Предел",6,"Получить Древний свиток в башне Мзарк","Септимий отдаёт ключ к двемерским глубинам. В Альфтанде и Чёрном Пределе ждут механизмы и опасные стражи.","Септимий Сегоний","Под землёй сохранилась машина, способная извлечь знание из свитка. Отыщи её.","Преодолеть двемерскую охрану","Настроить механизм Мзарк","Изучить резонатор и обходы","Подготовка уменьшает угрозу двемерского стража.","Двемерский центурион",180,14,1739),
        new Stage("Проклятие Алдуина","Глотка Мира",0,"Изучить Драконобой и отразить Алдуина","Древний свиток открывает видение прошлого. Ты наблюдаешь, как герои изгоняют Алдуина, затем дракон появляется над вершиной.","Партурнакс","Смотри внимательно. Знание древних героев может заставить его опуститься на землю.","Применить Драконобой","Победить Алдуина на вершине","Подготовить Голос и укрытие","Снижает силу атаки Алдуина.","Алдуин",210,15,1792),
        new Stage("Падший","Драконий Предел",0,"Получить согласие Балгруфа на ловушку","Побеждённый дракон улетел. Балгруф готов помочь, но гражданская война ставит Вайтран под угрозу.","Ярл Балгруф","Я не могу открыть дворец для дракона, пока город рискует стать добычей армий. Добейся передышки.","Предложить переговоры","Попросить Седобородых о совете","Предложить защиту города","Укрепляет доверие ярла.","",0,0,0),
        new Stage("Бесконечная пора","Высокий Хротгар",0,"Созвать Туллия и Ульфрика","Ты убеждаешь Седобородых открыть монастырь для переговоров, затем обращаешься к обеим сторонам.","Арнгейр","Если военачальники согласятся услышать друг друга, мы примем их здесь.","Пригласить обе стороны","Созвать мирный совет","Объяснить общую угрозу обеим армиям","Опыт убеждения облегчает переговоры.","",0,0,0),
        new Stage("Мирный совет","Высокий Хротгар",0,"Заключить временное перемирие","Вокруг стола спорят Ульфрик, Туллий, ярлы и их советники. Эсберн напоминает о цене затянувшейся войны.","Эсберн","Пока вы спорите о земле, Алдуин становится сильнее. Сейчас всем необходима передышка.","Предложить равный обмен","Заключить перемирие","Поддержать Империю в споре","Выбор меняет отношение сторон и записывается в журнал.","",0,0,0),
        new Stage("Призыв Одавинга","Драконий Предел",0,"Узнать имя дракона и подготовить ловушку","Клинки открывают имя Одавинга и требуют решения о Партурнаксе. Ты готовишься использовать новый Крик.","Эсберн","Имя дракона даст тебе возможность позвать его. Но прежде реши, как относишься к его сородичу на горе.","Пощадить Партурнакса","Открыть путь к ловушке","Отказаться решать судьбу Партурнакса сейчас","Твоё отношение к Клинкам меняется, но поиски продолжаются.","",0,0,0),
        new Stage("Ловушка для Одавинга","Драконий Предел",0,"Поймать дракона и узнать путь к Алдуину","Цепи в галерее приготовлены. Одавинг откликается на вызов, и его нужно заманить под механизм.","Ярл Балгруф","Мои люди готовы. Заставь дракона подойти туда, где мы сможем сомкнуть ловушку.","Заманить Одавинга","Обездвижить его и договориться","Подготовить цепи и укрытия","Подготовка позволяет удержать дракона быстрее.","Одавинг",190,13,2281),
        new Stage("Дом Пожирателя Мира","Скулдафн",2,"Добраться до портала в Совнгард","Одавинг приносит тебя к храму. За стенами Скулдафна ждут драугры и жрец, охраняющий портал.","Одавинг","Дальше путь лежит через храм. Я не смогу сопровождать тебя за его пределами.","Победить Накрина","Открыть портал","Изучить храмовые ловушки","Разведка ослабляет охранника портала.","Накрин",200,14,2383),
        new Stage("Совнгард","Совнгард",0,"Провести заблудившуюся душу к Залу","Туман скрывает тропу. Погибший воин просит помочь найти Зал доблести, пока Алдуин охотится за душами.","Погибший воин","Я потерял дорогу в этой мгле. Если видишь путь, позволь идти рядом.","Проводить душу","Добраться до моста","Разведать дорогу сквозь туман","Снижается риск потери сил.","",0,0,0),
        new Stage("Зал доблести","Зал доблести",0,"Пройти испытание Тсуна","На мосту стоит Тсун. Он требует доказательства, что живой герой достоин войти в чертог Шора.","Тсун","Твой путь необычен. Докажи в бою, что достоин перейти этот мост.","Принять испытание","Завоевать право войти","Подготовиться к поединку","Снижается опасность испытания.","Тсун",185,13,2442),
        new Stage("Драконоборец","Совнгард",0,"Развеять туман и победить Алдуина","Гормлейт, Хакон и Феллдир выходят на бой вместе с тобой. Ваши Голоса разрывают туман.","Гормлейт Золотая Рукоять","Сейчас мы сражаемся вместе. Когда туман рассеется, не дай ему снова подняться.","Использовать Драконобой и атаковать","Завершить битву","Объединить Голоса древних героев","Союзники ослабляют Алдуина перед схваткой.","Алдуин",260,16,2482),
        new Stage("Возвращение в Нирн","Совнгард",0,"Получить благословение Тсуна","Алдуин повержен. В Зале доблести славят победу, но твоя судьба по-прежнему связана с миром живых.","Тсун","Твоя победа останется в песнях. Вернись к живым и призывай героев, когда они понадобятся.","Вернуться в Нирн","Получить Зов доблести","Поблагодарить древних героев","Сохраняешь память о союзе в журнале.","",0,0,0),
        new Stage("Эпилог","Глотка Мира",0,"Поговорить с Партурнаксом и завершить историю","Над вершиной кружат драконы. Партурнакс размышляет о конце власти Алдуина и о будущем мира.","Партурнакс","Твоя победа изменила мир, но не избавила его от всех опасностей. Теперь выбирай собственный путь.","Завершить главную историю","Продолжить свободное путешествие","Обдумать судьбу драконов","Твой выбор остаётся частью летописи.","",0,0,0)
    };
    static void installOriginal(StoryDialoguePack pack){original=pack;}
    public static void loadLocal(Context context){
        // A private build can bundle the V7 file; existing private data is also reused.
        // Nothing is downloaded, and no import screen is required.
        for(int i=0;i<2;i++){
            try(java.io.InputStream in=i==0?context.getAssets().open("story_core_v7.json"):
                new java.io.FileInputStream(new java.io.File(context.getFilesDir(),"imported_story_v7.json"));
                java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
                byte[] buffer=new byte[16384];int n;
                while((n=in.read(buffer))!=-1){
                    if(out.size()+n>StoryDialoguePack.MAX_BYTES)throw new java.io.IOException("Story too large");
                    out.write(buffer,0,n);
                }
                String raw=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
                if(raw.startsWith("\uFEFF"))raw=raw.substring(1);
                StoryDialoguePack p=StoryDialoguePack.parse(raw);
                if(p.chapters().size()==COUNT){installOriginal(p);return;}
            }catch(Exception ignored){}
        }
    }
    static boolean hasOriginal(){return original!=null;}
    private static Stage stage(int id){return STAGES[Math.max(0,Math.min(COUNT-1,id-1))];}
    public static String title(int id){return stage(id).title;}
    public static String objective(GameEngine g){
        if(g.canonicalStage==0)return "Начать главную историю";
        if(g.canonicalStage>COUNT)return "Алдуин побеждён. Главная история завершена.";
        return g.canonicalStage+"/"+COUNT+" | "+stage(g.canonicalStage).goal;
    }
    private static String quest(int id){return "canonical_"+id;}
    private static String action(GameEngine g,String verb){return "CANON:"+g.canonicalStage+":"+g.canonicalPhase+":"+g.canonicalPage+":"+verb;}
    private static List<StoryDialoguePack.Line> lines(GameEngine g){
        ArrayList<StoryDialoguePack.Line> list=new ArrayList<>();
        StoryDialoguePack p=original;
        if(p==null)return list;
        Stage s=stage(g.canonicalStage);
        StoryDialoguePack.Chapter c=p.chapter(g.canonicalStage);
        if(c.id!=g.canonicalStage)return list;
        for(StoryDialoguePack.Line l:c.lines()){
            boolean post=g.canonicalStage==25?l.sourceLine>=2094:
                !s.enemy.isEmpty()&&s.split>0&&l.sourceLine>=s.split;
            // The recording contains a single negotiated outcome. Do not put
            // that branch's words in an alternative outcome's NPC mouths.
            if(g.canonicalStage==25&&post&&g.canonicalTreaty!=1&&
                (l.sourceLine<2121||l.sourceLine>2151))continue;
            if(post==(g.canonicalPhase==1))list.add(l);
        }
        return list;
    }
    private static int lastPage(List<StoryDialoguePack.Line> list){return Math.max(0,(list.size()-1)/PAGE);}
    private static String dialogue(String speaker,String text){return "**"+speaker+":**\n**— "+text+"**";}
    public static GameEngine.Scene scene(GameEngine g){
        if(g.canonicalStage<1||g.canonicalStage>COUNT)
            return new GameEngine.Scene("ГЛАВНАЯ ИСТОРИЯ","Путь Довакина завершён",objective(g))
                .primary("GO_WORLD","Свободное путешествие","Вернуться в открытый мир");
        Stage s=stage(g.canonicalStage);
        List<StoryDialoguePack.Line> lines=lines(g);int page=Math.min(g.canonicalPage,lastPage(lines));
        StringBuilder body=new StringBuilder(g.canonicalPhase==0?s.intro:
            g.canonicalStage==25?"Стороны выслушивают предложенные условия и возвращаются к переговорам.":
            "Опасность позади. "+s.detail+".");
        if(!g.canonicalRecent.isEmpty())body.append("\n\n").append(g.canonicalRecent);
        if(lines.isEmpty()&&g.canonicalPhase==0)body.append("\n\n").append(dialogue(s.speaker,s.dialogue));
        for(int i=page*PAGE;i<Math.min(lines.size(),(page+1)*PAGE);i++){
            StoryDialoguePack.Line l=lines.get(i);
            body.append("\n\n").append(dialogue(l.speaker,l.text));
            if(l.uncertain)body.append("\n(Частично восстановлено.)");
        }
        body.append("\n\nЦель: ").append(s.goal);
        GameEngine.Scene scene=new GameEngine.Scene("ГЛАВНАЯ ИСТОРИЯ | "+g.canonicalStage+"/"+COUNT,s.title,body.toString());
        if(page<lastPage(lines)){
            scene.primary(action(g,"NEXT"),"Ответить и продолжить разговор","Следующая часть текущей сцены");
            return scene;
        }
        if(g.canonicalPhase==1)return scene.primary(action(g,"ADVANCE"),g.canonicalStage==25?"Подписать перемирие":s.detail,"Завершить текущий этап задания");
        String primary=s.primary;
        if(g.canonicalStage==25)primary="Предложить обмен Маркарта на Рифтен";
        if(g.canonicalStage==26&&g.canonicalPaarthurnax!=0)primary="Продолжить подготовку ловушки";
        scene.primary(action(g,"MAIN"),primary,s.detail);
        if(!g.canonicalPrepared){
            if(g.canonicalStage==25){
                scene.add(action(g,"ALT"),"Потребовать дополнительных гарантий для Империи","Укрепляет отношения с Империей");
                scene.add(action(g,"STORM"),"Потребовать дополнительных гарантий для Братьев Бури","Укрепляет отношения с Братьями Бури");
            }else scene.add(action(g,"ALT"),s.alternate,s.altEffect);
        }
        if(g.hp<g.maxHp||g.stamina<g.maxStamina||g.magicka<g.maxMagicka)
            scene.add(action(g,"REST"),"Передохнуть","Один час отдыха восстанавливает часть сил");
        scene.add(action(g,"LEAVE"),"Отложить поручение","Вернуться к свободному путешествию; этап сохранится");
        return scene;
    }
    public static void open(GameEngine g){
        if(!g.started||"COMBAT".equals(g.mode))return;
        if(g.canonicalStage==0){g.canonicalStage=1;enter(g,true);}
        else if(g.canonicalStage<=COUNT){
            Stage s=stage(g.canonicalStage);
            // Returning from free exploration requires a new journey, not an invisible teleport.
            if(!s.place.equals(g.progress.location))move(g,s);
            g.mode="CANON";
        }else{g.mode="WORLD";g.message=objective(g);}
    }
    private static void move(GameEngine g,Stage s){
        if(!s.place.equals(g.progress.location)){
            g.day+=g.region==s.region?0:1;
            int total=g.hour+(g.region==s.region?2:5);
            g.day+=total/24;g.hour=total%24;
            g.stamina=Math.max(1,g.stamina-8);
            g.region=s.region;g.progress.location=s.place;
            g.visitedVisual.add(s.place);
            g.recordVisualEvent("Прибытие: "+s.place);
        }
    }
    private static void enter(GameEngine g,boolean first){
        Stage s=stage(g.canonicalStage);
        g.canonicalPage=0;g.canonicalPhase=0;g.canonicalPrepared=false;g.canonicalRecent="";
        if(first){g.region=s.region;g.progress.location=s.place;g.visitedVisual.add(s.place);}
        else move(g,s);
        g.progress.setQuestOutcome(quest(g.canonicalStage),"ACTIVE");
        g.mode="CANON";
    }
    private static void advance(GameEngine g){
        int id=g.canonicalStage;
        if(id<1||id>COUNT||"COMPLETED".equals(g.progress.questOutcome(quest(id))))return;
        g.progress.setQuestOutcome(quest(id),"COMPLETED");
        g.progress.completed.add("Сюжет: "+stage(id).title+" • день "+g.day);
        if(g.progress.completed.size()>80)g.progress.completed.remove(0);
        g.recordVisualEvent("Главная история: "+stage(id).title+" завершено");
        // Story flags are authoritative; an inventory limit can never block a quest.
        if(id==5&&g.progress.quantity("Драконий камень")==0)g.progress.addItem("Драконий камень",1);
        if(id==6)g.canonicalShouts|=1;
        if(id==7){g.reputation+=5;g.gold+=50;}
        if(id==8)g.canonicalShouts|=1;
        if(id==9)g.progress.addItem("Рог Юргена Призывателя Ветра",1);
        if(id==17)g.canonicalShouts|=2;
        if(id==21&&g.progress.quantity("Древний свиток")==0)g.progress.addItem("Древний свиток",1);
        if(id==22)g.canonicalShouts|=4;
        if(id==26)g.canonicalShouts|=8;
        if(id==32)g.canonicalShouts|=16;
        if(id==25&&g.canonicalTreaty==0)g.canonicalTreaty=1;
        if(id==26&&g.canonicalPaarthurnax==0)g.canonicalPaarthurnax=1;
        g.addXp(20+id);
        g.canonicalStage++;g.canonicalPage=0;g.canonicalPhase=0;g.canonicalPrepared=false;
        if(g.canonicalStage>COUNT){g.mode="WORLD";g.message=objective(g);g.reputation+=15;g.recordVisualEvent(g.message);}
        else enter(g,false);
        g.progress.sync(g);
    }
    public static void perform(GameEngine g,String value){
        if(!"CANON".equals(g.mode)||g.canonicalStage<1||g.canonicalStage>COUNT)return;
        String prefix="CANON:"+g.canonicalStage+":"+g.canonicalPhase+":"+g.canonicalPage+":";
        if(!value.startsWith(prefix))return;
        String verb=value.substring(prefix.length());Stage s=stage(g.canonicalStage);
        List<StoryDialoguePack.Line> list=lines(g);int last=lastPage(list);
        if("NEXT".equals(verb)){if(g.canonicalPage<last)g.canonicalPage++;return;}
        if(g.canonicalPage<last)return;
        if("LEAVE".equals(verb)){g.mode="WORLD";g.message="Поручение сохранено: "+s.goal;return;}
        if("REST".equals(verb)&&g.canonicalPhase==0){if(g.waitHours(1))g.canonicalRecent="Ты восстановил часть сил.";return;}
        if(g.canonicalPhase==1){if("ADVANCE".equals(verb))advance(g);return;}
        if(g.canonicalStage==25&&("MAIN".equals(verb)||"ALT".equals(verb)||"STORM".equals(verb))){
            if(g.canonicalTreaty==0){
                g.canonicalTreaty="ALT".equals(verb)?2:"STORM".equals(verb)?3:1;
                if(g.canonicalTreaty==2){g.holdReputation[3]=Math.min(100,g.holdReputation[3]+5);g.holdReputation[2]=Math.max(-100,g.holdReputation[2]-5);}
                if(g.canonicalTreaty==3){g.holdReputation[2]=Math.min(100,g.holdReputation[2]+5);g.holdReputation[3]=Math.max(-100,g.holdReputation[3]-5);}
            }
            g.canonicalPrepared=true;g.canonicalPhase=1;g.canonicalPage=0;
            g.canonicalRecent=g.canonicalTreaty==1?"Ты предлагаешь обмен владениями, чтобы добиться временного прекращения огня.":
                g.canonicalTreaty==2?"Ты настаиваешь на гарантиях безопасности имперских гарнизонов. Стороны обсуждают условия временной передышки.":
                "Ты настаиваешь на гарантиях безопасности владений Братьев Бури. Стороны обсуждают условия временной передышки.";
            g.recordVisualEvent(g.canonicalRecent);return;
        }
        if("ALT".equals(verb)&&!g.canonicalPrepared){
            g.canonicalPrepared=true;g.canonicalRecent=s.altEffect;
            int id=g.canonicalStage;
            if(id==26){g.canonicalPaarthurnax=2;}
            else if(id==2||id==12){
                int chance=g.progress.chance(g,2,9,20);
                if(new Random().nextInt(100)<chance){
                    g.progress.skillExperience(g,9,30);g.canonicalRecent="Ты успешно миновал опасность скрытно.";g.canonicalPhase=1;g.canonicalPage=0;
                }else{g.stamina=Math.max(0,g.stamina-10);g.canonicalRecent="Тебя заметили. Придётся готовиться к бою.";}
            }else if(id==3){if(g.progress.carryWeight()+1<=50)g.potions++;}
            else{g.progress.skillExperience(g,s.enemy.isEmpty()?12:9,20);g.stamina=Math.max(1,g.stamina-4);}
            g.recordVisualEvent("Сюжетное решение: "+s.alternate);
            g.progress.sync(g);return;
        }
        if(!"MAIN".equals(verb))return;
        if(!s.enemy.isEmpty()){
            if(g.canonicalStage==22)g.canonicalShouts|=4;
            int health=Math.max(35,s.hp+Math.min(60,g.level*3)-(g.canonicalPrepared?25:0));
            int damage=Math.max(3,s.damage+Math.min(5,g.level/4)-(g.canonicalPrepared?3:0));
            // The actual fight is handled by the existing turn-based combat engine.
            g.beginCanonicalBattle(s.enemy,health,damage,s.intro);
        }else{
            if(g.canonicalStage==26&&g.canonicalPaarthurnax==0)g.canonicalPaarthurnax=1;
            advance(g);
        }
    }
    public static int chance(GameEngine g,String action){
        if(g.canonicalStage!=2&&g.canonicalStage!=12)return -1;
        if(g.canonicalPhase!=0||g.canonicalPrepared||!action.equals(action(g,"ALT")))return -1;
        return g.progress.chance(g,2,9,20);
    }
    static void victory(GameEngine g){
        if(g.canonicalStage<1||g.canonicalStage>COUNT)return;
        if(g.canonicalPhase!=0)return;
        if(g.canonicalStage==5&&g.progress.quantity("Драконий камень")==0)g.progress.addItem("Драконий камень",1);
        if(g.canonicalStage==6)g.canonicalShouts|=1;
        if(g.canonicalStage==21&&g.progress.quantity("Древний свиток")==0)g.progress.addItem("Древний свиток",1);
        g.canonicalPhase=1;g.canonicalPage=0;g.canonicalRecent=g.message;g.mode="CANON";
    }
    public static String combatShout(GameEngine g){return (g.canonicalShouts&4)!=0&&"Алдуин".equals(g.enemyName)?"Драконобой":"Безжалостная сила";}
    public static String shouts(GameEngine g){
        StringBuilder b=new StringBuilder();
        String[] names={"Безжалостная сила","Чистое небо","Драконобой","Зов дракона","Зов доблести"};
        for(int i=0;i<names.length;i++)if((g.canonicalShouts&(1<<i))!=0){if(b.length()>0)b.append(", ");b.append(names[i]);}
        return b.length()==0?"Пока не изучены":b.toString();
    }
}
