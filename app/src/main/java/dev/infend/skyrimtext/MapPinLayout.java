package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** One immutable snapshot feeds both drawing and hit testing. Android-free. */
public final class MapPinLayout {
    public static final class Pin {
        public final ArrayList<SkyrimMapCatalog.Site> sites=new ArrayList<>();
        public float x,y;
        public boolean player,focus;
        private float sumX,sumY;
        void add(SkyrimMapCatalog.Site site,float sx,float sy){
            sites.add(site);sumX+=sx;sumY+=sy;x=sumX/sites.size();y=sumY/sites.size();
        }
    }
    private MapPinLayout(){}
    public static ArrayList<Pin> build(List<SkyrimMapCatalog.Site> sites,int sheet,
            int region,Set<String> hidden,String playerId,String focusId,
            float scale,float left,float top,float width,float height,float density){
        LinkedHashMap<String,Pin> groups=new LinkedHashMap<>();
        float cell=40f*density;
        for(SkyrimMapCatalog.Site site:sites){
            if(site.sheet!=sheet)continue;
            boolean player=site.id.equals(playerId),focus=site.id.equals(focusId);
            if(!player&&!focus&&((region>=0&&site.region!=region)||hidden.contains(site.type)))continue;
            float x=left+site.x*scale,y=top+site.y*scale;
            if(!Float.isFinite(x)||!Float.isFinite(y)||x<-45*density||y<-45*density||
                x>width+45*density||y>height+45*density)continue;
            String key=player||focus||site.capital()?site.id:
                ((int)Math.floor(x/cell))+":"+((int)Math.floor(y/cell));
            Pin pin=groups.get(key);
            if(pin==null){pin=new Pin();groups.put(key,pin);}
            pin.add(site,x,y);pin.player|=player;pin.focus|=focus;
        }
        return new ArrayList<>(groups.values());
    }
    public static Pin hit(List<Pin> pins,float x,float y,float radius){
        Pin best=null;float distance=radius*radius;
        for(Pin pin:pins){
            float dx=x-pin.x,dy=y-pin.y,d=dx*dx+dy*dy;
            if(d<distance||(d==distance&&pin.player)){
                distance=d;best=pin;
            }
        }
        return best;
    }
}
