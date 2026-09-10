package site.wikics.reader.core;

import java.util.*;

public final class Article {
    public final String title,url,body;
    public final boolean missing;
    public final List<Heading> headings;
    public Article(String title,String url,String body,boolean missing,List<Heading> headings){this.title=title;this.url=url;this.body=body;this.missing=missing;this.headings=headings;}
    public static final class Heading {public final String title,id;public Heading(String title,String id){this.title=title;this.id=id;}}
    public String document(boolean dark,int textSize){
        String bg=dark?"#15141b":"#f8f7fc",fg=dark?"#eeeaf6":"#242130",muted=dark?"#b7afc7":"#716b80",accent=dark?"#c1b2ff":"#6655cc",line=dark?"#34303f":"#e5e1ef",card=dark?"#211e2a":"#ffffff";
        String css="*{box-sizing:border-box}html{scroll-behavior:smooth;scroll-padding-top:20px}body{margin:0;background:"+bg+";color:"+fg+";font: "+textSize+"px/1.7 system-ui,-apple-system,sans-serif;overflow-wrap:anywhere}main{max-width:760px;margin:auto;padding:16px 24px 80px}h1,h2,h3,h4{line-height:1.3;letter-spacing:-.025em;font-weight:650}h1{font-size:1.9em;margin:12px 0 26px}h2{font-size:1.35em;margin:40px 0 14px;padding-top:12px;border-top:1px solid "+line+"}h3{font-size:1.1em;margin-top:26px}p{margin:12px 0}a{color:"+accent+";text-underline-offset:3px}img{max-width:100%;height:auto}ul,ol{padding-left:24px}li{margin:8px 0}.table-scroll{overflow-x:auto;margin:20px 0;border:1px solid "+line+";border-radius:14px;background:"+card+"}table{border-collapse:collapse;min-width:100%;font-size:.9em}th,td{padding:12px 14px;border-bottom:1px solid "+line+";text-align:left;vertical-align:top;min-width:110px}th{font-weight:650;color:"+muted+"}pre{overflow-x:auto;padding:18px;background:"+card+";border:1px solid "+line+";border-radius:12px}code{font-size:.9em}blockquote{margin:20px 0;border-left:3px solid "+accent+";padding:4px 18px;color:"+muted+"}details{margin:22px 0;padding:14px;background:"+card+";border-radius:14px}summary{cursor:pointer;color:"+accent+"}.byline,footer{font-size:12px;color:"+muted+";letter-spacing:.02em}footer{margin-top:40px;border-top:1px solid "+line+";padding-top:20px}.new{color:"+muted+";text-decoration-style:dashed}math{overflow-x:auto;max-width:100%}";
        StringBuilder toc=new StringBuilder();if(!headings.isEmpty()){toc.append("<details><summary>На этой странице</summary><ul>");for(Heading h:headings)toc.append("<li><a href=\"#").append(Html.escape(h.id)).append("\">").append(Html.escape(h.title)).append("</a></li>");toc.append("</ul></details>");}
        return "<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; img-src https:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'\"><title>"+Html.escape(title)+"</title><style>"+css+"</style></head><body><main><div class=\"byline\">WIKICS · МАТЕРИАЛЫ КУРСА</div><h1>"+Html.escape(title)+"</h1>"+toc+body+"<footer>Материал: <a href=\""+Html.escape(url)+"\">Wiki — Факультет компьютерных наук</a>. Авторы и лицензия указаны на исходной странице.</footer></main></body></html>";
    }
}
