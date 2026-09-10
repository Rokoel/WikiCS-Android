package site.wikics.reader;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import site.wikics.reader.core.*;

public final class MainActivity extends Activity {
    private final ExecutorService io=Executors.newFixedThreadPool(2);
    private final Handler main=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private Bookmarks bookmarks;
    private WikiRepository repository;
    private Ui u;
    private Catalog catalog;
    private WikiRepository.Loaded<Catalog> catalogState;
    private String program="",query="",group="",catalogError="";
    private int year=1,tab=0,readerGeneration=0,restoredScroll=0;
    private boolean catalogBusy=false;
    private final ArrayList<String> pageUrls=new ArrayList<>(),pageTitles=new ArrayList<>();
    private WikiRepository.Loaded<Article> articleState;
    private String readerError="";
    private LinearLayout root,content,coursesList;
    private TextView readerStatus;
    private WebView web;

    @Override public void onCreate(Bundle saved){
        prefs=getSharedPreferences("wikics",MODE_PRIVATE);
        String theme=prefs.getString("theme","system");
        boolean dark=theme.equals("dark")||(theme.equals("system")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        setTheme(dark?R.style.AppThemeDark:R.style.AppThemeLight);
        super.onCreate(saved);u=new Ui(this,dark);bookmarks=new Bookmarks(prefs);
        repository=new WikiRepository(new File(getFilesDir(),"pages"),name->getAssets().open(name));
        program=prefs.getString("program","");year=prefs.getInt("year",1);
        if(saved!=null){tab=saved.getInt("tab",0);query=saved.getString("query","");group=saved.getString("group","");restoredScroll=saved.getInt("readerScroll",0);ArrayList<String> urls=saved.getStringArrayList("urls"),titles=saved.getStringArrayList("titles");if(urls!=null&&titles!=null&&urls.size()==titles.size()){pageUrls.addAll(urls);pageTitles.addAll(titles);}}
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::goBack);
        renderLoading();
        io.execute(()->{try{WikiRepository.Loaded<Catalog> loaded=repository.localCatalog();onMain(()->{catalogState=loaded;catalog=loaded.value;if(!pageUrls.isEmpty())loadPage(false,false);else render();refreshCatalog(false);});}catch(Exception e){onMain(()->renderFatal(message(e)));}});
    }

