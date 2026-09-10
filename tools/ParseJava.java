import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Syntax-only validation when android.jar is unavailable. This is not an APK build. */
public class ParseJava {
    public static void main(String[] args)throws Exception{
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        List<File> sources=new ArrayList<>();
        try(var paths=Files.walk(Path.of("app/src/main/java"))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toFile()));}
        try(StandardJavaFileManager files=compiler.getStandardFileManager(diagnostics,null,java.nio.charset.StandardCharsets.UTF_8)){
            JavacTask task=(JavacTask)compiler.getTask(null,files,diagnostics,List.of("-proc:none"),null,files.getJavaFileObjectsFromFiles(sources));
            task.parse();
        }
        for(var d:diagnostics.getDiagnostics())System.err.println(d);
        if(diagnostics.getDiagnostics().stream().anyMatch(d->d.getKind()==Diagnostic.Kind.ERROR))System.exit(1);
        System.out.println("Java syntax parsed: "+sources.size()+" files. Android type checking still requires the SDK.");
    }
}
