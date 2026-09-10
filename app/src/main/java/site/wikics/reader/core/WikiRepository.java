package site.wikics.reader.core;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Blocking IO; callers must use a background executor. Cache writes are atomic. */
public final class WikiRepository {
    public interface Assets { InputStream open(String name) throws IOException; }
    public static final class Loaded<T> {
        public final T value;public final String source;public final long timestamp;
        public Loaded(T value,String source,long timestamp){this.value=value;this.source=source;this.timestamp=timestamp;}
    }
    private final File cache;private final Assets assets;
    private static final Map<String,String> SNAPSHOTS=new HashMap<>();
    static {
        SNAPSHOTS.put(WikiUrls.HOME,"home.html");
        SNAPSHOTS.put(WikiUrls.ORIGIN+"/Deep_Learning_DSBA_2026/2027","deep-learning.html");
        SNAPSHOTS.put(WikiUrls.ORIGIN+"/Stochastic_processes_and_applications_DSBA_2026/2027","stochastic.html");
        SNAPSHOTS.put(WikiUrls.ORIGIN+"/Time_Series_Analysis_DSBA_2026/2027","time-series.html");
    }
    public WikiRepository(File cache,Assets assets){this.cache=cache;this.assets=assets;cache.mkdirs();}
    public Loaded<Catalog> localCatalog()throws IOException{
        String html=local(WikiUrls.HOME);if(html==null)throw new IOException("Нет сохранённого каталога");
        try{return new Loaded<>(new CatalogParser().parse(html),source(WikiUrls.HOME),stamp(WikiUrls.HOME));}
        catch(IllegalArgumentException e){return new Loaded<>(new CatalogParser().parse(read(assets.open("snapshots/home.html"))),"snapshot",0);}
    }
    public Loaded<Catalog> refreshCatalog()throws IOException{
        String html=fetch(WikiUrls.HOME);Catalog catalog=new CatalogParser().parse(html);write(WikiUrls.HOME,html);
        return new Loaded<>(catalog,"live",stamp(WikiUrls.HOME));
    }
    public Loaded<Article> localArticle(String url)throws IOException{
        String key=WikiUrls.pageKey(url),html=local(key);if(html==null)return null;
        try{return new Loaded<>(new ArticleParser().parse(html,key),source(key),stamp(key));}catch(IllegalArgumentException e){return null;}
    }
    public Loaded<Article> refreshArticle(String url)throws IOException{
        String key=WikiUrls.pageKey(url),html=fetch(key);Article article=new ArticleParser().parse(html,key);write(key,html);
        return new Loaded<>(article,"live",stamp(key));
    }
    private String source(String key){return file(key).isFile()?"cache":"snapshot";}
    private long stamp(String key){return file(key).lastModified();}
    private String local(String key)throws IOException{File f=file(key);if(f.isFile())return read(new FileInputStream(f));String asset=SNAPSHOTS.get(key);return asset==null?null:read(assets.open("snapshots/"+asset));}
    private File file(String key){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format(Locale.ROOT,"%02x",b));return new File(cache,s+".html");}catch(Exception e){throw new IllegalStateException(e);}}
    private synchronized void write(String key,String html)throws IOException{
        File target=file(key),temp=File.createTempFile("wikics-",".tmp",cache);
        try{Files.write(temp.toPath(),html.getBytes(StandardCharsets.UTF_8));try{Files.move(temp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);}}
        finally{temp.delete();}
    }
    private String fetch(String url)throws IOException{
        if(!WikiUrls.trusted(url)||!url.startsWith("https://"))throw new IOException("Неподдерживаемый адрес вики");
        String current=url;
        for(int redirects=0;redirects<5;redirects++){
            HttpsURLConnection c=(HttpsURLConnection)new URL(current).openConnection();
            javax.net.ssl.HostnameVerifier standard =
                    HttpsURLConnection.getDefaultHostnameVerifier();

            c.setHostnameVerifier((hostname, session) ->
                    standard.verify(hostname, session)
                    || ("wikics.site".equalsIgnoreCase(hostname)
                        && standard.verify("wiki.cs.hse.ru", session)));

            try{
                c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);
                c.setRequestProperty("User-Agent","WikiCS-Android/1.0 (personal course reader)");c.setRequestProperty("Accept","text/html");
                int status=c.getResponseCode();
                if(status>=300&&status<400){String location=c.getHeaderField("Location");if(location==null)throw new IOException("Пустое перенаправление");String next;try{next=URI.create(current).resolve(location.replace(" ","%20")).toASCIIString();}catch(IllegalArgumentException e){throw new IOException("Некорректное перенаправление",e);}if(!WikiUrls.trusted(next))throw new IOException("Неподдерживаемое перенаправление");if(next.startsWith("http://"))next="https://"+next.substring(7);if(next.equals(current))throw new IOException("Не удалось перейти по адресу вики");current=next;continue;}
                if(status==404){InputStream err=c.getErrorStream();String body=err==null?"":read(err);if(body.contains("noarticletext"))return body;return "<h1 id=\"firstHeading\">Страница не опубликована</h1><div id=\"mw-content-text\"><div class=\"noarticletext\"></div></div>";}
                if(status<200||status>=300)throw new IOException("Вики временно недоступна (HTTP "+status+")");
                String type=c.getContentType();if(type!=null&&!type.toLowerCase(Locale.ROOT).contains("html"))throw new IOException("Этот файл нужно открыть в браузере");
                return read(c.getInputStream());
            }finally{c.disconnect();}
        }
        throw new IOException("Слишком много перенаправлений");
    }
    private static String read(InputStream input)throws IOException{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("Загрузка отменена");total+=n;if(total>6_000_000)throw new IOException("Страница превышает 6 МБ");out.write(buffer,0,n);}return out.toString("UTF-8");}}
}
