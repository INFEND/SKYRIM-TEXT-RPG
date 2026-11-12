package dev.infend.skyrimtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Exact offline atlas rebuilt from the player's 358 individually named PNGs.
 * All 358 glyphs are independent: no adjacent/reused graphics or guessed cells.
 * Duplicate source captions "замки" are disambiguated into door locks (164)
 * and castle buildings (175). The index stores verified sprite bounds.
 */
public final class SkyrimIconAtlas {
    public static final String IMAGE="icons/skyrim_icons_358_exact.webp";
    public static final String INDEX="icons/skyrim_icons_358_exact.json";
    private static final Map<String,Rect> glyphs=new HashMap<>();
    private static final Map<String,String> aliases=new HashMap<>();
    private static Bitmap atlas;
    private static boolean attempted;
    private static int resourceGeneration=-1;

    static {
        aliases.put("story","История");
        aliases.put("map","карта");
        aliases.put("bag","инвентарь");
        aliases.put("satchel","инвентарь");
        aliases.put("codex","кодекс");
        aliases.put("hero","герой");
        aliases.put("journal","журнал");
        aliases.put("settings","настройки");
        aliases.put("dragon","дракон");
        aliases.put("weapon","оружие");
        aliases.put("swords","мечи");
        aliases.put("daggers","кинжалы");
        aliases.put("axes","топоры");
        aliases.put("warhammers","боевые молоты");
        aliases.put("bows","луки");
        aliases.put("crossbows","арбалеты");
        aliases.put("armor","броня");
        aliases.put("helmets","шлемы");
        aliases.put("cuirasses","кирасы");
        aliases.put("gloves","перчатки");
        aliases.put("boots","сапоги");
        aliases.put("shields","щиты");
        aliases.put("shield","щиты");
        aliases.put("clothing","одежда");
        aliases.put("jewelry","украшения");
        aliases.put("rings","кольца");
        aliases.put("necklaces","ожерелья");
        aliases.put("amulets","амулеты");
        aliases.put("circlets","диадемы");
        aliases.put("potion","зелья");
        aliases.put("book","книги");
        aliases.put("scroll","свитки");
        aliases.put("scrolls","свитки");
        aliases.put("septims","септимы");
        aliases.put("lockpicks","отмычки");
        aliases.put("tools","инструменты");
        aliases.put("torches","факелы");
        aliases.put("materials","материалы");
        aliases.put("ingots","слитки");
        aliases.put("leather","кожа");
        aliases.put("dragonbones","драконьи кости");
        aliases.put("dragonscales","драконья чешуя");
        aliases.put("valuables","драгоценности");
        aliases.put("gems","драгоценные камни");
        aliases.put("ingredients","ингредиенты");
        aliases.put("artifacts","артефакты");
        aliases.put("food","еда");
        aliases.put("drinks","напитки");
        aliases.put("customweapon","пользовательское оружие");
        aliases.put("customitems","пользовательские предметы");
        aliases.put("favorites","избранное");
        aliases.put("star","избранное");
        aliases.put("magic","Магия");
        aliases.put("health","здоровье");
        aliases.put("stamina","запас сил");
        aliases.put("magicka","мана");
        aliases.put("experience","опыт");
        aliases.put("level","уровень");
        aliases.put("skills","навыки");
        aliases.put("castle","замки (сооружения)");
    }

    private SkyrimIconAtlas(){}

    private static String normalized(String key){
        return key==null?"":key.toLowerCase(Locale.ROOT).replace('ё','е').trim();
    }
    private static synchronized void ensure(Context context){
        int now=ResourceStore.generation();
        if(resourceGeneration!=now){resourceGeneration=now;attempted=false;atlas=null;glyphs.clear();}
        if(attempted)return;
        attempted=true;
        try(InputStream image=ResourceStore.open(context,IMAGE);
            InputStream data=ResourceStore.open(context,INDEX)) {
            BitmapFactory.Options config=new BitmapFactory.Options();
            config.inScaled=false;
            Bitmap loaded=BitmapFactory.decodeStream(image,null,config);
            if(loaded==null||loaded.getWidth()!=1728||loaded.getHeight()!=1920)
                throw new IllegalStateException("Размер именного атласа 358");
            ByteArrayOutputStream text=new ByteArrayOutputStream(30000);
            byte[] chunk=new byte[4096];int n;
            while((n=data.read(chunk))!=-1)text.write(chunk,0,n);
            JSONObject json=new JSONObject(new String(text.toByteArray(),StandardCharsets.UTF_8));
            if(json.optInt("total")!=358||json.optInt("unique")!=358
                    ||json.optInt("width")!=loaded.getWidth()
                    ||json.optInt("height")!=loaded.getHeight())
                throw new IllegalStateException("Контракт 358 уникальных иконок");
            JSONObject locations=json.getJSONObject("icons");
            java.util.Iterator<String> keys=locations.keys();
            while(keys.hasNext()){
                String name=keys.next();
                JSONArray values=locations.getJSONArray(name);
                if(values.length()!=4)continue;
                Rect box=new Rect(values.getInt(0),values.getInt(1),
                    values.getInt(2),values.getInt(3));
                if(box.left<0||box.top<0||box.right>loaded.getWidth()
                    ||box.bottom>loaded.getHeight()
                    ||box.width()<=0||box.height()<=0)continue;
                glyphs.put(normalized(name),box);
            }
            if(glyphs.size()!=358)throw new IllegalStateException("Неполный именной каталог иконок");
            atlas=loaded;
        }catch(Exception ex){
            glyphs.clear();
            atlas=null; // Existing 48-image WebP remains a safe fallback.
        }
    }
    public static String exactLabel(Context ctx,String name){
        ensure(ctx);
        return glyphs.containsKey(normalized(name))?name:null;
    }
    public static int knownCount(Context ctx){
        ensure(ctx);
        return glyphs.size();
    }
    private static Rect find(Context ctx,String name){
        ensure(ctx);
        String key=normalized(name);
        if(aliases.containsKey(key))key=normalized(aliases.get(key));
        // Lock means the actual door lock; castle is a separate named sprite.
        if("замок".equals(key)||"замок двери".equals(key))key=normalized("замки");
        if("крепостной замок".equals(key))key=normalized("замки (сооружения)");
        return glyphs.get(key);
    }
    public static boolean draw(Context context,Canvas canvas,String key,
                               int tint,float width,float height){
        Rect sprite=find(context,key);
        if(sprite==null||atlas==null||width<=0||height<=0)return false;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
        int light=(Color.red(tint)+Color.green(tint)+Color.blue(tint))/3;
        p.setAlpha(Math.max(175,Math.min(255,213+(light-110)/4)));
        float margin=Math.min(width,height)*.065f;
        float fit=Math.min((width-2*margin)/sprite.width(),
                           (height-2*margin)/sprite.height());
        float w=sprite.width()*fit,h=sprite.height()*fit;
        RectF dest=new RectF((width-w)/2f,(height-h)/2f,(width+w)/2f,(height+h)/2f);
        canvas.drawBitmap(atlas,sprite,dest,p);
        return true;
    }
    public static boolean drawMarker(Context ctx,Canvas c,String name,
                                     int tint,float x,float y,float size){
        Rect sprite=find(ctx,name);
        if(sprite==null||atlas==null)return false;
        c.save();
        c.translate(x-size/2f,y-size/2f);
        boolean drawn=draw(ctx,c,name,tint,size,size);
        c.restore();
        return drawn;
    }
}
