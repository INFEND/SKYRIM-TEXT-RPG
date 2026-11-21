package dev.infend.skyrimtext;
final class AiModelCatalog {
    static final String[] IDS={"gpt-6-luna","gpt-6.1-terra","gpt-6.1-sol","gpt-6-astra"};
    static final String[] LABELS={"GPT-6 Luna | экономный","GPT-6.1 Terra | быстрый",
        "GPT-6.1 Sol | глубокий сюжет","GPT-6 Astra | максимум качества"};
    static final String DEFAULT_API="gpt-6.1-terra";
    private AiModelCatalog(){}
    static int position(String value){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(value))return i;return 1;}
    static String label(String value){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(value))return LABELS[i];return value;}
}
