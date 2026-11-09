package dev.infend.skyrimtext;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewParent;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A real offline interactive terrain map, built from the six player-provided
 * geographic screenshots. Regions not covered by those screenshots are a
 * subdued terrain interpolation, never presented as exact captured terrain.
 *
 * All markers use the SAME WorldAtlas places used by story/travel/filtering,
 * with one geographic calibration per old atlas hold. No fake locations.
 *
 * Camera is independent of the app's vertical ScrollView: drag and pinch here
 * move the map, while the story and inventory scroll normally elsewhere.
 */
public final class SkyrimTerrainMapView extends View {
    public interface OnPlace { void selected(WorldAtlas.Place location); }
    public interface OnCatalog {
        void selected(ArrayList<SkyrimMapCatalog.Site> locations);
    }

    public static final String TERRAIN="maps/skyrim_user_mosaic.webp";
    public static final int MAP_W=3500;
    public static final int MAP_H=1720;

    private static final int[] OLD_CENTER_X={565,833,803,239,146,416,812,578,373,975};
    private static final int[] OLD_CENTER_Y={635,938,496,303,676,967,287,247,405,399};
    private static final int[] NEW_CENTER_X={2020,2660,2720,1220,390,1610,2490,1940,1460,3280};
    private static final int[] NEW_CENTER_Y={850,1270,555,470,955,1385,265,250,520,300};

    private static Bitmap shared;
    private static boolean loadAttempted;
    private static synchronized Bitmap terrain(Context ctx){
        if(!loadAttempted){
            loadAttempted=true;
            try(java.io.InputStream in=ctx.getApplicationContext().getAssets().open(TERRAIN)){
                BitmapFactory.Options config=new BitmapFactory.Options();
                config.inPreferredConfig=Bitmap.Config.RGB_565;
                config.inScaled=false;
                Bitmap value=BitmapFactory.decodeStream(in,null,config);
                if(value!=null&&value.getWidth()==MAP_W&&value.getHeight()==MAP_H)
                    shared=value;
                else if(value!=null)value.recycle();
            }catch(Exception ignored){}
        }
        return shared;
    }

    private final GameEngine game;
    private final OnPlace callback;
    private OnCatalog catalogCallback;
    private final Set<String> hiddenTypes;
    private final SharedPreferences prefs;
    private final Paint terrainPaint=new Paint(Paint.FILTER_BITMAP_FLAG|Paint.ANTI_ALIAS_FLAG);
    private final Paint pinPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureDetector gesture;
    private final ScaleGestureDetector scaler;

    private float scale=1f,minScale=.1f,maxScale=1f;
    private float left,top;
    private boolean initialized;
    private boolean dragged;
    private float touchX,touchY;