    private void onMain(Runnable action){main.post(()->{if(!isFinishing()&&!isDestroyed())action.run();});}
    private String message(Exception e){return e.getMessage()==null?"Не удалось загрузить страницу":e.getMessage();}
    private void shell(boolean withNavigation){
        releaseWebView();
        root=u.column();root.setBackgroundColor(u.bg);root.setFocusableInTouchMode(true);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        FrameLayout frame=new FrameLayout(this);content=u.column();frame.addView(content,new FrameLayout.LayoutParams(-1,-1,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        frame.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int width=Math.min(r-l,u.dp(760));if(content.getLayoutParams().width!=width){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)content.getLayoutParams();p.width=width;content.setLayoutParams(p);}});
        root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        if(withNavigation)root.addView(navigation());setContentView(root);root.requestApplyInsets();root.requestFocus();
    }
    private LinearLayout navigation(){
        LinearLayout bar=u.row();u.padding(bar,16,8,16,8);bar.setBackgroundColor(u.card);
        String[] names={"Курсы","Сохранённое","Настройки"},icons={"book","saved","settings"};
        for(int i=0;i<3;i++){final int target=i;LinearLayout item=u.column();item.setGravity(Gravity.CENTER);u.padding(item,0,8,0,8);if(tab==i)u.surface(item,u.soft,18,false,true);else u.surface(item,Color.TRANSPARENT,18,false,true);
            item.addView(u.icon(icons[i],tab==i?u.accent:u.muted),new LinearLayout.LayoutParams(u.dp(22),u.dp(22)));u.gap(item,5);TextView navLabel=u.text(
                names[i],
                11,
                tab==i ? u.accent : u.muted,
                true
            );
            navLabel.setGravity(Gravity.CENTER);
            navLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            item.addView(
                navLabel,
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            );item.setContentDescription(names[i]);item.setFocusable(true);item.setOnClickListener(v->{hideKeyboard();tab=target;render();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(u.dp(3),0,u.dp(3),0);bar.addView(item,lp);}
        return bar;
    }
    private LinearLayout scrollContent(){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);LinearLayout inner=u.column();u.padding(inner,24,24,24,32);scroll.addView(inner,new ScrollView.LayoutParams(-1,-2));content.addView(scroll,new LinearLayout.LayoutParams(-1,-1));return inner;}
    private void heading(LinearLayout parent,String eyebrow,String title,String subtitle){parent.addView(u.label(eyebrow));u.gap(parent,14);TextView h=u.text(title,34,u.ink,true);h.setLetterSpacing(-.035f);parent.addView(h);if(!subtitle.isEmpty()){u.gap(parent,10);TextView s=u.text(subtitle,15,u.muted,false);s.setLineSpacing(u.dp(3),1);parent.addView(s);}u.gap(parent,26);}
    private void render(){if(catalog==null){renderLoading();return;}if(!pageUrls.isEmpty()){renderReader();return;}if(!prefs.getBoolean("onboarded",false)||program.isEmpty()||catalog.program(program)==null){renderSettings(true);return;}if(tab==1)renderSaved();else if(tab==2)renderSettings(false);else renderHome();}
    private void renderLoading(){shell(false);LinearLayout p=scrollContent();heading(p,"WIKICS / ФКН","Всё для учёбы.","Открываем каталог материалов");ProgressBar progress=new ProgressBar(this);p.addView(progress,new LinearLayout.LayoutParams(u.dp(32),u.dp(32)));}
    private void renderFatal(String error){shell(false);LinearLayout p=scrollContent();heading(p,"WIKICS","Каталог недоступен",error);p.addView(u.button("Попробовать снова",true,()->refreshCatalog(true)));}

    private void renderHome(){
        shell(true);LinearLayout p=scrollContent();
        LinearLayout brand=u.row();TextView label=u.label("WIKICS / ФКН");brand.addView(label,new LinearLayout.LayoutParams(0,-2,1));brand.addView(u.iconButton("refresh","Обновить каталог",()->refreshCatalog(true)),new LinearLayout.LayoutParams(u.dp(48),u.dp(48)));p.addView(brand);
        TextView h=u.text("Моя учёба.",36,u.ink,true);h.setLetterSpacing(-.035f);p.addView(h);u.gap(p,8);p.addView(u.text("Материалы, которые нужны сейчас.",15,u.muted,false));u.gap(p,24);
        LinearLayout profile=u.row();u.padding(profile,18,18,16,18);u.surface(profile,u.soft,22,false,true);LinearLayout profileText=u.column();profileText.addView(u.text(catalog.program(program).display(),20,u.accent,true));u.gap(profileText,6);profileText.addView(u.text(year+" курс  ·  "+catalog.academicYear,13,u.muted,false));profile.addView(profileText,new LinearLayout.LayoutParams(0,-2,1));profile.addView(u.icon("arrow",u.accent),new LinearLayout.LayoutParams(u.dp(22),u.dp(22)));profile.setOnClickListener(v->{tab=2;render();});profile.setContentDescription("Изменить программу и курс");p.addView(profile);u.gap(p,18);
        LinearLayout search=u.row();u.surface(search,u.card,16,true,false);u.padding(search,14,0,10,0);search.addView(u.icon("search",u.muted),new LinearLayout.LayoutParams(u.dp(20),u.dp(20)));
        EditText input=new EditText(this);input.setTextSize(15);input.setTextColor(u.ink);input.setHintTextColor(u.muted);input.setHint("Найти среди моих курсов");input.setSingleLine(true);input.setBackgroundColor(Color.TRANSPARENT);input.setPadding(u.dp(12),u.dp(12),0,u.dp(12));input.setText(query);input.setContentDescription("Поиск по выбранным программе и курсу");input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.addView(input,new LinearLayout.LayoutParams(0,u.dp(52),1));p.addView(search);u.gap(p,14);
        LinkedHashSet<String> groups=new LinkedHashSet<>();for(Catalog.Course c:catalog.filter(program,year,"",false,""))groups.add(c.group);
        if(!groups.contains(group))group="";
        if(groups.size()>1){HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=u.row();addChip(chips,"Все разделы",group.isEmpty(),()->{group="";renderHome();});for(String g:groups)addChip(chips,g,group.equals(g),()->{group=g;renderHome();});hs.addView(chips);p.addView(hs);u.gap(p,16);}
        TextView source=u.text(catalogBusy?"Обновляем каталог…":catalogError.isEmpty()?sourceLabel(catalogState.source,catalogState.timestamp):sourceLabel(catalogState.source,catalogState.timestamp)+" · сайт недоступен",11,u.muted,false);p.addView(source);u.gap(p,22);
        coursesList=u.column();p.addView(coursesList);rebuildCourses();
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){query=s.toString();rebuildCourses();}public void afterTextChanged(Editable e){}});
        input.setOnEditorActionListener((v,id,event)->{hideKeyboard();return true;});
    }
    private void addChip(LinearLayout row,String text,boolean selected,Runnable action){TextView chip=u.text(text,12,selected?u.accent:u.muted,selected);u.padding(chip,14,14,14,14);chip.setMinHeight(u.dp(48));u.surface(chip,selected?u.soft:u.card,14,!selected,true);chip.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMargins(0,0,u.dp(8),0);row.addView(chip,lp);}
    private void rebuildCourses(){
        if(coursesList==null||catalog==null)return;coursesList.removeAllViews();
        List<Catalog.Course> list=catalog.filter(program,year,group,prefs.getBoolean("hideUnavailable",false),query);
        LinearLayout count=u.row();count.addView(u.label("МАТЕРИАЛЫ"),new LinearLayout.LayoutParams(0,-2,1));count.addView(u.text(String.valueOf(list.size()),12,u.muted,false));coursesList.addView(count);u.gap(coursesList,14);
        if(list.isEmpty()){empty(coursesList,"Ничего не найдено",query.isEmpty()?"В этом разделе пока нет курсов. Проверьте фильтры или обновите каталог.":"Попробуйте другое название курса.","search");return;}
        for(Catalog.Course c:list){courseCard(coursesList,c.title,c.modules.isEmpty()?c.group:c.modules,c.unavailable,()->{hideKeyboard();openPage(c.url,c.title,c.unavailable);});}
    }
    private void courseCard(LinearLayout parent,String title,String detail,boolean missing,Runnable open){
        LinearLayout row=u.row();u.padding(row,16,18,14,18);u.surface(row,u.card,20,true,true);
        FrameLayout tile=new FrameLayout(this);u.surface(tile,missing?u.bg:u.soft,13,false,false);FrameLayout.LayoutParams iconLp=new FrameLayout.LayoutParams(u.dp(22),u.dp(22),Gravity.CENTER);tile.addView(u.icon(missing?"clock":"book",missing?u.muted:u.accent),iconLp);LinearLayout.LayoutParams tileLp=new LinearLayout.LayoutParams(u.dp(44),u.dp(44));tileLp.setMargins(0,0,u.dp(14),0);row.addView(tile,tileLp);
        LinearLayout text=u.column();TextView name=u.text(title,16,u.ink,true);name.setLineSpacing(u.dp(3),1);text.addView(name);u.gap(text,7);text.addView(u.text(missing?"Не опубликован"+(detail.isEmpty()?"":" · "+detail):detail,12,u.muted,false));row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.addView(u.icon("arrow",u.muted),new LinearLayout.LayoutParams(u.dp(18),u.dp(18)));
        row.setFocusable(true);row.setOnClickListener(v->open.run());parent.addView(row);u.gap(parent,10);
    }
    private void empty(LinearLayout p,String title,String explanation,String icon){u.gap(p,24);p.addView(u.icon(icon,u.accent),new LinearLayout.LayoutParams(u.dp(38),u.dp(38)));u.gap(p,22);p.addView(u.text(title,23,u.ink,true));u.gap(p,10);TextView body=u.text(explanation,15,u.muted,false);body.setLineSpacing(u.dp(4),1);p.addView(body);u.gap(p,24);}

    private void renderSaved(){shell(true);LinearLayout p=scrollContent();heading(p,"ВАША ПОДБОРКА","Сохранённое.","Страницы для "+catalog.program(program).display()+", "+year+" курса.");int count=0;for(Bookmarks.Entry e:bookmarks.all())if(e.program.equals(program)&&e.year==year){count++;courseCard(p,e.title,"Сохранено для чтения",false,()->openPage(e.url,e.title,false));}if(count==0)empty(p,"Самое нужное — рядом","Нажмите на закладку в открытом материале. Текст страницы останется доступен без интернета.","saved");}

    private void renderSettings(boolean onboarding){
        shell(!onboarding);LinearLayout p=scrollContent();heading(p,onboarding?"ДОБРО ПОЖАЛОВАТЬ В WIKICS":"ПОД ВАШУ УЧЁБУ",onboarding?"Только ваши курсы.":"Настройки.",onboarding?"Выберите программу и курс. Мы соберём материалы в одном месте.":"Программа, чтение и внешний вид.");
        p.addView(u.label("УЧЕБНЫЙ ПРОФИЛЬ"));u.gap(p,12);
        Catalog.Program selected=catalog.program(program);
        setting(p,"Программа",selected==null?"Выбрать программу":selected.display(),()->chooseProgram(onboarding));
        setting(p,"Курс",selected==null?"Сначала выберите программу":year+" курс",()->{if(selected==null){chooseProgram(onboarding);return;}String[] labels=new String[selected.years.size()];for(int i=0;i<labels.length;i++)labels[i]=selected.years.get(i)+" курс";new AlertDialog.Builder(this).setTitle("Курс обучения").setItems(labels,(d,which)->{year=selected.years.get(which);group="";query="";persistProfile();renderSettings(onboarding);}).show();});
        u.gap(p,10);p.addView(u.text("Учебный год: "+catalog.academicYear+". Программы и курсы берутся из каталога вики.",12,u.muted,false));
        if(onboarding){u.gap(p,28);TextView begin=u.button("Открыть мои курсы",true,()->{if(catalog.program(program)==null){chooseProgram(true);return;}persistProfile();prefs.edit().putBoolean("onboarded",true).apply();tab=0;renderHome();});p.addView(begin);u.gap(p,18);p.addView(u.text("Без аккаунта. Настройки сохраняются на устройстве.",12,u.muted,false));return;}
        u.gap(p,30);p.addView(u.label("ЧТЕНИЕ"));u.gap(p,12);
        String theme=prefs.getString("theme","system");String themeLabel=theme.equals("dark")?"Тёмная":theme.equals("light")?"Светлая":"Как в системе";
        setting(p,"Тема",themeLabel,()->new AlertDialog.Builder(this).setTitle("Оформление").setItems(new String[]{"Как в системе","Светлая","Тёмная"},(d,i)->{prefs.edit().putString("theme",new String[]{"system","light","dark"}[i]).apply();recreate();}).show());
        setting(p,"Размер текста",prefs.getInt("textSize",17)+" sp",()->new AlertDialog.Builder(this).setTitle("Размер текста в материалах").setItems(new String[]{"Компактный · 15","Обычный · 17","Крупный · 20"},(d,i)->{prefs.edit().putInt("textSize",new int[]{15,17,20}[i]).apply();renderSettings(false);}).show());
        LinearLayout toggle=u.row();u.padding(toggle,16,16,12,16);u.surface(toggle,u.card,16,true,false);LinearLayout wording=u.column();wording.addView(u.text("Скрывать неопубликованные",14,u.ink,true));u.gap(wording,6);wording.addView(u.text("Только доступные по каталогу",12,u.muted,false));toggle.addView(wording,new LinearLayout.LayoutParams(0,-2,1));Switch sw=new Switch(this);sw.setContentDescription("Скрывать неопубликованные страницы");sw.setChecked(prefs.getBoolean("hideUnavailable",false));sw.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean("hideUnavailable",checked).apply());toggle.addView(sw);p.addView(toggle);
        u.gap(p,30);p.addView(u.label("ИСТОЧНИК"));u.gap(p,12);setting(p,"Wiki ФКН","wikics.site",()->external(WikiUrls.HOME));setting(p,"Каталог",catalogBusy?"Обновляем…":sourceLabel(catalogState.source,catalogState.timestamp),()->refreshCatalog(true));
        u.gap(p,20);p.addView(u.text("WikiCS 1.0 · Независимый клиент\nМатериалы принадлежат авторам вики. Сохранённые тексты доступны офлайн; изображения и внешние файлы могут требовать интернет.",12,u.muted,false));
    }
    private void setting(LinearLayout parent,String title,String value,Runnable action){LinearLayout row=u.row();u.padding(row,16,18,14,18);u.surface(row,u.card,16,true,true);LinearLayout text=u.column();text.addView(u.text(title,14,u.ink,true));u.gap(text,6);text.addView(u.text(value,13,u.muted,false));row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.addView(u.icon("arrow",u.muted),new LinearLayout.LayoutParams(u.dp(19),u.dp(19)));row.setOnClickListener(v->action.run());parent.addView(row);u.gap(parent,9);}
    private void chooseProgram(boolean onboarding){String[] names=new String[catalog.programs.size()];for(int i=0;i<names.length;i++)names[i]=catalog.programs.get(i).display();new AlertDialog.Builder(this).setTitle("Ваша программа").setItems(names,(dialog,which)->{Catalog.Program p=catalog.programs.get(which);program=p.id;if(!p.years.contains(year))year=p.years.get(0);group="";query="";persistProfile();renderSettings(onboarding);}).show();}
    private void persistProfile(){prefs.edit().putString("program",program).putInt("year",year).apply();}

    private String sourceLabel(String source,long when){if(source.equals("snapshot"))return "Снимок · 10.09.2026";String date=new SimpleDateFormat("dd.MM · HH:mm",Locale.getDefault()).format(new Date(when));return(source.equals("live")?"Обновлено · ":"Сохранено · ")+date;}
    private void refreshCatalog(boolean explicit){if(catalogBusy)return;catalogBusy=true;catalogError="";if(catalog!=null&&pageUrls.isEmpty()&&explicit)render();io.execute(()->{try{WikiRepository.Loaded<Catalog> loaded=repository.refreshCatalog();onMain(()->{catalogBusy=false;catalogState=loaded;catalog=loaded.value;if(pageUrls.isEmpty()&&!(getCurrentFocus() instanceof EditText))render();if(explicit)toast("Каталог обновлён");});}catch(Exception e){onMain(()->{catalogBusy=false;catalogError=message(e);if(catalog==null)renderFatal(catalogError);else if(pageUrls.isEmpty()&&!(getCurrentFocus() instanceof EditText))render();if(explicit)toast(catalogError);});}});}

    private String pageUrl(){return pageUrls.get(pageUrls.size()-1);}
    private String pageTitle(){return pageTitles.get(pageTitles.size()-1);}
    private void openPage(String url,String title,boolean knownMissing){
        String safe=WikiUrls.resolve(WikiUrls.HOME,url);if(safe.isEmpty()){toast("Ссылка недоступна");return;}if(!WikiUrls.article(safe)){external(safe);return;}
        pageUrls.add(safe);pageTitles.add(title);articleState=null;readerError="";restoredScroll=0;loadPage(false,knownMissing);
    }
    private void loadPage(boolean forceNetwork,boolean knownMissing){
        if(pageUrls.isEmpty())return;final int generation=++readerGeneration;final String url=pageUrl(),title=pageTitle();readerError="";
        if(web!=null)restoredScroll=web.getScrollY();
        if(knownMissing&&articleState==null)articleState=new WikiRepository.Loaded<>(new Article(title,WikiUrls.pageKey(url),"",true,Collections.emptyList()),catalogState==null?"snapshot":catalogState.source,catalogState==null?0:catalogState.timestamp);
        renderReader();
        io.execute(()->{
            try{
                WikiRepository.Loaded<Article> local=repository.localArticle(url);
                if(local!=null)onMain(()->{if(generation!=readerGeneration)return;articleState=local;renderReader();});
                if(knownMissing&&!forceNetwork)return;
                WikiRepository.Loaded<Article> fresh=repository.refreshArticle(url);
                onMain(()->{if(generation!=readerGeneration)return;if(web!=null)restoredScroll=web.getScrollY();articleState=fresh;readerError="";renderReader();});
            }catch(Exception e){onMain(()->{if(generation!=readerGeneration)return;readerError=message(e);if(articleState==null)renderReader();else if(readerStatus!=null)readerStatus.setText(sourceLabel(articleState.source,articleState.timestamp)+" · не удалось обновить");});}
        });
    }
    @SuppressWarnings("deprecation")
    private void renderReader(){
        if(pageUrls.isEmpty()){render();return;}shell(false);
        LinearLayout toolbar=u.row();u.padding(toolbar,10,4,10,4);toolbar.addView(u.iconButton("back","Назад",this::goBack),new LinearLayout.LayoutParams(u.dp(48),u.dp(48)));
        TextView title=u.text("Материалы",15,u.ink,true);toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        boolean saved=bookmarks.contains(pageUrl(),program,year);
        View save=u.iconButton(saved?"savedFill":"saved",saved?"Удалить закладку":"Сохранить страницу",this::toggleBookmark);save.setEnabled(articleState!=null&&!articleState.value.missing);save.setAlpha(save.isEnabled()?1:.3f);toolbar.addView(save,new LinearLayout.LayoutParams(u.dp(48),u.dp(48)));
        View menu=u.iconButton("more","Действия со страницей",()->{});menu.setOnClickListener(v->readerMenu(v));toolbar.addView(menu,new LinearLayout.LayoutParams(u.dp(48),u.dp(48)));content.addView(toolbar);
        readerStatus=u.text(articleState==null?"Загрузка страницы…":sourceLabel(articleState.source,articleState.timestamp),11,u.muted,false);u.padding(readerStatus,24,2,24,12);content.addView(readerStatus);
        if(articleState==null){LinearLayout p=u.column();u.padding(p,24,30,24,30);content.addView(p,new LinearLayout.LayoutParams(-1,0,1));
            if(readerError.isEmpty()){ProgressBar progress=new ProgressBar(this);p.addView(progress,new LinearLayout.LayoutParams(u.dp(30),u.dp(30)));u.gap(p,24);p.addView(u.text(pageTitle(),24,u.ink,true));u.gap(p,12);p.addView(u.text("Загружаем материалы с вики…",15,u.muted,false));}
            else{empty(p,"Не удалось загрузить",readerError+". Если вы ещё не открывали эту страницу, для первого чтения нужен интернет.","clock");p.addView(u.button("Попробовать снова",true,()->loadPage(true,false)));u.gap(p,12);p.addView(u.button("Открыть в браузере",false,()->external(pageUrl())));}return;}
        Article article=articleState.value;
        if(article.missing){ScrollView scroll=new ScrollView(this);LinearLayout p=u.column();u.padding(p,24,38,24,30);scroll.addView(p);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));empty(p,"Страница ещё\nне опубликована",pageTitle(),"clock");p.addView(u.text("В каталоге есть название курса, но его страница пока пуста. Вы сможете открыть материалы, когда автор их добавит.",16,u.muted,false));u.gap(p,28);p.addView(u.button("Проверить снова",true,()->loadPage(true,false)));u.gap(p,12);p.addView(u.button("Открыть в браузере",false,()->external(pageUrl())));return;}
        web=new WebView(this);web.setBackgroundColor(u.bg);web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(false);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSafeBrowsingEnabled(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);settings.setSupportMultipleWindows(false);settings.setDefaultTextEncodingName("UTF-8");settings.setTextZoom(Math.round(getResources().getConfiguration().fontScale*100));
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setDownloadListener((url,userAgent,disposition,mime,length)->external(url));
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return routeLink(request.getUrl().toString());}
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return routeLink(url);}
            @Override public void onPageFinished(WebView view,String url){if(view!=web)return;if(restoredScroll>0){final int y=restoredScroll;restoredScroll=0;view.postDelayed(()->{if(view==web)view.scrollTo(0,y);},120);}}
        });
        content.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        web.loadDataWithBaseURL(article.url,article.document(u.dark,prefs.getInt("textSize",17)),"text/html","UTF-8",article.url);
        if(!readerError.isEmpty())readerStatus.setText(sourceLabel(articleState.source,articleState.timestamp)+" · не удалось обновить");
    }
    private boolean routeLink(String url){
        String safe=WikiUrls.resolve(pageUrl(),url);if(safe.isEmpty())return true;
        if(safe.contains("#")&&WikiUrls.pageKey(safe).equals(WikiUrls.pageKey(pageUrl())))return false;
        if(WikiUrls.article(safe)){String title;try{title=Uri.decode(Uri.parse(safe).getLastPathSegment()).replace('_',' ');}catch(Exception e){title="Материалы";}openPage(safe,title,false);}else external(safe);return true;
    }
    private void toggleBookmark(){if(articleState==null||articleState.value.missing)return;boolean was=bookmarks.contains(pageUrl(),program,year);bookmarks.toggle(new Bookmarks.Entry(pageUrl(),pageTitle(),program,year));if(web!=null)restoredScroll=web.getScrollY();renderReader();toast(was?"Закладка удалена":"Сохранено. Текст доступен без интернета.");}
    private void readerMenu(View anchor){PopupMenu popup=new PopupMenu(this,anchor);popup.getMenu().add(0,1,0,"Содержание");popup.getMenu().add(0,2,1,"Обновить страницу");popup.getMenu().add(0,3,2,"Открыть в браузере");popup.getMenu().add(0,4,3,"Поделиться ссылкой");popup.setOnMenuItemClickListener(item->{switch(item.getItemId()){case 1:showContents();break;case 2:loadPage(true,false);break;case 3:external(pageUrl());break;case 4:Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,pageTitle()+"\n"+WikiUrls.pageKey(pageUrl()));startActivity(Intent.createChooser(share,"Поделиться"));break;}return true;});popup.show();}
    private void showContents(){if(articleState==null||articleState.value.headings.isEmpty()){toast("У страницы нет разделов");return;}List<Article.Heading> headings=articleState.value.headings;String[] names=new String[headings.size()];for(int i=0;i<names.length;i++)names[i]=headings.get(i).title;new AlertDialog.Builder(this).setTitle("Содержание").setItems(names,(d,i)->{if(web!=null)web.loadUrl(articleState.value.url+"#"+Uri.encode(headings.get(i).id));}).show();}
    private void external(String url){String safe=WikiUrls.resolve(WikiUrls.HOME,url);if(safe.isEmpty()){toast("Неподдерживаемая ссылка");return;}try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(safe)));}catch(ActivityNotFoundException e){toast("Установите приложение для открытия этой ссылки");}}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private void hideKeyboard(){View focused=getCurrentFocus();if(focused!=null){InputMethodManager input=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(input!=null)input.hideSoftInputFromWindow(focused.getWindowToken(),0);focused.clearFocus();}}
    private void goBack(){
        hideKeyboard();if(!pageUrls.isEmpty()){readerGeneration++;pageUrls.remove(pageUrls.size()-1);pageTitles.remove(pageTitles.size()-1);articleState=null;readerError="";restoredScroll=0;if(!pageUrls.isEmpty())loadPage(false,false);else render();}
        else if(tab!=0){tab=0;render();}else finish();
    }
    @Override public void onBackPressed(){goBack();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("tab",tab);state.putString("query",query);state.putString("group",group);state.putStringArrayList("urls",new ArrayList<>(pageUrls));state.putStringArrayList("titles",new ArrayList<>(pageTitles));state.putInt("readerScroll",web==null?restoredScroll:web.getScrollY());}
    private void releaseWebView(){if(web!=null){web.stopLoading();if(web.getParent() instanceof ViewGroup)((ViewGroup)web.getParent()).removeView(web);web.destroy();web=null;}}
    @Override protected void onDestroy(){readerGeneration++;io.shutdownNow();main.removeCallbacksAndMessages(null);releaseWebView();super.onDestroy();}
}
