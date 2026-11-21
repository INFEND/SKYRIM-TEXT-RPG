package dev.infend.skyrimtext;

/** Streaming JSON string decoder. No Android, no untrusted JSON is executed or applied. */
final class AiScenePreview {
    private AiScenePreview(){}
    private static final int LIMIT=30000;
    // Completion begins after the assistant prefix, which can open Qwen thinking.
    static String directPrompt(String formatted){
        int open=formatted.lastIndexOf("<think>");
        if(open>=0&&formatted.substring(open+7).trim().isEmpty())
            return formatted+"\n</think>\n\n";
        // Qwen chat templates can omit the opening thinking tag, in which case
        // the model itself emits it. Prefill an empty block before any answer.
        if(formatted.trim().endsWith("<|im_start|>assistant"))
            return formatted+(formatted.endsWith("\n")?"":"\n")+"<think>\n\n</think>\n\n";
        return formatted;
    }
    static String scenePrefix(String formatted,boolean scene){
        if(!scene)return "";
        String tail=formatted.trim();
        if(tail.endsWith("<|im_start|>assistant")||tail.endsWith("</think>"))
            return "{\"scene\":\"";
        return "";
    }
    static String read(String partial){
        if(partial==null||partial.isEmpty())return "";
        int start=0;
        // Qwen-style explicit thinking prefix: do not expose internal deliberation.
        int thought=partial.indexOf("<think>");
        int close=partial.indexOf("</think>",Math.max(0,thought));
        if(thought>=0&&close<0)return "";
        if(close>=0)start=close+8;
        start=partial.indexOf('{',start);
        if(start<0)return "";
        int depth=0;
        char previous=0;
        for(int i=start;i<partial.length();i++){
            char c=partial.charAt(i);
            if(Character.isWhitespace(c))continue;
            if(c=='{'||c=='['){depth++;previous=c;continue;}
            if(c=='}'||c==']'){depth=Math.max(0,depth-1);previous=c;continue;}
            if(c=='"'){
                int end=quotedEnd(partial,i);
                if(end<0)return "";
                if(depth==1&&(previous=='{'||previous==',')&&partial.regionMatches(i+1,"scene",0,5)&&end==i+6){
                    int colon=skipSpace(partial,end+1);
                    if(colon>=partial.length()||partial.charAt(colon)!=':')return "";
                    int value=skipSpace(partial,colon+1);
                    if(value>=partial.length()||partial.charAt(value)!='"')return "";
                    return decode(partial,value+1);
                }
                i=end;previous='"';continue;
            }
            previous=c;
        }
        return "";
    }
    private static int skipSpace(String s,int i){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;return i;}
    private static int quotedEnd(String s,int start){
        boolean escaped=false;
        for(int i=start+1;i<s.length();i++){
            char c=s.charAt(i);
            if(escaped)escaped=false;
            else if(c=='\\')escaped=true;
            else if(c=='"')return i;
        }
        return -1;
    }
    private static String decode(String s,int start){
        StringBuilder out=new StringBuilder();
        for(int i=start;i<s.length()&&out.length()<LIMIT;i++){
            char c=s.charAt(i);
            if(c=='"')break;
            if(c=='\\'){
                if(++i>=s.length())break;
                switch(s.charAt(i)){
                    case '"':c='"';break;case '\\':c='\\';break;case '/':c='/';break;
                    case 'n':c='\n';break;case 'r':c='\r';break;case 't':c='\t';break;
                    case 'b':c='\b';break;case 'f':c='\f';break;
                    case 'u':
                        if(i+4>=s.length())return safe(out);
                        int value=0;
                        for(int j=1;j<=4;j++){
                            int digit=Character.digit(s.charAt(i+j),16);
                            if(digit<0)return safe(out);
                            value=(value<<4)|digit;
                        }
                        c=(char)value;i+=4;break;
                    default:return safe(out);
                }
            }
            out.append(c);
        }
        return safe(out);
    }
    private static String safe(StringBuilder out){
        // A partial Unicode escape may end at a high surrogate; wait for its pair.
        if(out.length()>0&&Character.isHighSurrogate(out.charAt(out.length()-1)))out.setLength(out.length()-1);
        return out.toString();
    }
}
