package site.wikics.reader.core;

import java.util.*;

public final class ArticleParser {
    private static final Set<String> ALLOWED=new HashSet<>(Arrays.asList(
        "p","div","span","a","h1","h2","h3","h4","h5","h6","b","strong","i","em","u","s","small","sup","sub","br","hr","ul","ol","li","dl","dt","dd","table","thead","tbody","tfoot","tr","th","td","caption","pre","code","blockquote","img","details","summary","math","semantics","mrow","mi","mo","mn","msup","msub","msubsup","mfrac","msqrt","mroot","mtext","mspace","mover","munder","munderover","mtable","mtr","mtd","mfenced"));
    private static final Set<String> DROP=new HashSet<>(Arrays.asList("script","style","iframe","frame","object","embed","form","input","button","textarea","select","link","meta","base","svg","noscript","annotation","annotation-xml"));
    public Article parse(String html,String url){
        Html.Node doc=Html.parse(html);Html.Node root=doc.first(n->n.attr("id").equals("mw-content-text"));
        if(root==null)throw new IllegalArgumentException("Сайт вернул не страницу вики");
        Html.Node title=doc.first(n->n.attr("id").equals("firstHeading"));String name=title==null?"Материалы курса":title.text();
        if(root.first(n->n.hasClass("noarticletext"))!=null)return new Article(name,url,"",true,Collections.emptyList());
        Html.Node content=root.first(n->n.hasClass("mw-parser-output"));if(content==null)content=root;
        List<Article.Heading> headings=new ArrayList<>();int i=0;
        for(Html.Node h:content.all(n->n.tag.matches("h[2-4]")&&!insideToc(n))){
            Html.Node marker=h.first(n->!n.attr("id").isEmpty());String id=marker==null?"reader-section-"+(++i):marker.attr("id");
            if(marker==null)h.attrs.put("id",id);
            headings.add(new Article.Heading(h.text().replace("[править]",""),id));
        }
        StringBuilder body=new StringBuilder();render(content,body,url);
        if(content.text().length()<3)throw new IllegalArgumentException("На странице нет материалов");
        return new Article(name,url,body.toString(),false,headings);
    }
    private static boolean insideToc(Html.Node n){for(Html.Node p=n;p!=null;p=p.parent)if(p.attr("id").equals("toc")||p.hasClass("toc"))return true;return false;}
    private void render(Html.Node n,StringBuilder out,String base){
        if(n.tag.equals("#text")){out.append(Html.escape(n.data));return;}
        if(DROP.contains(n.tag)||n.attr("id").equals("toc")||n.hasClass("toc")||n.hasClass("mw-editsection")||n.hasClass("printfooter")||n.hasClass("catlinks"))return;
        boolean tag=ALLOWED.contains(n.tag);
        if(n.tag.equals("table"))out.append("<div class=\"table-scroll\">");
        if(tag){
            out.append('<').append(n.tag);
            for(String a:Arrays.asList("id","title","alt","lang","dir","colspan","rowspan","scope","start","display")){
                String v=n.attr(a);if(!v.isEmpty())out.append(' ').append(a).append("=\"").append(Html.escape(v)).append('"');
            }
            if(n.hasClass("new"))out.append(" class=\"new\"");
            if(n.tag.equals("a")){
                String href=n.attr("href");String safe=href.startsWith("#")?href:WikiUrls.resolve(base,href);
                if(!safe.isEmpty())out.append(" href=\"").append(Html.escape(safe)).append('"');
            }
            if(n.tag.equals("img")){
                String src=WikiUrls.resolve(base,n.attr("src"));
                if(src.startsWith("https://"))out.append(" src=\"").append(Html.escape(src)).append('"');
                out.append(" loading=\"lazy\"");
            }
            out.append('>');
        }
        for(Html.Node c:n.children)render(c,out,base);
        if(tag&&!Arrays.asList("img","br","hr").contains(n.tag))out.append("</").append(n.tag).append('>');
        if(n.tag.equals("table"))out.append("</div>");
    }
}
