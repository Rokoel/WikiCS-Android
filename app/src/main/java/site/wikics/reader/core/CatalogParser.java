package site.wikics.reader.core;

import java.util.*;
import java.util.regex.*;

/** Reads the courses table: program separator rows span the numbered year columns.
 * The shared electives column is deliberately excluded from every student profile. */
public final class CatalogParser {
    private static final Pattern YEAR=Pattern.compile("^([1-6])\\s*(?:[-–]?[йы])?\\s*курс",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    private static final Pattern MODULES=Pattern.compile("\\s*\\((modules?\\s+[^)]+)\\)\\s*$",Pattern.CASE_INSENSITIVE);
    public Catalog parse(String html){
        Html.Node doc=Html.parse(html);
        Html.Node table=doc.first(n->n.tag.equals("table")&&n.hasClass("courses"));
        if(table==null)throw new IllegalArgumentException("На вики изменилась таблица курсов. Сохранённый каталог остаётся доступен.");
        List<Html.Node> rows=table.all(n->n.tag.equals("tr")&&n.ancestor("table")==table);
        List<Integer> years=new ArrayList<>();List<Integer> yearColumns=new ArrayList<>();
        for(Html.Node row:rows){int column=0;for(Html.Node cell:cells(row)){Matcher m=YEAR.matcher(cell.text());if(m.find()){years.add(Integer.parseInt(m.group(1)));yearColumns.add(column);}column+=span(cell,"colspan");}if(!years.isEmpty())break;}
        if(years.isEmpty())throw new IllegalArgumentException("Не удалось определить курсы обучения");
        List<Catalog.Program> programs=new ArrayList<>();List<Catalog.Course> courses=new ArrayList<>();
        String program="";Set<String> unique=new HashSet<>();
        for(Html.Node row:rows){
            List<Html.Node> cells=cells(row);if(cells.isEmpty())continue;
            Html.Node separator=null;for(Html.Node c:cells)if(span(c,"colspan")==years.size()&&c.all(n->n.tag.equals("a")).isEmpty()&&!c.text().isEmpty())separator=c;
            if(separator!=null){Html.Node marker=separator.first(n->!n.attr("id").isEmpty());program=marker==null?separator.text():marker.attr("id");String p=program;if(programs.stream().noneMatch(v->v.id.equals(p)))programs.add(new Catalog.Program(program,separator.text(),years));continue;}
            if(program.isEmpty()||cells.size()<years.size())continue;
            // Data rows start with the four year cells. The fifth column may span 15 rows.
            for(int col=0;col<years.size();col++){
                Html.Node cell=cells.get(col);if(cell.tag.equals("th")||span(cell,"colspan")!=1)continue;
                String group="Основные курсы";
                for(Html.Node node:cell.all(n->n.tag.equals("b")||n.tag.equals("strong")||n.tag.equals("a"))){
                    if(!node.tag.equals("a")){
                        if(node.ancestor("a")!=null||!node.all(n->n.tag.equals("a")).isEmpty())continue;
                        String label=node.text();
                        if(!label.isEmpty()&&!label.matches("(?i)^\\d+(st|nd|rd|th) year.*"))group=label;
                        continue;
                    }
                    String url=WikiUrls.resolve(WikiUrls.HOME,node.attr("href"));String title=node.text();
                    if(url.isEmpty()||title.isEmpty()||WikiUrls.pageKey(url).equals(WikiUrls.HOME))continue;
                    String identity=program+"/"+years.get(col)+"/"+group+"/"+url;if(!unique.add(identity))continue;
                    Matcher m=MODULES.matcher(title);String modules="";if(m.find()){modules=m.group(1);title=title.substring(0,m.start()).trim();}
                    boolean missing=node.hasClass("new")||node.attr("href").contains("redlink=1");
                    courses.add(new Catalog.Course(title,url,program,years.get(col),group,modules,missing));
                }
            }
        }
        if(programs.isEmpty()||courses.isEmpty())throw new IllegalArgumentException("Каталог пуст: сохранён предыдущий снимок");
        String academic="";for(Html.Node h:doc.all(n->n.tag.equals("h2"))){Matcher m=Pattern.compile("Курсы за (20\\d{2}/(?:20)?\\d{2})").matcher(h.text());if(m.find()){academic=m.group(1);break;}}
        return new Catalog(academic,programs,courses);
    }
    private static List<Html.Node> cells(Html.Node row){List<Html.Node> r=new ArrayList<>();for(Html.Node n:row.children)if(n.tag.equals("td")||n.tag.equals("th"))r.add(n);return r;}
    private static int span(Html.Node cell,String key){try{return Math.max(1,Math.min(100,Integer.parseInt(cell.attr(key))));}catch(Exception e){return 1;}}
}
