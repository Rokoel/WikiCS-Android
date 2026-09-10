import site.wikics.reader.core.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CoreTests {
    static int checks;
    static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);System.out.println("PASS "+name);}
    static String fixture(String name)throws Exception{return Files.readString(Path.of("app/src/main/assets/snapshots/"+name+".html"));}
    public static void main(String[] args)throws Exception{
        Catalog c=new CatalogParser().parse(fixture("home"));
        check(c.programs.size()==9,"All nine program rows parsed");
        check(c.academicYear.equals("2026/27"),"Academic year comes from the page");
        check(c.program("DSBA").name.equals("ПАД"),"ПАД maps to the page's DSBA anchor");
        check(c.program("DSBA").years.equals(Arrays.asList(1,2,3,4)),"Study years come from table headers");
        List<Catalog.Course> dsba=c.filter("DSBA",3,"",false,"");
        System.out.println("DSBA year 3: "+dsba.size()+"; full catalog: "+c.courses.size());
        check(dsba.size()==20,"Exactly 20 DSBA year-three links");
        check(dsba.stream().allMatch(x->x.program.equals("DSBA")&&x.year==3),"No cross-program or cross-year leakage");
        check(c.filter("DSBA",3,"",false,"Calculus").isEmpty(),"Search stays inside the profile");
        Catalog.Course dl=dsba.stream().filter(x->x.title.equals("Deep Learning")).findFirst().orElseThrow();
        check(dl.modules.equals("modules 1-2")&&!dl.unavailable,"Published Deep Learning and module metadata");
        Catalog.Course tsa=dsba.stream().filter(x->x.title.equals("Time Series Analysis")).findFirst().orElseThrow();
        check(tsa.unavailable&&!tsa.url.contains("action=")&&!tsa.url.contains("redlink"),"Unpublished links never open an editor");
        check(c.filter("DSBA",3,"",true,"").stream().noneMatch(x->x.unavailable),"Hide-unpublished filter");
        check(c.filter("DSBA",3,"Specialization Data Science in Finance",false,"").size()==3,"Specialization grouping preserved");
        check(c.filter("RICP",3,"",false,"").isEmpty(),"Empty year stays empty");
        check(c.courses.stream().noneMatch(x->x.title.contains("Kolmogorov")),"Shared electives do not leak into year columns");
        check(c.filter("DSBA",3,"",false,"deep LEARNING").size()==1,"Case-insensitive local search");
        ArticleParser p=new ArticleParser();Article deep=p.parse(fixture("deep-learning"),dl.url);
        check(!deep.missing&&deep.headings.size()==11,"Published article and full table of contents");
        check(deep.body.contains("Hackathon")&&deep.body.contains("<table"),"Grading text and teacher table preserved");
        check(!deep.body.contains("Персональные инструменты")&&!deep.body.contains("mw-editsection"),"Wiki navigation and editing controls removed");
        Article stochastic=p.parse(fixture("stochastic"),WikiUrls.ORIGIN+"/Stochastic_processes_and_applications_DSBA_2026/2027");
        check(!stochastic.missing&&stochastic.body.contains("Sources of Wisdom")&&stochastic.body.contains("0.25"),"Second page and grading formula preserved");
        check(stochastic.body.contains("https://t.me/")&&stochastic.body.contains("table-scroll"),"Resource URLs and horizontal tables retained");
        check(p.parse(fixture("time-series"),tsa.url).missing,"Actual noarticletext snapshot recognized");
        check(WikiUrls.resolve(WikiUrls.HOME,"https://wiki.cs.hse.ru/Deep_Learning_DSBA_2026/2027").equals(dl.url),"Legacy wiki links use the supplied mirror");
        check(WikiUrls.resolve(dl.url,"/index.php?title=C%2B%2B_2026/2027&action=edit&redlink=1").equals(WikiUrls.ORIGIN+"/C%2B%2B_2026/2027"),"Plus signs, slash and redlink URL normalization");
        check(WikiUrls.resolve(dl.url,"javascript:alert(1)").isEmpty(),"JavaScript URLs blocked");
        check(!WikiUrls.trusted("https://wikics.site.attacker.example/test")&&!WikiUrls.trusted("https://wikics.site@attacker.example/test"),"Exact host validation");
        check(!WikiUrls.article("https://wikics.site/images/notes.pdf")&&!WikiUrls.article("https://example.org/x"),"Downloads and external pages use the browser");
        Article dirty=p.parse("<h1 id='firstHeading'>Test</h1><div id='mw-content-text'><p onclick='alert(1)'>Safe text<script>bad()</script><a href='javascript:bad()'>link</a><img src='data:text/html,bad' onerror='bad()'><iframe src='https://evil.test'></iframe><math><mi>x</mi><msup><mi>y</mi><mn>2</mn></msup></math></p></div>",dl.url);
        check(!dirty.body.contains("onclick")&&!dirty.body.contains("<script")&&!dirty.body.contains("javascript:")&&!dirty.body.contains("<iframe")&&!dirty.body.contains("onerror")&&!dirty.body.contains("data:"),"Reader sanitization drops executable content");
        check(dirty.body.contains("<math>")&&dirty.body.contains("<msup>"),"MathML preserved");
        check(Html.decode("C++ &amp; X &#x3b1; &nbsp; &#169;").equals("C++ & X α   ©"),"Entity decoding");
        boolean failed=false;try{new CatalogParser().parse("<html>502 Bad Gateway</html>");}catch(IllegalArgumentException e){failed=true;}check(failed,"Gateway response rejected as a catalog");
        Path cache=Files.createTempDirectory("wikics-test-");WikiRepository repo=new WikiRepository(cache.toFile(),name->Files.newInputStream(Path.of("app/src/main/assets/"+name)));
        check(repo.localCatalog().source.equals("snapshot"),"First launch works from the provided snapshot");
        check(repo.localArticle(dl.url).value.body.contains("Homework"),"Seeded published article available offline");
        check(repo.localArticle(tsa.url).value.missing,"Seeded unavailable page retains its state");
        check(repo.localArticle(WikiUrls.ORIGIN+"/unknown_page")==null,"Uncached page does not invent content");
        Files.createDirectories(Path.of("test-output"));
        Files.writeString(Path.of("test-output/deep-learning-reader.html"),deep.document(false,17));
        Files.writeString(Path.of("test-output/stochastic-reader-dark.html"),stochastic.document(true,17));
        System.out.println("\n"+checks+" checks passed.");
    }
}
