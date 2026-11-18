package dev.infend.skyrimtext;

/** Read-only, save-scoped store facts shared by online and offline prompts. */
final class FixShopAiContext {
    static final String RULE="Дары/печати: только перечисленные активны. Механика, расходы и лимиты принадлежат движку; не имитируй их через effects, не выдавай покупки. XP навыка указывай без бонуса. Используй только доступные кнопки.";
    private static final String[] G={"повтор провала/день","спасение 35% HP/день","нет яда/мороза","навык XP+25%","страховка золота/день","якорь/день","удалённый сундук","очищение/день","HP20%→MP40%/бой","пропуск атаки/бой","щит 50/бой","вампиризм 10%,≤15","первый удар +50%,≤40","магия −30% цены","сильный удар −30% цены","побег WILD/день","переправа 0","путь −1/3","кузница везде","половина сил/день","штраф в столице/день","золото победы +25%","MP→силы/бой","защита +10% сил/бой"};
    private static final String[] S={"контратака после защиты/бой","магия→щит 20/бой","возврат 3 MP","добивание WILD ≤10%","зелья +25%","припасы −15%","материал/день","случайные встречи ×0.5","дом/день","3 комплекта","навык +100 XP/день","сон ≥6ч +25% сил"};
    static boolean combatAvailable(GameEngine g,String action){
        if(!"COMBAT".equals(g.mode)||g.enemyHp<=0)return false;
        FixShopGifts f=g.fixShop.gifts;
        switch(action){case "FIXGIFT:blood":return f.canBlood(g);case "FIXGIFT:time":return f.availableBattle(FixShopGifts.TIME);case "FIXGIFT:balance":return f.canBalance(g);case "FIXGIFT:escape":return f.canEscape(g);default:return false;}
    }
    static String giftStatus(GameEngine g,int i){
        FixShopGifts f=g.fixShop.gifts;
        if(!f.on(i))return "выкл";
        if(f.status(i,g).equals("Использовано сегодня"))return "исчерпан сегодня";
        if("COMBAT".equals(g.mode)){
            if(i==FixShopGifts.BLOOD&&f.bloodUsed)return "исчерпан в бою";
            if((i==FixShopGifts.TIME||i==FixShopGifts.AEGIS||i==FixShopGifts.AMBUSH||i==FixShopGifts.BALANCE||i==FixShopGifts.RESOLVE)&&!f.availableBattle(i))return "исчерпан в бою";
        }
        return "вкл";
    }
    static String sealStatus(GameEngine g,int i){
        FixShopSeals f=g.fixShop.seals;if(!f.on(i))return "выкл";
        if(f.status(i,g).equals("Использовано сегодня"))return "исчерпан сегодня";
        if("COMBAT".equals(g.mode)){
            if(i==FixShopSeals.RIPOSTE)return f.riposteUsed?"исчерпан в бою":f.riposteReady?"контратака готова":"после защиты";
            if(i==FixShopSeals.MANA_WARD&&f.wardUsed)return "исчерпан в бою";
        }
        return "вкл";
    }
    static String context(GameEngine g,boolean compact){
        FixShopGifts f=g.fixShop.gifts;FixShopSeals s=g.fixShop.seals;
        if(f.owned==0&&s.owned==0)return "Дары/печати не куплены. Не предлагай и не применяй их.";
        StringBuilder b=new StringBuilder(compact?"Только эти дары/печати; эффекты движка, не начисляй повторно. ":"ДАРЫ И ПЕЧАТИ (текущее сохранение). "+RULE+"\n");
        int off=0;
        for(int i=0;i<FixShopGifts.NAME.length;i++)if(f.owns(i)){
            if(compact&&!f.on(i)){off++;continue;}
            append(b,FixShopGifts.NAME[i],giftStatus(g,i),compact?G[i]:FixShopGifts.DESC[i],compact);
        }
        for(int i=0;i<FixShopSeals.NAME.length;i++)if(s.owns(i)){
            if(compact&&!s.on(i)){off++;continue;}
            append(b,FixShopSeals.NAME[i],sealStatus(g,i),compact?S[i]:FixShopSeals.DESC[i],compact);
        }
        if(off>0)b.append("Выключено: ").append(off).append(". ");
        if(f.on(FixShopGifts.ANCHOR)&&!f.anchor.isEmpty())b.append("Якорь: ").append(f.anchor).append(". ");
        if(s.on(FixShopSeals.ARSENAL)){int n=0;for(org.json.JSONObject set:s.sets)if(set!=null)n++;b.append("Комплектов: ").append(n).append("/3. ");}
        if("COMBAT".equals(g.mode)){b.append("Доступные спецкоманды: ");for(String a:new String[]{"FIXGIFT:blood","FIXGIFT:time","FIXGIFT:balance","FIXGIFT:escape"})if(combatAvailable(g,a))b.append(a).append(' ');}
        return b.toString();
    }
    private static void append(StringBuilder b,String name,String status,String effect,boolean compact){
        b.append(name).append(compact?": ":" | ").append(effect);
        if(!compact||!"вкл".equals(status))b.append(" (").append(status).append(')');
        b.append(compact?"; ":"\n");
    }
    /** Stage a reroll; never mutate a live game before an atomic save. */
    static boolean mayRetry(GameEngine g,int chance,int first,boolean alreadyStaged){
        return !alreadyStaged&&g.fixShop.gifts.on(FixShopGifts.FATE)&&g.fixShop.gifts.fateDay!=g.day&&chance>0&&chance<100&&first>chance;
    }
    static void commitRetry(GameEngine g,int day){
        if(day==g.day&&g.fixShop.gifts.on(FixShopGifts.FATE)&&g.fixShop.gifts.fateDay!=day){g.fixShop.gifts.fateDay=day;g.recordVisualEvent("Благосклонность судьбы: повтор проверки ИИ");}
    }
}
