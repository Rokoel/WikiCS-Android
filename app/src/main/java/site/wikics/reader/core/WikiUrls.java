package site.wikics.reader.core;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class WikiUrls {
    public static final String ORIGIN="https://wikics.site";
    public static final String HOME=ORIGIN+"/Wiki_%D0%A4%D0%9A%D0%9D";
    private WikiUrls() {}
    public static boolean trusted(String url){try{URI u=URI.create(url);return ("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&("wikics.site".equalsIgnoreCase(u.getHost())||"wiki.cs.hse.ru".equalsIgnoreCase(u.getHost()));}catch(Exception e){return false;}}
    public static String resolve(String base,String href) {
        try {
            if(href==null||href.trim().isEmpty()||href.matches("(?s).*[\\x00-\\x1f].*"))return "";
            URI u=URI.create(base).resolve(href.trim().replace(" ","%20"));
            if(!Arrays.asList("https","http","mailto","tel").contains(u.getScheme()==null?"":u.getScheme().toLowerCase(Locale.ROOT)))return "";
            if(u.getRawUserInfo()!=null)return "";
            if(!trusted(u.toString()))return u.toASCIIString();
            String path=u.getRawPath();if(path==null||path.isEmpty())path="/";
            String title=query(u.getRawQuery(),"title");
            if(path.endsWith("index.php")&&!title.isEmpty())path="/"+encodeTitle(title);
            return URI.create(ORIGIN+path+(u.getRawFragment()==null?"":"#"+u.getRawFragment())).toASCIIString();
        }catch(Exception e){return "";}
    }
    public static String pageKey(String url){String u=resolve(HOME,url);int n=u.indexOf('#');return n<0?u:u.substring(0,n);}
    public static String encodeTitle(String title){try{return URLEncoder.encode(title.replace(' ','_'),"UTF-8").replace("%2F","/");}catch(Exception e){throw new IllegalArgumentException(e);}}
    private static String query(String query,String key){if(query==null)return "";for(String s:query.split("&")){String[] p=s.split("=",2);if(p[0].equals(key)&&p.length==2){try{return URLDecoder.decode(p[1],"UTF-8");}catch(Exception e){return "";}}}return "";}
    public static boolean article(String url){
        if(!trusted(url))return false;
        try{String p=URI.create(url).getPath().toLowerCase(Locale.ROOT);return !p.startsWith("/images/")&&!p.matches(".*\\.(pdf|zip|rar|7z|png|jpe?g|svg|gif|webp|mp4|mp3|ipynb|pptx?|docx?|xlsx?|csv|txt)$")&&!p.startsWith("/служебная:")&&!p.startsWith("/special:")&&!p.equals("/api.php");}catch(Exception e){return false;}
    }
}
