package dev.infend.skyrimtext;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;
import android.widget.OverScroller;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

/** Offline map sheets. A single geographic projection drives all markers. */
public final class SkyrimTerrainMapView extends View {
    public interface OnPlace {void selected(WorldAtlas.Place location);}
    public interface OnCatalog {void selected(ArrayList<SkyrimMapCatalog.Site> locations);}
    public static final String TERRAIN="maps/skyrim_geographic.webp";
    public static final int MAP_W=4096,MAP_H=4096;
    private static final String[] TEXTURES={TERRAIN,"maps/solstheim_terrain.webp"};
    private static final int[] WIDTH={MAP_W,2048},HEIGHT={MAP_H,2048};
    private static final Bitmap[] IMAGES=new Bitmap[2];
    private static int resourceGeneration=-1;
    private static final java.util.concurrent.ExecutorService LOADER=Executors.newSingleThreadExecutor();

    private final GameEngine game;
    private final OnPlace callback;
    private OnCatalog catalogCallback;
    private final Set<String> hiddenTypes;
    private final SharedPreferences prefs;
    private final Paint imagePaint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureDetector gesture;
    private final ScaleGestureDetector scaler;
    private final OverScroller inertia;
    private final ArrayList<SkyrimMapCatalog.Site> sites=SkyrimMapCatalog.all();
    private ArrayList<MapPinLayout.Pin> pins;
    private String focusId="";
    private int sheet,region=-1;
    private float scale=1f,minScale=.1f,maxScale=1f,left,top;
    private boolean initialized,dragged;
    private volatile boolean loading,failed;
    private final float density;
    private final ArrayList<RectF> labels=new ArrayList<>();

