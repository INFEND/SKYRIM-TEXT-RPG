package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The 48 original transparent icons supplied by the player.
 * One compact offline WebP atlas replaces all procedurally drawn placeholders.
 * Cell and label order exactly follows 01_... through 48_... in the ZIP.
 */
public final class SkyrimIconView extends View {
    public static final int COLUMNS=8, ROWS=6, CELL=128, TOTAL=48;
    public static final String ASSET="icons/skyrim_icons_48.webp";
    private static final String[] KEYS={
        "story","map","bag","codex","hero","journal","settings","dragon",
        "weapon","swords","daggers","axes","warhammers","bows","crossbows","armor",
        "helmets","cuirasses","gloves","boots","shields","clothing","jewelry","rings",
        "necklaces","amulets","circlets","potion","book","scrolls","septims","lockpicks",
        "tools","torches","materials","ingots","leather","dragonbones","dragonscales","valuables",
        "gems","ingredients","artifacts","food","drinks","customweapon","customitems","favorites"
    };
    private static final Map<String,Integer> INDEX=new HashMap<>();
    private static final Rect[] VISIBLE=new Rect[TOTAL];
    private static Bitmap atlas;
    private static boolean loadFailed;
    static{
        for(int i=0;i<KEYS.length;i++)INDEX.put(KEYS[i],i);
        INDEX.put("shield",20);INDEX.put("star",47);INDEX.put("satchel",2);
        INDEX.put("scroll",29);INDEX.put("book",28);INDEX.put("potion",27);
    }

