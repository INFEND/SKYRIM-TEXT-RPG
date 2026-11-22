package dev.infend.skyrimtext;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Only uncontested, low-stakes actions can skip a dice roll.
 * This never promises an NPC's cooperation, a secret, or a reward.
 * Composite, risky and ambiguous commands still use the normal AI check.
 */
final class AiRoutineAction {
    private static final Pattern ROUTINE=Pattern.compile(
        "^(?:я\\s+)?(?:подойти|подхожу|отойти|отхожу|уйти|ухожу|поговорить|разговариваю|спросить|спрашиваю|осмотреться|осматриваюсь|оглядеться|оглядываюсь)(?:\\s+[^.!?;:]{1,100})?[.!?]?$",
        Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    private static final Pattern REMAINS=Pattern.compile(
        "^(?:я\\s+)?обыскать\\s+(?:труп|тело|останки)(?:\\s+[^.!?;:]{1,60})?[.!?]?$",
        Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    // Android may reject Java's inline Unicode-character-class flag on the first typed action.
    // Unicode letter boundaries and explicit flags preserve Russian risk detection.
    private static final Pattern RISK=Pattern.compile(
        "(?<!\\p{L})(?:враг|вражд|бой|боя|бою|драк|драугр|дракон|опасн|заперт|закрыт|замк|страж|охран|скрыт|скрытн|тайн|незамет|воров|краст|украст|ограб|обман|угроз|шантаж|насил|убий|убить|напад|побег|заклин|магич|взлом|ловушк|секрет)\\p{L}*(?!\\p{L})",
        Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    private AiRoutineAction(){}

    static boolean requiresNoCheck(String request){
        if(request==null)return false;
        String action=request.trim().toLowerCase(Locale.ROOT).replace('ё','е');
        if(action.isEmpty()||action.length()>140||action.indexOf('\n')>=0
            ||action.indexOf(',')>=0||action.contains(" и ")||action.contains(" затем ")
            ||action.contains(" чтобы ")||action.contains(" после ")||action.contains(" если ")
            ||action.contains(" или ")||RISK.matcher(action).find())return false;
        return ROUTINE.matcher(action).matches()||REMAINS.matcher(action).matches();
    }
}
