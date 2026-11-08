package dev.infend.skyrimtext;

/** Model reasoning budget shared by ChatGPT OAuth and OpenAI API. */
final class AiReasoningEffort {
    static final String DEFAULT = "auto";
    static final String[] VALUES={"auto","low","medium","high","xhigh","max"};
    static final String[] LABELS={
        "Автоматически · выбор модели",
        "Низкий · быстрее",
        "Средний · сбалансированный",
        "Высокий · глубже",
        "Очень высокий · сложные истории",
        "Максимальный · самое глубокое размышление"
    };

    private AiReasoningEffort(){}

    static String normalize(String value) {
        if(value!=null)
            for(String supported:VALUES)if(supported.equals(value))return supported;
        return DEFAULT;
    }

    static int position(String value){
        String normalized=normalize(value);
        for(int i=0;i<VALUES.length;i++)
            if(VALUES[i].equals(normalized))return i;
        return 0;
    }

    static String label(String value){return LABELS[position(value)];}

    static boolean isAutomatic(String value){return DEFAULT.equals(normalize(value));}

    /** Reasoning tokens are part of the API output budget; reserve enough for narration. */
    static int apiOutputLimit(String value) {
        switch(normalize(value)) {
            case "low":return 3200;
            case "high":return 9000;
            case "xhigh":return 14000;
            case "max":return 20000;
            case "medium":return 5500;
            default:return 5500;
        }
    }

    /** Deep reasoning can take longer before the first streamed text token appears. */
    static int networkReadTimeoutMs(String value){
        switch(normalize(value)){
            case "max":
            case "xhigh":return 300000;
            case "high":return 210000;
            default:return 120000;
        }
    }

    static boolean modelSupportsEffort(String model,String value){
        String effort=normalize(value);
        if("auto".equals(effort))return true;
        // Known GPT-6 text models support these five common effort levels.
        if("gpt-6-luna".equals(model)||"gpt-6.1-terra".equals(model)
                ||"gpt-6.1-sol".equals(model)||"gpt-6-astra".equals(model))
            return true;
        // Dynamic catalog models can be different; let server validate them.
        return true;
    }
}