    private final String glyph;
    private final int tint;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);

    public SkyrimIconView(Context context,String glyph,int tint){
        super(context);
        this.glyph=glyph==null?"favorites":glyph;
        this.tint=tint;
        setBackgroundColor(Color.TRANSPARENT);
        setContentDescription("Значок: "+this.glyph);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int size=(int)(30*getResources().getDisplayMetrics().density+0.5f);
        setMeasuredDimension(resolveSize(size,widthSpec),resolveSize(size,heightSpec));
    }
    public static int assetIndex(String key){
        Integer i=INDEX.get(key);
        return i==null?47:i;
    }

    private static synchronized Bitmap load(Context context){
        if(atlas!=null||loadFailed)return atlas;
        try(InputStream input=context.getAssets().open(ASSET)){
            Bitmap b=BitmapFactory.decodeStream(input);
            if(b==null||b.getWidth()!=COLUMNS*CELL||b.getHeight()!=ROWS*CELL)
                throw new IllegalStateException("Неверный атлас Skyrim");
            atlas=b;
        }catch(Exception ex){loadFailed=true;}
        return atlas;
    }

    /** Tight crop of the source's transparent border, cached once per icon. */
    private static synchronized Rect bounds(Bitmap image,int index){
        if(VISIBLE[index]!=null)return VISIBLE[index];
        int x=(index%COLUMNS)*CELL, y=(index/COLUMNS)*CELL;
        int[] pixels=new int[CELL*CELL];
        image.getPixels(pixels,0,CELL,x,y,CELL,CELL);
        int left=CELL,top=CELL,right=-1,bottom=-1;
        for(int py=0;py<CELL;py++)for(int px=0;px<CELL;px++){
            if((pixels[py*CELL+px]>>>24)<12)continue;
            if(px<left)left=px;if(px>right)right=px;
            if(py<top)top=py;if(py>bottom)bottom=py;
        }
        if(right<left||bottom<top){
            VISIBLE[index]=new Rect(x,y,x+CELL,y+CELL);
        }else{
            int border=Math.max(2,CELL/25);
            VISIBLE[index]=new Rect(x+Math.max(0,left-border),y+Math.max(0,top-border),
                x+Math.min(CELL,right+border+1),y+Math.min(CELL,bottom+border+1));
        }
        return VISIBLE[index];
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        // New 358-label atlas, indexed by the source's real icon bounds.
        if(SkyrimIconAtlas.draw(getContext(),canvas,glyph,tint,getWidth(),getHeight()))return;
        // Legacy 48-icon atlas remains as a safe fallback for old/unknown keys.
        Bitmap b=load(getContext());
        if(b==null||getWidth()==0||getHeight()==0)return;
        Rect crop=bounds(b,assetIndex(glyph));
        float margin=Math.min(getWidth(),getHeight())*.055f;
        float maxWidth=Math.max(1,getWidth()-2*margin);
        float maxHeight=Math.max(1,getHeight()-2*margin);
        float scale=Math.min(maxWidth/crop.width(),maxHeight/crop.height());
        float drawWidth=crop.width()*scale,drawHeight=crop.height()*scale;
        float centerX=getWidth()/2f,centerY=getHeight()/2f;
        RectF dest=new RectF(centerX-drawWidth/2,centerY-drawHeight/2,
            centerX+drawWidth/2,centerY+drawHeight/2);
        // Keep the artwork's original grey/silver gradients and transparent alpha.
        // Selected entries are brighter without flattening detail with a tint filter.
        int value=(Color.red(tint)+Color.green(tint)+Color.blue(tint))/3;
        paint.setAlpha(Math.max(155,Math.min(255,205+(value-125)/4)));
        canvas.drawBitmap(b,crop,dest,paint);
    }

    private static boolean has(String s,String... words){
        for(String word:words)if(s.contains(word))return true;
        return false;
    }

    /** Screen labels, tabs, groups, and controls use the matching user icon. */
    public static String iconForLabel(String label){
        if(label==null||label.trim().isEmpty())return "favorites";
        String t=label.toLowerCase(Locale.ROOT).replace('ё','е');
        if(has(t,"драконь","драконов")&&has(t,"кост"))return "dragonbones";
        if(has(t,"драконь","драконов")&&has(t,"чешу"))return "dragonscales";
        if(has(t,"драгоценн")&&has(t,"камн"))return "gems";
        if(has(t,"боев","военн")&&has(t,"молот"))return "warhammers";
        if(has(t,"двуруч")&&has(t,"молот"))return "warhammers";
        if(has(t,"арбалет"))return "crossbows";
        if(has(t,"стрела","болт"))return "bows";
        if(has(t,"булава"))return "weapon";
        if(has(t,"кинжал","бритва"))return "daggers";
        if(has(t,"топор","секир"))return "axes";
        if(has(t,"меч","клинок","сабл"))return "swords";
        if(has(t,"лук","стрельб"))return "bows";
        if(has(t,"шлем","капюшон","маск"))return "helmets";
        if(has(t,"кирас","нагрудник"))return "cuirasses";
        if(has(t,"перчатк","рукавиц","наруч"))return "gloves";
        if(has(t,"сапог","ботин"))return "boots";
        if(has(t,"щит","блокирован"))return "shields";
        if(has(t,"кольц"))return "rings";
        if(has(t,"ожерел","цепочк"))return "necklaces";
        if(has(t,"амулет"))return "amulets";
        if(has(t,"диадем","корон"))return "circlets";
        if(has(t,"отмыч","взлом"))return "lockpicks";
        if(has(t,"факел"))return "torches";
        if(has(t,"слиток"))return "ingots";
        if(has(t,"кожаные полос","кожа","кожевенн"))return "leather";
        if(has(t,"драгоценност","сокровищ"))return "valuables";
        if(has(t,"украшени","ювелирн"))return "jewelry";
        if(has(t,"ингредиент","горноцвет","лаванд","пшениц","сахар"))return "ingredients";
        if(has(t,"артефакт","даэдрическ"))return "artifacts";
        if(has(t,"зель","эликсир","алхими"))return "potion";
        if(has(t,"свиток"))return "scrolls";
        if(has(t,"книг","библиотек","рукопис"))return "book";
        if(has(t,"септим","монет","валют","кошель"))return "septims";
        if(has(t,"материал","ресурс","сырье"))return "materials";
        if(has(t,"инструмент","кузнеч"))return "tools";
        if(has(t,"напит","вино","медовух"))return "drinks";
        if(has(t,"еда","пища","провиант","хлеб"))return "food";
        if(has(t,"одежд","одеян","плащ","шляп"))return "clothing";
        if(has(t,"оруж","боевое"))return "weapon";
        if(has(t,"брон","доспех"))return "armor";
        if(has(t,"дракон","скайрим","skyrim"))return "dragon";
        if(has(t,"избран","любим"))return "favorites";
        if(has(t,"карта","локац","маршрут","путь","местности"))return "map";
        if(has(t,"инвентар","сумка","поклажа","предмет"))return "bag";
        if(has(t,"кодекс","бестиарий"))return "codex";
        if(has(t,"герой","характерист","рас","навык","уровень"))return "hero";
        if(has(t,"журнал","задан","квест","летопись"))return "journal";
        if(has(t,"настройк","систем","тема","модел","ии-","обновлен"))return "settings";
        if(has(t,"истори","сюжет","сцен","игров","начать","продолж","ответ"))return "story";
        return "artifacts";
    }

    /**
     * Always resolve inventory categories to the exact icon supplied for them.
     * Broad text heuristics can accidentally match a word inside another label.
     */
    public static String iconForCategory(String category){
        if(category==null)return "bag";
        String key=category.trim().toLowerCase(Locale.ROOT).replace('ё','е');
        switch(key){
            case "все":return "bag";
            case "деньги":case "септимы":return "septims";
            case "еда":case "пища":return "food";
            case "напитки":return "drinks";
            case "зелья":return "potion";
            case "инструменты":return "tools";
            case "оружие":return "weapon";
            case "броня":return "armor";
            case "одежда":return "clothing";
            case "украшения":return "jewelry";
            case "материалы":return "materials";
            case "ингредиенты":return "ingredients";
            case "драгоценности":return "valuables";
            case "артефакты":return "artifacts";
            case "боеприпасы":return "bows";
            case "камни душ":return "gems";
            case "книги":return "book";
            case "свитки":return "scrolls";
            default:return iconForLabel(category);
        }
    }

    /** Correct physical item glyph takes precedence over category decoration. */
    public static String iconForItem(String name,String category){
        String t=name==null?"":name.toLowerCase(Locale.ROOT).replace('ё','е');
        if(has(t,"септим","монет"))return "septims";
        if(has(t,"отмыч"))return "lockpicks";
        if(has(t,"факел"))return "torches";
        if(has(t,"амулет","оберег"))return "amulets";
        if(has(t,"зель","эликсир"))return "potion";
        if(has(t,"медовух","вино","пиво","напит","сок","квас"))return "drinks";
        if(has(t,"хлеб","сыр","мясо","рыба","яблок","еда","пища","похлебк","суп"))return "food";
        if(has(t,"кольц"))return "rings";
        if(has(t,"ожерел"))return "necklaces";
        if(has(t,"диадем"))return "circlets";
        if(has(t,"драконь")&&has(t,"кост"))return "dragonbones";
        if(has(t,"драконь")&&has(t,"чешу"))return "dragonscales";
        if(has(t,"кинжал","бритва"))return "daggers";
        if(has(t,"боевой молот"))return "warhammers";
        if(has(t,"арбалет"))return "crossbows";
        if(has(t,"топор","секир"))return "axes";
        if(has(t,"щит"))return "shields";
        if(has(t,"шлем","капюшон","маска"))return "helmets";
        if(has(t,"кирас"))return "cuirasses";
        if(has(t,"перчатк","рукавиц"))return "gloves";
        if(has(t,"сапог","ботин"))return "boots";
        if(has(t,"слиток"))return "ingots";
        if(has(t,"кожаные полос"))return "leather";
        if(has(t,"меч","клинок"))return "swords";
        if(has(t,"лук"))return "bows";
        if(has(t,"свиток"))return "scrolls";
        String id=iconForLabel(name);
        if(!"artifacts".equals(id))return id;
        return iconForCategory(category);
    }
}
