package site.wikics.reader;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
import site.wikics.reader.core.WikiUrls;

final class Bookmarks {
    static final class Entry {
        final String url,title,program;final int year;
        Entry(String url,String title,String program,int year){this.url=WikiUrls.pageKey(url);this.title=title;this.program=program;this.year=year;}
    }
    final SharedPreferences prefs;
    Bookmarks(SharedPreferences prefs){this.prefs=prefs;}
    List<Entry> all(){List<Entry> result=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("bookmarks","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);result.add(new Entry(o.getString("url"),o.getString("title"),o.getString("program"),o.getInt("year")));}}catch(JSONException ignored){}return result;}
    boolean contains(String url,String program,int year){String key=WikiUrls.pageKey(url);for(Entry e:all())if(e.url.equals(key)&&e.program.equals(program)&&e.year==year)return true;return false;}
    void toggle(Entry entry){List<Entry> list=all();boolean exists=list.removeIf(e->e.url.equals(entry.url)&&e.program.equals(entry.program)&&e.year==entry.year);if(!exists)list.add(0,entry);JSONArray a=new JSONArray();for(Entry e:list){JSONObject o=new JSONObject();try{o.put("url",e.url);o.put("title",e.title);o.put("program",e.program);o.put("year",e.year);a.put(o);}catch(JSONException ignored){}}prefs.edit().putString("bookmarks",a.toString()).apply();}
}