    public SkyrimTerrainMapView(Context context,GameEngine game,
                                OnPlace callback,Set<String> hiddenTypes){
        super(context);
        this.game=game;
        this.callback=callback;
        this.hiddenTypes=new HashSet<>(hiddenTypes);
        this.prefs=context.getSharedPreferences("skyrim_text_v1",Context.MODE_PRIVATE);
        setLayerType(View.LAYER_TYPE_HARDWARE,null);
        setContentDescription("Карта Скайрима. Проведи пальцем для перемещения, "
            +"разведи два пальца для увеличения, нажми на локацию для открытия.");
        gesture=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){
                if(!scaler.isInProgress()){
                    dragged=true;
                    left-=dx;top-=dy;constrain();invalidate();
                }
                return true;
            }
            @Override public boolean onSingleTapConfirmed(MotionEvent e){
                if(dragged)return true;
                select(e.getX(),e.getY());
                return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e){
                zoomAt(1.85f,e.getX(),e.getY());
                persist();
                return true;
            }
        });
        scaler=new ScaleGestureDetector(context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener(){
                @Override public boolean onScale(ScaleGestureDetector detector){
                    zoomAt(detector.getScaleFactor(),
                        detector.getFocusX(),detector.getFocusY());
                    dragged=true;
                    return true;
                }
            });
    }

    public void setOnCatalog(OnCatalog listener){catalogCallback=listener;}

    /** One aggregated marker per coarse reference grid cell. */
    private LinkedHashMap<String,ArrayList<SkyrimMapCatalog.Site>> catalogClusters(){
        LinkedHashMap<String,ArrayList<SkyrimMapCatalog.Site>> grouped=new LinkedHashMap<>();
        for(SkyrimMapCatalog.Site place:SkyrimMapCatalog.all()){
            if(hiddenTypes.contains(place.type))continue;
            ArrayList<SkyrimMapCatalog.Site> list=grouped.get(place.grid);
            if(list==null){
                list=new ArrayList<>();
                grouped.put(place.grid,list);
            }
            list.add(place);
        }
        return grouped;
    }

    /** Screenshots were georegistered by their visible city markers. */
    public static float positionX(WorldAtlas.Place p){
        int r=Math.max(0,Math.min(p.region,OLD_CENTER_X.length-1));
        return NEW_CENTER_X[r]+(p.x-OLD_CENTER_X[r])*1.85f;
    }
    public static float positionY(WorldAtlas.Place p){
        int r=Math.max(0,Math.min(p.region,OLD_CENTER_Y.length-1));
        return NEW_CENTER_Y[r]+(p.y-OLD_CENTER_Y[r])*1.85f;
    }

    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
        super.onSizeChanged(w,h,oldW,oldH);
        if(w<=0||h<=0)return;
        minScale=Math.min(w/(float)MAP_W,h/(float)MAP_H);
        maxScale=minScale*11f;
        if(!initialized){
            initialized=true;
            float requested=prefs.getFloat("terrain_map_zoom",2.4f);
            scale=Math.max(minScale,Math.min(maxScale,minScale*requested));
            float cx=prefs.getFloat("terrain_map_x",Float.NaN);
            float cy=prefs.getFloat("terrain_map_y",Float.NaN);
            if(!Float.isFinite(cx)||!Float.isFinite(cy)){
                WorldAtlas.Place where=WorldAtlas.find(game.progress.location);
                if(where!=null){cx=positionX(where);cy=positionY(where);}
                else{cx=NEW_CENTER_X[Math.max(0,Math.min(game.region,9))];
                     cy=NEW_CENTER_Y[Math.max(0,Math.min(game.region,9))];}
            }
            left=w*.5f-cx*scale;top=h*.5f-cy*scale;
        }else{
            // Preserve the physical point at the old center across rotation.
            float cx=(oldW*.5f-left)/Math.max(.0001f,scale);
            float cy=(oldH*.5f-top)/Math.max(.0001f,scale);
            scale=Math.max(minScale,Math.min(maxScale,scale));
            left=w*.5f-cx*scale;top=h*.5f-cy*scale;
        }
        constrain();
    }

    private void constrain(){
        if(getWidth()<=0||getHeight()<=0)return;
        float canvasW=MAP_W*scale,canvasH=MAP_H*scale;
        left=canvasW<=getWidth()?(getWidth()-canvasW)/2f:
            Math.max(getWidth()-canvasW,Math.min(0,left));
        top=canvasH<=getHeight()?(getHeight()-canvasH)/2f:
            Math.max(getHeight()-canvasH,Math.min(0,top));
    }
    private void zoomAt(float factor,float pivotX,float pivotY){
        if(!Float.isFinite(factor)||factor<=0)return;
        float next=Math.max(minScale,Math.min(maxScale,scale*factor));
        if(scale<=0)return;
        float ratio=next/scale;
        left=pivotX-(pivotX-left)*ratio;
        top=pivotY-(pivotY-top)*ratio;
        scale=next;
        constrain();invalidate();
    }

    public void zoomBy(float multiplier){
        zoomAt(multiplier,getWidth()/2f,getHeight()/2f);
        persist();
    }
    public void showWholeProvince(){
        if(getWidth()<=0)return;
        scale=minScale;
        left=(getWidth()-MAP_W*scale)/2f;
        top=(getHeight()-MAP_H*scale)/2f;
        constrain();invalidate();persist();
    }
    public void centerOnPlayer(){
        WorldAtlas.Place player=WorldAtlas.find(game.progress.location);
        if(player!=null)centerOn(player);
    }
    public void centerOn(WorldAtlas.Place target){
        if(getWidth()<=0||target==null)return;
        scale=Math.max(scale,minScale*2.7f);
        left=getWidth()/2f-positionX(target)*scale;
        top=getHeight()/2f-positionY(target)*scale;
        constrain();invalidate();persist();
    }
    private void persist(){
        if(!initialized||scale<=0)return;
        float cx=(getWidth()/2f-left)/scale;
        float cy=(getHeight()/2f-top)/scale;
        prefs.edit().putFloat("terrain_map_zoom",scale/minScale)
            .putFloat("terrain_map_x",cx).putFloat("terrain_map_y",cy).apply();
    }

    private boolean visible(WorldAtlas.Place p){
        return p.name.equals(game.progress.location)||!hiddenTypes.contains(p.type);
    }
    private boolean major(WorldAtlas.Place p){
        return p.type.equals("Столица")||p.name.equals("Воронья Скала");
    }
    private boolean player(WorldAtlas.Place p){
        return p.name.equals(game.progress.location);
    }
    private float screenX(WorldAtlas.Place p){return left+positionX(p)*scale;}
    private float screenY(WorldAtlas.Place p){return top+positionY(p)*scale;}

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawColor(0xff091011);
        Bitmap image=terrain(getContext());
        if(image!=null){
            terrainPaint.setAlpha(255);
            canvas.drawBitmap(image,null,new RectF(left,top,
                left+MAP_W*scale,top+MAP_H*scale),terrainPaint);
        }
        // Dark map labels stay legible even over snowy terrain.
        float density=getResources().getDisplayMetrics().density;
        float zoom=scale/Math.max(.0001f,minScale);
        float radius=12f*density;
        if(zoom>=1.6f){
            // Multiple real places fall in the same coarse map square. Show
            // a cluster count instead of claiming fabricated exact coordinates.
            for(Map.Entry<String,ArrayList<SkyrimMapCatalog.Site>> pair:catalogClusters().entrySet()){
                ArrayList<SkyrimMapCatalog.Site> list=pair.getValue();
                if(list.isEmpty())continue;
                SkyrimMapCatalog.Site site=list.get(0);
                float x=left+site.x*scale,y=top+site.y*scale;
                if(x<-60||y<-40||x>getWidth()+60||y>getHeight()+40)continue;
                float r=(zoom>4f?15f:12f)*density;
                pinPaint.setStyle(Paint.Style.FILL);
                pinPaint.setColor(0xe60d1519);
                canvas.drawCircle(x,y,r+2*density,pinPaint);
                pinPaint.setColor(0xffc9ced1);
                canvas.drawCircle(x,y,r,pinPaint);
                pinPaint.setColor(0xff182124);
                pinPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                pinPaint.setTextSize(10*density);
                String count=String.valueOf(list.size());
                float width=pinPaint.measureText(count);
                canvas.drawText(count,x-width/2f,y+3*density,pinPaint);
                if(zoom>=5.6f){
                    pinPaint.setTypeface(SovngardeTypeface.face(getContext(),false));
                    pinPaint.setTextSize(12*density);
                    pinPaint.setColor(0xffdfe5e6);
                    canvas.drawText(site.grid+" · "+count+" мест",x+r+5*density,y,pinPaint);
                }
            }
        }
        for(WorldAtlas.Place p:WorldAtlas.all()){
            if(!visible(p))continue;
            boolean selected=player(p),capital=major(p);
            if(zoom<1.5f&&!selected&&!capital)continue;
            if(zoom<2.5f&&!selected&&!capital&&!p.type.equals("Деревня")
                    &&!p.type.equals("Поселение"))continue;
            float x=screenX(p),y=screenY(p);
            if(x<-100||y<-60||x>getWidth()+100||y>getHeight()+60)continue;
            float size=(selected?28:capital?23:17)*density;
            String symbol=selected?"маркер игрока":capital?"столицы владений":
                p.type.contains("Пещера")?"пещеры":
                p.type.contains("Руины")?"нордские руины":
                p.type.contains("Форт")?"крепости":
                p.type.contains("Деревня")?"деревни":
                p.type.contains("Святилище")?"даэдрические святилища":"обнаруженные места";
            int color=selected?0xfff5df9b:capital?0xffe4d9c4:0xffc5cbd0;
            if(!SkyrimIconAtlas.drawMarker(getContext(),canvas,symbol,color,x,y,size)){
                pinPaint.setStyle(Paint.Style.FILL);pinPaint.setColor(0xff0b1315);
                canvas.drawCircle(x,y,radius*.74f,pinPaint);
                pinPaint.setColor(color);canvas.drawCircle(x,y,radius*.48f,pinPaint);
            }
            if(selected||capital||(zoom>=4.2f)){
                pinPaint.setTypeface(SovngardeTypeface.face(getContext(),selected||capital));
                pinPaint.setTextSize((selected?17:capital?15:13)*density);
                pinPaint.setStyle(Paint.Style.STROKE);
                pinPaint.setStrokeWidth(3f*density);
                pinPaint.setColor(0xe3091014);
                canvas.drawText(p.name,x+radius+4*density,y-4*density,pinPaint);
                pinPaint.setStyle(Paint.Style.FILL);pinPaint.setColor(color);
                canvas.drawText(p.name,x+radius+4*density,y-4*density,pinPaint);
            }
            if(selected){
                pinPaint.setStyle(Paint.Style.STROKE);
                pinPaint.setColor(0xfff5df9b);pinPaint.setStrokeWidth(1.7f*density);
                canvas.drawCircle(x,y,radius*1.4f,pinPaint);
                pinPaint.setStyle(Paint.Style.FILL);
            }
        }
        // Small zoom indication. Does not intercept touch or cover map controls.
        pinPaint.setTypeface(android.graphics.Typeface.DEFAULT);
        pinPaint.setStyle(Paint.Style.FILL);
        pinPaint.setTextSize(10*density);
        pinPaint.setColor(0xfff1f1e9);
        canvas.drawText(String.format(java.util.Locale.ROOT,"×%.1f",zoom),
            12*density,getHeight()-13*density,pinPaint);
    }

    private void select(float x,float y){
        WorldAtlas.Place best=null;float bestDistance=Float.MAX_VALUE;
        float radius=35f*getResources().getDisplayMetrics().density;
        for(WorldAtlas.Place p:WorldAtlas.all()){
            if(!visible(p))continue;
            float dx=x-screenX(p),dy=y-screenY(p);
            float distance=dx*dx+dy*dy;
            if(distance<bestDistance){bestDistance=distance;best=p;}
        }
        ArrayList<SkyrimMapCatalog.Site> catalogBest=null;
        float catalogDistance=Float.MAX_VALUE;
        if(scale/Math.max(.0001f,minScale)>=1.6f){
            for(ArrayList<SkyrimMapCatalog.Site> list:catalogClusters().values()){
                if(list.isEmpty())continue;
                SkyrimMapCatalog.Site p=list.get(0);
                float sx=left+p.x*scale,sy=top+p.y*scale;
                float dx=x-sx,dy=y-sy,distance=dx*dx+dy*dy;
                if(distance<catalogDistance){catalogDistance=distance;catalogBest=list;}
            }
        }
        if(catalogBest!=null&&catalogDistance<radius*radius
                &&(best==null||catalogDistance<bestDistance)&&catalogCallback!=null)
            catalogCallback.selected(catalogBest);
        else if(best!=null&&bestDistance<radius*radius&&callback!=null)
            callback.selected(best);
        performClick();
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            dragged=false;touchX=event.getX();touchY=event.getY();
            ViewParent parent=getParent();
            if(parent!=null)parent.requestDisallowInterceptTouchEvent(true);
        }
        scaler.onTouchEvent(event);
        gesture.onTouchEvent(event);
        if(action==MotionEvent.ACTION_POINTER_DOWN)dragged=true;
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
            persist();
            ViewParent parent=getParent();
            if(parent!=null)parent.requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
