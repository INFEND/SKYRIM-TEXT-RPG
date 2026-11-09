package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Stable world-anchored markers shared by the renderer and hit-testing.
 * No screen-cell clusters or numbers. Collision decisions depend ONLY on
 * world coordinates, zoom, marker priority and filters: panning cannot cause
 * any marker to move between groups or change its visible identity.
 */
public final class MapPinLayout {
    public static final class Pin {
        public final ArrayList<SkyrimMapCatalog.Site> sites=new ArrayList<>(1);
        public float x,y;
        public final boolean player,focus;
        public final int priority;
        public final float sizeDp;
        public final float touchRadius;
        private Pin(SkyrimMapCatalog.Site site,boolean player,boolean focus,
                    int priority,float sizeDp,float x,float y,float density){
            sites.add(site);
            this.player=player;this.focus=focus;this.priority=priority;
            this.sizeDp=sizeDp;this.x=x;this.y=y;
            touchRadius=Math.max(10*density,sizeDp*density*.69f);
        }
    }
    private static final class Candidate {
        final SkyrimMapCatalog.Site site;
        final boolean player,focus;
        final int priority;
        Candidate(SkyrimMapCatalog.Site site,boolean player,boolean focus){
            this.site=site;this.player=player;this.focus=focus;
            priority=priority(site,player,focus);
        }
    }
    private MapPinLayout(){}

    private static int priority(SkyrimMapCatalog.Site s,boolean player,boolean focus){
        if(player)return 1000;
        if(focus)return 950;
        if(s.capital())return 850;
        String t=s.type;
        if(t.contains("Деревня")||t.contains("Поселение"))return 720;
        if(t.contains("Форт")||t.contains("Крепость"))return 640;
        if(t.contains("Башня")||t.contains("Святилище"))return 540;
        if(t.contains("Двемер")||t.contains("Руины"))return 450;
        if(t.contains("Пещера")||t.contains("Шахта"))return 400;
        return 330;
    }
    /** Stable smooth zoom response, capped to readable physical dp sizes. */
    public static float sizeDp(float zoom,int priority){
        float z=Math.max(1f,Math.min(14f,zoom));
        float t=(float)(Math.log(z)/Math.log(14d));
        float result=10f+16f*t;
        if(priority>=1000)return 18f+15f*t;
        if(priority>=950)return 16f+15f*t;
        if(priority>=850)return 14f+15f*t;
        if(priority>=720)return 11f+16f*t;
        return result;
    }
    private static long key(int x,int y){
        return ((long)x<<32)^(y&0xffffffffL);
    }

    /** Compatibility overload for Java callers supplying the original signature. */
    public static ArrayList<Pin> build(List<SkyrimMapCatalog.Site> sites,int sheet,
            int region,Set<String> hidden,String playerId,String focusId,
            float scale,float left,float top,float width,float height,float density){
        return build(sites,sheet,region,hidden,playerId,focusId,
            scale,left,top,width,height,density,Math.max(1f,scale));
    }
    public static ArrayList<Pin> build(List<SkyrimMapCatalog.Site> sites,int sheet,
            int region,Set<String> hidden,String playerId,String focusId,
            float scale,float left,float top,float width,float height,float density,float zoom){
        ArrayList<Candidate> candidates=new ArrayList<>();
        for(SkyrimMapCatalog.Site site:sites){
            if(site.sheet!=sheet)continue;
            boolean player=site.id.equals(playerId),focus=site.id.equals(focusId);
            if(!player&&!focus&&
                ((region>=0&&site.region!=region)||(hidden!=null&&hidden.contains(site.type))))
                continue;
            if(!Float.isFinite(site.x)||!Float.isFinite(site.y))continue;
            candidates.add(new Candidate(site,player,focus));
        }
        // World coordinates and a stable ID decide tie breaks, never screen X/Y
        // or input ordering, so the same selection survives any camera translation.
        candidates.sort(Comparator.<Candidate>comparingInt(c->c.priority).reversed()
            .thenComparing(c->c.site.id));
        ArrayList<Pin> shown=new ArrayList<>();
        HashMap<Long,ArrayList<Pin>> buckets=new HashMap<>();
        // At most 33dp marker + 4dp spacing; test 3x3 neighboring cells.
        final float cell=42f*Math.max(.1f,density);
        for(Candidate c:candidates){
            float mapX=c.site.x*scale,mapY=c.site.y*scale;
            int cx=(int)Math.floor(mapX/cell),cy=(int)Math.floor(mapY/cell);
            float size=sizeDp(zoom,c.priority);
            boolean overlaps=false;
            for(int gx=cx-1;gx<=cx+1&&!overlaps;gx++){
                for(int gy=cy-1;gy<=cy+1&&!overlaps;gy++){
                    ArrayList<Pin> bucket=buckets.get(key(gx,gy));
                    if(bucket==null)continue;
                    for(Pin other:bucket){
                        SkyrimMapCatalog.Site anchor=other.sites.get(0);
                        float dx=mapX-anchor.x*scale,dy=mapY-anchor.y*scale;
                        float gap=(size+other.sizeDp)*density*.5f+3f*density;
                        if(dx*dx+dy*dy<gap*gap){overlaps=true;break;}
                    }
                }
            }
            if(overlaps)continue;
            Pin pin=new Pin(c.site,c.player,c.focus,c.priority,size,
                left+mapX,top+mapY,density);
            shown.add(pin);
            buckets.computeIfAbsent(key(cx,cy),unused->new ArrayList<>()).add(pin);
        }
        return shown;
    }

    /** Translation updates positions without rebuilding visibility or hierarchy. */
    public static void translate(List<Pin> pins,float dx,float dy){
        if(pins==null||(!Float.isFinite(dx))||(!Float.isFinite(dy)))return;
        for(Pin pin:pins){pin.x+=dx;pin.y+=dy;}
    }
    /** Hit-test only the icons actually drawn, using their individual sizes. */
    public static Pin hit(List<Pin> pins,float x,float y,float radius){
        Pin best=null;float nearest=Float.MAX_VALUE;
        for(Pin pin:pins){
            float allowance=Math.min(radius,pin.touchRadius);
            float dx=x-pin.x,dy=y-pin.y,distance=dx*dx+dy*dy;
            if(distance<=allowance*allowance&&
                (distance<nearest||(distance==nearest&&best!=null&&pin.priority>best.priority))){
                nearest=distance;best=pin;
            }
        }
        return best;
    }
}
