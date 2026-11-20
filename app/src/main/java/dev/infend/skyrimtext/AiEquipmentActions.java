package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

/**
 * AI-authored gear uses the existing durable custom-equipment slots.
 * Every proposed operation is validated before any inventory or game mutation.
 */
final class AiEquipmentActions {
    static final class Change {
        final boolean create;
        final int slot,type,stat;
        final String name,description;
        final double weight;
        Change(boolean create,int slot,int type,int stat,double weight,String name,String description){
            this.create=create;this.slot=slot;this.type=type;this.stat=stat;this.weight=weight;
            this.name=name;this.description=description;
        }
    }
    private AiEquipmentActions(){}
    private static int integer(JSONObject o,String field,int min,int max)throws JSONException{
        Object v=o.opt(field);
        if(!(v instanceof Number))throw new JSONException(field+" должен быть числом");
        double n=((Number)v).doubleValue();
        if(!Double.isFinite(n)||n!=Math.rint(n)||n<min||n>max)
            throw new JSONException(field+" вне диапазона");
        return (int)n;
    }
    private static double weight(JSONObject o)throws JSONException{
        Object v=o.opt("weight");
        if(!(v instanceof Number))throw new JSONException("Вес должен быть числом");
        double n=((Number)v).doubleValue();
        if(!Double.isFinite(n)||n<.01||n>50)
            throw new JSONException("Вес предмета вне диапазона 0.01–50 кг");
        return n;
    }
    private static String text(JSONObject o,String field,int min,int max)throws JSONException{
        String value=o.optString(field,"").trim();
        if(value.length()<min||value.length()>max)throw new JSONException("Неверное поле "+field);
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))
            throw new JSONException("Недопустимый символ в "+field);
        return value;
    }
    static ArrayList<Change> prepare(GameEngine g,JSONObject effects,
                                      Map<String,Integer> inventoryDelta)throws JSONException{
        ArrayList<Change> out=new ArrayList<>();
        if(!effects.has("equipmentChanges")||effects.isNull("equipmentChanges"))return out;
        if(!(effects.opt("equipmentChanges") instanceof JSONArray))
            throw new JSONException("Изменения снаряжения должны быть массивом");
        JSONArray source=effects.getJSONArray("equipmentChanges");
        if(source.length()>3)throw new JSONException("Не более трёх предметов за ход");
        boolean[] used=g.compensation.owned.clone();
        HashSet<Integer> touched=new HashSet<>();
        HashSet<String> taken=new HashSet<>();
        for(int i=0;i<CompensationPowers.SLOTS;i++){
            String name=g.compensation.itemNames[i];
            if(name!=null&&!name.isEmpty())taken.add(name.toLowerCase(Locale.ROOT));
        }
        double resultingWeight=g.progress.carryWeight();
        for(Map.Entry<String,Integer> delta:inventoryDelta.entrySet()){
            ItemCatalog.Item item=ItemCatalog.find(delta.getKey());
            if(item!=null)resultingWeight+=item.weight*delta.getValue();
        }
        for(int i=0;i<source.length();i++){
            JSONObject row=source.optJSONObject(i);
            if(row==null)throw new JSONException("Неверная запись снаряжения");
            String mode=row.optString("mode","");
            if(!"create".equals(mode)&&!"modify".equals(mode))
                throw new JSONException("mode должен быть create либо modify");
            boolean create="create".equals(mode);
            int slot;
            if(create){
                if(row.has("slot"))throw new JSONException("При создании слот не задаётся");
                slot=-1;
                for(int j=3;j<CompensationPowers.SLOTS;j++)if(!used[j]){slot=j;break;}
                if(slot<0)throw new JSONException("Слоты собственного снаряжения закончились");
                used[slot]=true;
            }else{
                slot=integer(row,"slot",3,CompensationPowers.SLOTS-1);
                if(!g.compensation.owned[slot]||!touched.add(slot))
                    throw new JSONException("Предмет для изменения не найден");
                taken.remove(g.compensation.itemNames[slot].toLowerCase(Locale.ROOT));
            }
            String name=text(row,"name",2,60);
            String description=text(row,"description",0,400);
            int type=integer(row,"type",0,2),stat=integer(row,"stat",0,500);
            double newWeight=weight(row);
            if(!create&&g.compensation.itemType[slot]!=type)
                throw new JSONException("Тип существующего предмета менять нельзя");
            if(ItemCatalog.find(name)!=null||!taken.add(name.toLowerCase(Locale.ROOT)))
                throw new JSONException("Название предмета уже используется");
            if(!touched.add(slot)&&create)throw new JSONException("Повтор предмета");
            resultingWeight+=newWeight-(create?0:g.compensation.itemWeight[slot]);
            if(resultingWeight<0||resultingWeight>g.progress.carryLimit()+0.00001)
                throw new JSONException("Не хватает места для нового снаряжения");
            out.add(new Change(create,slot,type,stat,newWeight,name,description));
        }
        return out;
    }
    static void commit(GameEngine g,ArrayList<Change> changes)throws JSONException{
        for(Change c:changes){
            boolean applied=c.create?
                g.compensation.createItem(g,c.name,c.type,c.stat,c.weight,c.description):
                g.compensation.updateCustomItem(g,c.slot,c.name,c.type,c.stat,c.weight,c.description);
            if(!applied)throw new JSONException("Не удалось применить авторское снаряжение");
        }
    }
}
