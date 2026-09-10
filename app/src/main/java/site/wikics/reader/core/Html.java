package site.wikics.reader.core;

import java.util.*;
import java.util.regex.*;
import java.util.function.Predicate;

/** Small, bounded HTML tree reader for the server-rendered MediaWiki markup.
 * It never executes markup. Reader output is separately allow-listed. */
public final class Html {
    private Html() {}
    private static final Set<String> VOID = new HashSet<>(Arrays.asList(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"));
    private static final Pattern ATTR = Pattern.compile("([^\\s=/>]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+)))?");
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z][a-zA-Z0-9]+);");
    private static final Map<String,String> ENTITIES = new HashMap<>();
    static {
        String[][] pairs = {{"amp","&"},{"lt","<"},{"gt",">"},{"quot","\""},{"apos","'"},{"nbsp"," "},
            {"ndash","–"},{"mdash","—"},{"hellip","…"},{"laquo","«"},{"raquo","»"},{"times","×"},
            {"middot","·"},{"copy","©"},{"le","≤"},{"ge","≥"},{"ne","≠"},{"rarr","→"},
            {"lsquo","‘"},{"rsquo","’"},{"ldquo","“"},{"rdquo","”"},{"minus","−"},{"plusmn","±"}};
        for (String[] p : pairs) ENTITIES.put(p[0],p[1]);
    }
    public static final class Node {
        public final String tag;
        public String data = "";
        public final Map<String,String> attrs = new LinkedHashMap<>();
        public final List<Node> children = new ArrayList<>();
        public Node parent;
        Node(String tag) { this.tag=tag; }
        public String attr(String key) { return attrs.getOrDefault(key, ""); }
        public boolean hasClass(String value) { return Arrays.asList(attr("class").split("\\s+")).contains(value); }
        public String text() { StringBuilder b=new StringBuilder(); appendText(b); return b.toString().replace('\u00a0',' ').replaceAll("\\s+"," ").trim(); }
        private void appendText(StringBuilder b) {
            if (tag.equals("#text")) { b.append(data); return; }
            if (tag.equals("script") || tag.equals("style")) return;
            for (Node n:children) n.appendText(b);
            if (Arrays.asList("p","div","li","tr","td","th","br","h1","h2","h3","h4").contains(tag)) b.append(' ');
        }
        public List<Node> all(Predicate<Node> predicate) { List<Node> r=new ArrayList<>(); walk(this,predicate,r); return r; }
        public Node first(Predicate<Node> predicate) { if(predicate.test(this))return this; for(Node n:children){Node r=n.first(predicate);if(r!=null)return r;}return null; }
        public Node ancestor(String tag) { for(Node n=parent;n!=null;n=n.parent)if(n.tag.equals(tag))return n;return null; }
    }
    private static void walk(Node n,Predicate<Node> p,List<Node> out) { if(p.test(n))out.add(n);for(Node c:n.children)walk(c,p,out); }
    public static Node parse(String html) {
        if(html.length()>6_000_000)throw new IllegalArgumentException("Страница слишком большая");
        Node root=new Node("#document"); Deque<Node> stack=new ArrayDeque<>();stack.push(root);
        int i=0, count=0; String lower=html.toLowerCase(Locale.ROOT);
        while(i<html.length()) {
            if(++count>200_000)throw new IllegalArgumentException("Слишком сложная страница");
            if(html.startsWith("<!--",i)){int e=html.indexOf("-->",i+4);i=e<0?html.length():e+3;continue;}
            if(html.charAt(i)!='<') {int e=html.indexOf('<',i);if(e<0)e=html.length();Node t=new Node("#text");t.data=decode(html.substring(i,e));add(stack.peek(),t);i=e;continue;}
            int e=i+1;char quote=0;
            for(;e<html.length();e++){char c=html.charAt(e);if(quote!=0){if(c==quote)quote=0;}else if(c=='\''||c=='"')quote=c;else if(c=='>')break;}
            if(e==html.length())break;
            String token=html.substring(i+1,e).trim();i=e+1;
            if(token.isEmpty()||token.startsWith("!")||token.startsWith("?"))continue;
            boolean closing=token.startsWith("/");if(closing)token=token.substring(1).trim();
            int k=0;while(k<token.length()&&(Character.isLetterOrDigit(token.charAt(k))||token.charAt(k)=='-'||token.charAt(k)==':'))k++;
            if(k==0)continue;String tag=token.substring(0,k).toLowerCase(Locale.ROOT);
            if(closing){boolean found=false;for(Node n:stack)if(n.tag.equals(tag)){found=true;break;}if(found)while(stack.size()>1){if(stack.pop().tag.equals(tag))break;}continue;}
            // Common optional end tags used by MediaWiki tables and lists.
            if((tag.equals("td")||tag.equals("th"))&&(stack.peek().tag.equals("td")||stack.peek().tag.equals("th")))stack.pop();
            if((tag.equals("p")||tag.equals("li")||tag.equals("tr"))&&stack.peek().tag.equals(tag))stack.pop();
            Node node=new Node(tag);Matcher m=ATTR.matcher(token.substring(k));
            while(m.find()){String val=m.group(2)!=null?m.group(2):m.group(3)!=null?m.group(3):m.group(4)!=null?m.group(4):"";node.attrs.putIfAbsent(m.group(1).toLowerCase(Locale.ROOT),decode(val));}
            add(stack.peek(),node);
            if(tag.equals("script")||tag.equals("style")){int stop=lower.indexOf("</"+tag,i);i=stop<0?html.length():stop;continue;}
            if(!VOID.contains(tag)&&!token.endsWith("/")){if(stack.size()>256)throw new IllegalArgumentException("Слишком глубокая разметка");stack.push(node);}
        }
        return root;
    }
    private static void add(Node p,Node c){c.parent=p;p.children.add(c);}
    public static String decode(String value) {
        Matcher m=ENTITY.matcher(value);StringBuffer b=new StringBuffer();
        while(m.find()){String key=m.group(1),v=ENTITIES.get(key);if(key.startsWith("#")){try{int cp=key.startsWith("#x")?Integer.parseInt(key.substring(2),16):Integer.parseInt(key.substring(1));v=Character.isValidCodePoint(cp)&&cp!=0?new String(Character.toChars(cp)):"�";}catch(Exception ignored){v="�";}}
            m.appendReplacement(b,Matcher.quoteReplacement(v==null?m.group():v));}
        m.appendTail(b);return b.toString();
    }
    public static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