    public SkyrimTerrainMapView(Context context,GameEngine game,OnPlace callback,Set<String> hiddenTypes){
        super(context);this.game=game;this.callback=callback;
        this.hiddenTypes=new HashSet<>(hiddenTypes);
        prefs=context.getSharedPreferences("skyrim_text_v1",Context.MODE_PRIVATE);
        density=getResources().getDisplayMetrics().density;
        SkyrimMapCatalog.Site player=SkyrimMapCatalog.find(game.progress.location);
        sheet=Math.max(0,Math.min(1,prefs.getInt("terrain_map_sheet",player==null?0:player.sheet)));
        inertia=new OverScroller(context);
        setContentDescription("Карта Скайрима и Солстейма. Перемещай одним пальцем, "+
            "изменяй масштаб двумя пальцами, нажимай на метки локаций.");
        gesture=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){inertia.forceFinished(true);return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){
                if(!scaler.isInProgress()){
                    dragged=true;
                    float previousX=left,previousY=top;
                    left-=dx;top-=dy;constrain();
                    moved(left-previousX,top-previousY);
                }
                return true;
            }
            @Override public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){
                if(scaler.isInProgress())return false;
                dragged=true;
                int minX=Math.min(0,Math.round(getWidth()-WIDTH[sheet]*scale));
                int minY=Math.min(0,Math.round(getHeight()-HEIGHT[sheet]*scale));
                if(minX==0&&minY==0)return false;
                inertia.fling(Math.round(left),Math.round(top),Math.round(vx),Math.round(vy),
                    minX,minX==0?Math.round(left):0,minY,minY==0?Math.round(top):0);
                postInvalidateOnAnimation();return true;
            }
            @Override public boolean onSingleTapConfirmed(MotionEvent e){
                if(!dragged)select(e.getX(),e.getY());return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e){
                if(scale>=maxScale*.95f)showWholeProvince();
                else{zoomAt(1.85f,e.getX(),e.getY());persist();}
                return true;
            }
        });
        scaler=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector d){
                zoomAt(d.getScaleFactor(),d.getFocusX(),d.getFocusY());dragged=true;return true;
            }
        });
        loadTerrain();
    }
    private void loadTerrain(){
        final int requested=sheet;
        final int generation=ResourceStore.generation();
        synchronized(IMAGES){
            if(resourceGeneration!=generation){java.util.Arrays.fill(IMAGES,null);resourceGeneration=generation;}
            if(IMAGES[requested]!=null){loading=false;failed=false;return;}
        }
        loading=true;failed=false;
        final Context app=getContext().getApplicationContext();
        LOADER.execute(()->{
            Bitmap decoded=null;
            synchronized(IMAGES){decoded=IMAGES[requested];}
            if(decoded==null){
                try(java.io.InputStream in=ResourceStore.open(app,TEXTURES[requested])){
                    BitmapFactory.Options options=new BitmapFactory.Options();
                    options.inPreferredConfig=Bitmap.Config.RGB_565;options.inScaled=false;
                    decoded=BitmapFactory.decodeStream(in,null,options);
                    if(decoded!=null&&(decoded.getWidth()!=WIDTH[requested]||decoded.getHeight()!=HEIGHT[requested])){
                        decoded.recycle();decoded=null;
                    }
                    synchronized(IMAGES){if(decoded!=null&&resourceGeneration==generation)IMAGES[requested]=decoded;}
                }catch(Exception ignored){}
            }
            final boolean success=decoded!=null;
            post(()->{if(sheet==requested){loading=false;failed=!success;invalidate();}});
        });
    }
    public void setOnCatalog(OnCatalog listener){catalogCallback=listener;}
    public int getSheet(){return sheet;}
    public void setSheet(int next){
        next=Math.max(0,Math.min(1,next));if(next==sheet)return;
        persist();inertia.forceFinished(true);sheet=next;focusId="";initialized=false;
        prefs.edit().putInt("terrain_map_sheet",sheet).apply();
        configure(getWidth(),getHeight(),0,0);loadTerrain();changed();
    }
    public void setMapRegion(int next){
        region=next;focusId="";
        if(next>=0)setSheet(next==9?1:0);
        changed();
    }
    public static float positionX(WorldAtlas.Place place){
        SkyrimMapCatalog.Site site=place==null?null:SkyrimMapCatalog.find(place.name);
        return site==null?Float.NaN:site.x;
    }
    public static float positionY(WorldAtlas.Place place){
        SkyrimMapCatalog.Site site=place==null?null:SkyrimMapCatalog.find(place.name);
        return site==null?Float.NaN:site.y;
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
        super.onSizeChanged(w,h,oldW,oldH);configure(w,h,oldW,oldH);
    }
    private String key(String property){return "terrain_map_v2_"+sheet+"_"+property;}
    private void configure(int w,int h,int oldW,int oldH){
        if(w<=0||h<=0)return;
        float previousScale=scale;
        float cx=(oldW*.5f-left)/Math.max(.0001f,previousScale);
        float cy=(oldH*.5f-top)/Math.max(.0001f,previousScale);
        float relative=initialized?previousScale/Math.max(.0001f,minScale):2.4f;
        minScale=Math.min(w/(float)WIDTH[sheet],h/(float)HEIGHT[sheet]);maxScale=minScale*14f;
        if(!initialized){
            float z=prefs.getFloat(key("zoom"),sheet==0?2.4f:1.2f);
            relative=Float.isFinite(z)?z:2.4f;
            cx=prefs.getFloat(key("x"),Float.NaN);cy=prefs.getFloat(key("y"),Float.NaN);
            if(!Float.isFinite(cx)||!Float.isFinite(cy)){
                SkyrimMapCatalog.Site player=SkyrimMapCatalog.find(game.progress.location);
                cx=player!=null&&player.sheet==sheet?player.x:WIDTH[sheet]/2f;
                cy=player!=null&&player.sheet==sheet?player.y:HEIGHT[sheet]/2f;
            }
            initialized=true;
        }
        scale=Math.max(minScale,Math.min(maxScale,minScale*relative));
        left=w*.5f-cx*scale;top=h*.5f-cy*scale;constrain();changed();
    }
    private void changed(){pins=null;invalidate();}
    /** During drags/flings, icon visibility does not change. Move the
     * already chosen world-anchored pins by the same camera translation. */
    private void moved(float dx,float dy){
        MapPinLayout.translate(pins,dx,dy);
        postInvalidateOnAnimation();
    }
    private void constrain(){
        if(getWidth()<=0||getHeight()<=0)return;
        float width=WIDTH[sheet]*scale,height=HEIGHT[sheet]*scale;
        left=width<=getWidth()?(getWidth()-width)/2f:Math.max(getWidth()-width,Math.min(0,left));
        top=height<=getHeight()?(getHeight()-height)/2f:Math.max(getHeight()-height,Math.min(0,top));
    }
    private void zoomAt(float factor,float px,float py){
        if(!initialized||!Float.isFinite(factor)||factor<=0)return;
        inertia.forceFinished(true);
        float next=Math.max(minScale,Math.min(maxScale,scale*factor)),ratio=next/scale;
        left=px-(px-left)*ratio;top=py-(py-top)*ratio;scale=next;constrain();changed();
    }
    public void zoomBy(float factor){zoomAt(factor,getWidth()/2f,getHeight()/2f);persist();}
    public void showWholeProvince(){
        if(getWidth()<=0)return;
        inertia.forceFinished(true);scale=minScale;
        left=(getWidth()-WIDTH[sheet]*scale)/2f;top=(getHeight()-HEIGHT[sheet]*scale)/2f;
        constrain();changed();persist();
    }
    public void centerOnPlayer(){
        SkyrimMapCatalog.Site player=SkyrimMapCatalog.find(game.progress.location);
        if(player!=null)centerOn(player);
    }
    public void centerOn(WorldAtlas.Place place){
        if(place!=null)centerOn(SkyrimMapCatalog.find(place.name));
    }
    public void centerOn(SkyrimMapCatalog.Site target){
        if(target==null)return;
        setSheet(target.sheet);focusId=target.id;
        if(getWidth()<=0){post(()->centerOn(target));return;}
        inertia.forceFinished(true);
        scale=Math.min(maxScale,Math.max(scale,minScale*5.5f));
        left=getWidth()/2f-target.x*scale;top=getHeight()/2f-target.y*scale;
        constrain();changed();persist();
    }
    private void persist(){
        if(!initialized||scale<=0||minScale<=0||getWidth()<=0)return;
        prefs.edit().putFloat(key("zoom"),scale/minScale)
            .putFloat(key("x"),(getWidth()/2f-left)/scale)
            .putFloat(key("y"),(getHeight()/2f-top)/scale)
            .putInt("terrain_map_sheet",sheet).apply();
    }
    public void saveCamera(){persist();}
    public void restoreCamera(){
        initialized=false;sheet=Math.max(0,Math.min(1,prefs.getInt("terrain_map_sheet",sheet)));
        configure(getWidth(),getHeight(),0,0);loadTerrain();changed();
    }
    private ArrayList<MapPinLayout.Pin> renderedPins(){
        if(pins==null){
            SkyrimMapCatalog.Site player=SkyrimMapCatalog.find(game.progress.location);
            pins=MapPinLayout.build(sites,sheet,region,hiddenTypes,player==null?"":player.id,
                focusId,scale,left,top,getWidth(),getHeight(),density,
                scale/Math.max(.0001f,minScale));
        }
        return pins;
    }
    /** Shared source of location-type icons for both the map and the location list.
     * A single mapping prevents glyph mismatches between views. */
    public static String iconForSite(SkyrimMapCatalog.Site site){
        String t=site.type;
        if(site.capital())return "столицы владений";
        if(t.contains("Двемер"))return "двемерские руины";
        if(t.contains("дракона"))return "логова драконов";
        if(t.contains("Пещера"))return "пещеры";
        if(t.contains("Руины")||t.contains("курган"))return "нордские руины";
        if(t.contains("лагерь")||t.contains("Лагерь"))return "лагеря";
        if(t.contains("Форт"))return "крепости";
        if(t.contains("Башня"))return "башни";
        if(t.contains("Деревня")||t.contains("Поселение"))return "деревни";
        if(t.contains("Святилище"))return "даэдрические святилища";
        return "обнаруженные места";
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(0xff080c0e);
        Bitmap image;synchronized(IMAGES){image=IMAGES[sheet];}
        if(image!=null)canvas.drawBitmap(image,null,new RectF(left,top,left+WIDTH[sheet]*scale,top+HEIGHT[sheet]*scale),imagePaint);
        ArrayList<MapPinLayout.Pin> shown=renderedPins();labels.clear();
        // Markers never collapse into moving numeric clusters: each icon has
        // one immutable location ID and world coordinate. Size changes only
        // as a continuous function of zoom, and is capped in dp.
        for(MapPinLayout.Pin pin:shown){
            float diameter=pin.sizeDp*density;
            float margin=diameter*.65f;
            if(pin.x < -margin||pin.y < -margin||
                pin.x>getWidth()+margin||pin.y>getHeight()+margin)continue;
            SkyrimMapCatalog.Site site=pin.sites.get(0);
            int color=pin.player?0xfff2dc98:pin.focus?0xffffffff:0xffd5dfe1;
            float radius=diameter*.47f;
            ink.setStyle(Paint.Style.FILL);ink.setColor(0xe0091216);
            canvas.drawCircle(pin.x,pin.y,radius+1.1f*density,ink);
            if(!SkyrimIconAtlas.drawMarker(getContext(),canvas,
                pin.player?"маркер игрока":iconForSite(site),color,pin.x,pin.y,diameter)){
                ink.setColor(color);
                canvas.drawCircle(pin.x,pin.y,Math.max(2f*density,diameter*.24f),ink);
            }
            if(pin.player||pin.focus){
                ink.setStyle(Paint.Style.STROKE);
                ink.setStrokeWidth(1.3f*density);
                ink.setColor(color);
                canvas.drawCircle(pin.x,pin.y,radius+3f*density,ink);
                ink.setStyle(Paint.Style.FILL);
            }
        }
        // Reserve text rectangles in priority order, so labels never cover each other.
        for(int pass=0;pass<3;pass++)for(MapPinLayout.Pin pin:shown){
            SkyrimMapCatalog.Site site=pin.sites.get(0);
            int priority=pin.player||pin.focus?0:site.capital()?1:2;
            if(priority!=pass)continue;
            if(pass==2&&scale/minScale<4.2f)continue;
            ink.setTypeface(SovngardeTypeface.face(getContext(),pass<2));
            ink.setTextSize((pass==0?16:pass==1?14:12)*density);
            String title=site.name;
            float maxWidth=Math.min(225*density,getWidth()-18*density);
            while(title.length()>8&&ink.measureText(title)>maxWidth)title=title.substring(0,title.length()-2);
            if(!title.equals(site.name))title+="…";
            float tw=ink.measureText(title);
            // Do not clamp label X to a screen edge. A clamped label slides
            // relative to its pin during panning and looks like map jitter.
            float tx=pin.x+pin.sizeDp*density*.58f+4*density;
            float ty=pin.y-5*density;
            if(tx<5*density||tx+tw>getWidth()-5*density||
               ty<20*density||ty>getHeight()-31*density)continue;
            RectF rect=new RectF(tx-3*density,ty-17*density,tx+tw+3*density,ty+4*density);
            boolean overlap=false;for(RectF old:labels)if(RectF.intersects(old,rect)){overlap=true;break;}
            if(overlap)continue;labels.add(rect);
            ink.setColor(0xff080d11);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(3*density);
            canvas.drawText(title,tx,ty,ink);
            ink.setStyle(Paint.Style.FILL);ink.setColor(pin.player?0xfff2dc98:0xfff0f1eb);
            canvas.drawText(title,tx,ty,ink);
        }
        ink.setTypeface(android.graphics.Typeface.DEFAULT);ink.setTextSize(11*density);
        ink.setStyle(Paint.Style.FILL);ink.setColor(0xe6081115);
        canvas.drawRect(0,getHeight()-27*density,getWidth(),getHeight(),ink);
        ink.setColor(0xffe1e7e7);
        String status=(sheet==0?"СКАЙРИМ":"СОЛСТЕЙМ")+
            String.format(java.util.Locale.ROOT,"  ×%.1f",scale/Math.max(.0001f,minScale));
        if(loading)status+=" | Загрузка рельефа…";
        else if(failed)status+=" | Рельеф недоступен";
        if(SkyrimMapCatalog.find(game.progress.location)==null)status+=" | Герой вне поверхности";
        canvas.drawText(status,10*density,getHeight()-10*density,ink);
    }
    private void select(float x,float y){
        MapPinLayout.Pin pin=MapPinLayout.hit(renderedPins(),x,y,28*density);
        if(pin==null){performClick();return;}
        SkyrimMapCatalog.Site site=pin.sites.get(0);
        focusId=site.id;changed();
        WorldAtlas.Place place=WorldAtlas.find(SkyrimMapCatalog.travelDestination(site));
        if(catalogCallback!=null){
            ArrayList<SkyrimMapCatalog.Site> selected=new ArrayList<>(1);
            selected.add(site);
            catalogCallback.selected(selected);
        }else if(place!=null&&callback!=null)callback.selected(place);
        performClick();
    }
    @Override public void computeScroll(){
        if(inertia.computeScrollOffset()){
            float previousX=left,previousY=top;
            left=inertia.getCurrX();top=inertia.getCurrY();constrain();
            moved(left-previousX,top-previousY);
            if(inertia.isFinished())persist();
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            dragged=false;ViewParent parent=getParent();
            if(parent!=null)parent.requestDisallowInterceptTouchEvent(true);
        }
        scaler.onTouchEvent(event);gesture.onTouchEvent(event);
        if(action==MotionEvent.ACTION_POINTER_DOWN)dragged=true;
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
            persist();ViewParent parent=getParent();
            if(parent!=null)parent.requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }
    @Override protected void onDetachedFromWindow(){inertia.forceFinished(true);persist();super.onDetachedFromWindow();}
    @Override public boolean performClick(){super.performClick();return true;}
}

