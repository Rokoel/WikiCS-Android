package site.wikics.reader.core;

import java.util.*;

public final class Catalog {
    public final String academicYear;
    public final List<Program> programs;
    public final List<Course> courses;
    public Catalog(String academicYear,List<Program> programs,List<Course> courses){this.academicYear=academicYear;this.programs=Collections.unmodifiableList(programs);this.courses=Collections.unmodifiableList(courses);}
    public static final class Program {
        public final String id,name;
        public final List<Integer> years;
        public Program(String id,String name,List<Integer> years){this.id=id;this.name=name;this.years=Collections.unmodifiableList(new ArrayList<>(years));}
        public String display(){return id.equals("DSBA")?"ПАД · DSBA":name;}
    }
    public static final class Course {
        public final String title,url,program,group,modules;
        public final int year;
        public final boolean unavailable;
        public Course(String title,String url,String program,int year,String group,String modules,boolean unavailable){this.title=title;this.url=url;this.program=program;this.year=year;this.group=group;this.modules=modules;this.unavailable=unavailable;}
    }
    public Program program(String id){for(Program p:programs)if(p.id.equals(id))return p;return null;}
    public List<Course> filter(String program,int year,String group,boolean hideUnavailable,String query){
        List<Course> result=new ArrayList<>();String q=query.trim().toLowerCase(Locale.ROOT);
        for(Course c:courses)if(c.program.equals(program)&&c.year==year&&(!hideUnavailable||!c.unavailable)&&(group.isEmpty()||c.group.equals(group))&&(q.isEmpty()||(c.title+" "+c.group+" "+c.modules).toLowerCase(Locale.ROOT).contains(q)))result.add(c);
        return result;
    }
}
