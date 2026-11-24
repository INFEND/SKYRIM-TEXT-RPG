package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import java.io.InputStream;
import java.util.Locale;

/** Compact offline atlas of the 283 user-approved action icons. */
public final class ActionChoiceIcons {
    private ActionChoiceIcons(){}
    public static final int TOTAL=283, CELL=96, COLS=17, ROWS=17;
    public static final String ASSET="icons/action_choices_283.webp";
    private static Bitmap atlas;
    private static final String[] LABELS={
        "Поговорить",
        "Начать диалог",
        "Поприветствовать",
        "Попрощаться",
        "Задать вопрос",
        "Ответить",
        "Выслушать",
        "Узнать слухи",
        "Уточнить",
        "Рассказать",
        "Объяснить",
        "Убедить",
        "Уговорить",
        "Запугать",
        "Угрожать",
        "Обмануть",
        "Признаться",
        "Извиниться",
        "Поблагодарить",
        "Похвалить",
        "Оскорбить",
        "Пошутить",
        "Попросить помощи",
        "Предложить помощь",
        "Договориться",
        "Прервать разговор",
        "Вступить в бой",
        "Атаковать",
        "Сильная атака",
        "Быстрая атака",
        "Ударить",
        "Ударить щитом",
        "Блокировать",
        "Парировать",
        "Уклониться",
        "Контратаковать",
        "Выстрелить из лука",
        "Выстрелить из арбалета",
        "Прицелиться",
        "Метнуть предмет",
        "Атаковать скрытно",
        "Обезоружить",
        "Сменить оружие",
        "Достать оружие",
        "Убрать оружие",
        "Добить противника",
        "Защитить союзника",
        "Отступить",
        "Сбежать",
        "Сдаться",
        "Бросить вызов",
        "Осмотреть поле боя",
        "Применить заклинание",
        "Применить магию огня",
        "Применить магию мороза",
        "Применить магию молнии",
        "Восстановить здоровье магией",
        "Призвать существо",
        "Воскресить мёртвого",
        "Применить иллюзию",
        "Стать невидимым",
        "Обнаружить жизнь",
        "Обнаружить мёртвых",
        "Применить паралич",
        "Наложить проклятие",
        "Снять проклятие",
        "Создать магический щит",
        "Поглотить магию",
        "Отразить магию",
        "Применить телекинез",
        "Применить свет",
        "Применить тьму",
        "Применить драконий крик",
        "Изучить слово силы",
        "Поглотить душу дракона",
        "Применить расовую способность",
        "Превратиться в оборотня",
        "Превратиться в вампира-лорда",
        "Вернуть обычный облик",
        "Активировать артефакт",
        "Благословить",
        "Рассеять магию",
        "Одноручное оружие",
        "Двуручное оружие",
        "Стрельба",
        "Блокирование",
        "Кузнечное дело",
        "Тяжёлая броня",
        "Лёгкая броня",
        "Скрытность",
        "Взлом",
        "Карманные кражи",
        "Красноречие",
        "Алхимия",
        "Зачарование",
        "Разрушение",
        "Восстановление",
        "Изменение",
        "Иллюзия",
        "Колдовство",
        "Идти вперёд",
        "Вернуться назад",
        "Повернуть налево",
        "Повернуть направо",
        "Идти пешком",
        "Бежать",
        "Красться",
        "Прыгнуть",
        "Взобраться",
        "Спуститься",
        "Переплыть",
        "Войти",
        "Выйти",
        "Войти в пещеру",
        "Войти в здание",
        "Посетить город",
        "Посетить деревню",
        "Идти в лес",
        "Идти по дороге",
        "Пересечь мост",
        "Осмотреть окрестности",
        "Исследовать локацию",
        "Искать путь",
        "Следовать по следам",
        "Открыть карту",
        "Отметить место",
        "Быстрое перемещение",
        "Подождать в пути",
        "Осмотреть предмет",
        "Взять",
        "Поднять",
        "Подобрать всё",
        "Положить",
        "Выбросить",
        "Использовать",
        "Экипировать",
        "Снять экипировку",
        "Открыть контейнер",
        "Закрыть контейнер",
        "Обыскать контейнер",
        "Обыскать тело",
        "Прочитать книгу",
        "Прочитать записку",
        "Прочитать свиток",
        "Написать записку",
        "Активировать рычаг",
        "Нажать кнопку",
        "Зажечь факел",
        "Потушить факел",
        "Осмотреть механизм",
        "Разрушить предмет",
        "Передвинуть предмет",
        "Сложить предметы",
        "Разделить стопку",
        "Отметить избранным",
        "Спрятаться",
        "Подкрасться",
        "Следить",
        "Подслушать",
        "Украсть предмет",
        "Обокрасть карман",
        "Взломать замок",
        "Обезвредить ловушку",
        "Установить ловушку",
        "Отвлечь внимание",
        "Скрыть следы",
        "Спрятать тело",
        "Сбежать от стражи",
        "Заплатить штраф",
        "Сдаться страже",
        "Ковать оружие",
        "Ковать броню",
        "Улучшить оружие",
        "Улучшить броню",
        "Расплавить руду",
        "Изготовить слиток",
        "Добыть руду",
        "Рубить дрова",
        "Собрать растения",
        "Собрать грибы",
        "Собрать ингредиенты",
        "Охотиться",
        "Ловить рыбу",
        "Снять шкуру",
        "Выделать кожу",
        "Приготовить еду",
        "Сварить зелье",
        "Создать яд",
        "Зачаровать предмет",
        "Снять зачарование",
        "Построить",
        "Отремонтировать",
        "Разобрать на материалы",
        "Купить",
        "Продать",
        "Торговаться",
        "Обменять",
        "Передать золото",
        "Получить оплату",
        "Проверить кошелёк",
        "Оценить предмет",
        "Взять кредит",
        "Вернуть долг",
        "Вложить золото",
        "Открыть лавку",
        "Выпить зелье",
        "Использовать яд",
        "Перевязать рану",
        "Вылечить болезнь",
        "Вылечить отравление",
        "Поесть",
        "Попить",
        "Отдохнуть",
        "Поспать",
        "Разбить лагерь",
        "Развести костёр",
        "Согреться",
        "Охладиться",
        "Проверить здоровье",
        "Восстановить запас сил",
        "Восстановить магию",
        "Взять задание",
        "Принять задание",
        "Отказаться от задания",
        "Продолжить задание",
        "Завершить задание",
        "Сдать задание",
        "Получить награду",
        "Проверить цели",
        "Открыть журнал",
        "Исследовать улику",
        "Найти персонажа",
        "Найти предмет",
        "Поговорить с целью",
        "Передать сообщение",
        "Доставить предмет",
        "Сделать выбор",
        "Выбрать сторону",
        "Раскрыть тайну",
        "Начать сюжетное событие",
        "Посмотреть характеристики",
        "Повысить уровень",
        "Распределить очки",
        "Изучить способность",
        "Выбрать перк",
        "Тренировать навык",
        "Обучиться у наставника",
        "Изменить экипировку",
        "Изучить талант",
        "Активировать камень-хранитель",
        "Выбрать созвездие",
        "Проверить активные эффекты",
        "Нанять спутника",
        "Пригласить спутника",
        "Отпустить спутника",
        "Приказать следовать",
        "Приказать ждать",
        "Приказать атаковать",
        "Приказать защищать",
        "Попросить нести вещи",
        "Обменяться вещами",
        "Подарить подарок",
        "Повысить доверие",
        "Помириться",
        "Подружиться",
        "Флиртовать",
        "Предложить брак",
        "Разорвать отношения",
        "Подтвердить",
        "Отменить",
        "Продолжить",
        "Назад",
        "Пропустить",
        "Подождать",
        "Остановиться",
        "Повторить",
        "Выбрать другое действие",
        "Ввести своё действие",
        "Осмотреться",
        "Открыть настройки",
        "Сохранить игру",
        "Загрузить игру",
        "Справка"
    };
    private static String normalize(String text){
        return text==null?"":text.toLowerCase(Locale.ROOT).replace('ё','е').replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
    }
    private static boolean starts(String text,String... terms){
        for(String term:terms)if(text.startsWith(term))return true;
        return false;
    }
    /** One-based atlas index; zero means use the existing skill glyph. */
    public static int resolve(String action,String title,String skill){
        String a=action==null?"":action.toUpperCase(Locale.ROOT);
        String t=normalize(title);
        switch(a){
            case "ATTACK":return 28;
            case "HEAVY":return 29;
            case "FLEE":return 48;
            case "BLOCK":return 33;
        }
        // Prefer a specific full phrase over a generic verb, e.g. open map vs open container.
        int best=0,length=0;
        for(int i=0;i<LABELS.length;i++){
            String label=normalize(LABELS[i]);
            if((t.equals(label)||t.startsWith(label+" "))&&label.length()>length){best=i+1;length=label.length();}
        }
        if(best!=0)return best;
        if(a.startsWith("REGION:"))return 127;
        switch(a){
            case "TALK":return 1;
            case "TALK_HELP":return 8;
            case "TALK_GIVE":return 198;
            case "REST":return 213;
            case "EVENT_HELP":return 24;
            case "EVENT_SEARCH":return 121;
            case "EVENT_LEAVE":return 105;
        }
        if(starts(t,"расспрос","спрос","поинтерес"))return 5;
        if(starts(t,"побесед","заговор","обратиться"))return 1;
        if(starts(t,"убед","уговор"))return 12;
        if(starts(t,"пригроз","запуг"))return 14;
        if(starts(t,"помочь","оказать помощь"))return 24;
        if(starts(t,"обыск"))return 140;
        if(starts(t,"осмотр","огляд"))return 279;
        if(starts(t,"исслед","обслед"))return 122;
        if(starts(t,"направиться","отправиться","пойти","пройти","двинуться","подойти"))return 105;
        if(starts(t,"вернуться","возвратиться"))return 102;
        if(starts(t,"напасть","атак","удар"))return 28;
        if(starts(t,"выстрел"))return 37;
        if(starts(t,"сбежать","убежать","бежать прочь"))return 49;
        if(starts(t,"уклон"))return 35;
        if(starts(t,"спрят","затаиться"))return 156;
        if(starts(t,"прокрасться","подкрасться"))return 157;
        if(starts(t,"вскрыть","взлом"))return 162;
        if(starts(t,"украсть","похитить"))return 160;
        if(starts(t,"купить","приобрести"))return 194;
        if(starts(t,"продать","сбыть"))return 195;
        if(starts(t,"торгов","торговать"))return 196;
        if(starts(t,"прочесть","читать"))return 142;
        if(starts(t,"отдох","передох"))return 213;
        if(starts(t,"переночевать","лечь спать"))return 214;
        if(starts(t,"ожидать","переждать"))return 274;
        if(starts(t,"вылеч","исцел"))return 209;
        if(starts(t,"колдовать","сотворить","наложить заклинание"))return 53;
        if(starts(t,"найти","разыскать","искать"))return 233;
        String s=normalize(skill);
        for(int i=82;i<100;i++)if(normalize(LABELS[i]).equals(s))return i+1;
        return 0;
    }
    private static synchronized Bitmap load(Context context){
        if(atlas!=null)return atlas;
        try(InputStream input=context.getAssets().open(ASSET)){
            Bitmap image=BitmapFactory.decodeStream(input);
            if(image!=null&&image.getWidth()==COLS*CELL&&image.getHeight()==ROWS*CELL)atlas=image;
        }catch(Exception ignored){}
        return atlas;
    }
    public static View view(Context context,int index,String fallback,int tint){
        Bitmap image=index>=1&&index<=TOTAL?load(context):null;
        if(image==null)return new SkyrimIconView(context,fallback,tint);
        final int cell=index-1;
        View icon=new View(context){
            private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            private final Rect src=new Rect(cell%COLS*CELL,cell/COLS*CELL,(cell%COLS+1)*CELL,(cell/COLS+1)*CELL);
            private final RectF dst=new RectF();
            @Override protected void onDraw(Canvas canvas){
                super.onDraw(canvas);
                float side=Math.min(getWidth(),getHeight())*.94f;
                dst.set((getWidth()-side)/2,(getHeight()-side)/2,(getWidth()+side)/2,(getHeight()+side)/2);
                canvas.drawBitmap(image,src,dst,paint);
            }
        };
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return icon;
    }
}
