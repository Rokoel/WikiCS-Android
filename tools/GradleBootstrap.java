import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Source-visible launcher for environments where no Gradle wrapper JAR is bundled.
 * Downloads the official distribution, verifies its published SHA-256, then runs it. */
public class GradleBootstrap {
    static final String VERSION="8.11.1";
    static final String URL="https://services.gradle.org/distributions/gradle-"+VERSION+"-bin.zip";
    public static void main(String[] args)throws Exception{
        if(Runtime.version().feature()<17)throw new IllegalStateException("Use JDK 17 or newer (Android Studio's bundled JDK works).");
        Path home=Path.of(System.getProperty("user.home"),".gradle","wikics-bootstrap");Files.createDirectories(home);
        Path install=home.resolve("gradle-"+VERSION),ready=install.resolve(".verified");
        try(FileChannel channel=FileChannel.open(home.resolve("install.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.lock()){
            if(!Files.isRegularFile(ready)){
                System.out.println("Downloading and verifying official Gradle "+VERSION+"…");
                Path zip=home.resolve("gradle-"+VERSION+".zip");
                String checksum;
                try(InputStream in=download(URL+".sha256")){checksum=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim().split("\\s+")[0];}
                if(!checksum.matches("[0-9a-fA-F]{64}"))throw new IOException("Invalid official checksum response");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                try(InputStream in=new DigestInputStream(download(URL),digest)){Files.copy(in,zip,StandardCopyOption.REPLACE_EXISTING);}
                StringBuilder actual=new StringBuilder();for(byte b:digest.digest())actual.append(String.format("%02x",b));
                if(!actual.toString().equalsIgnoreCase(checksum)){Files.deleteIfExists(zip);throw new IOException("Gradle checksum mismatch; download discarded");}
                try(ZipInputStream in=new ZipInputStream(Files.newInputStream(zip))){ZipEntry entry;while((entry=in.getNextEntry())!=null){Path target=home.resolve(entry.getName()).normalize();if(!target.startsWith(install))throw new IOException("Unexpected distribution archive path");if(entry.isDirectory())Files.createDirectories(target);else{Files.createDirectories(target.getParent());Files.copy(in,target,StandardCopyOption.REPLACE_EXISTING);}}}
                install.resolve("bin/gradle").toFile().setExecutable(true);Files.writeString(ready,checksum);Files.deleteIfExists(zip);
            }
        }
        // Generate the real Gradle wrapper from a tiny separate project. This avoids
        // needing an Android SDK merely to run the initial --version/bootstrap step.
        Path project=Path.of("").toAbsolutePath();
        if(!Files.isRegularFile(project.resolve("gradle/wrapper/gradle-wrapper.jar"))){
            Path staging=Files.createTempDirectory(home,"wrapper-");
            Files.writeString(staging.resolve("settings.gradle"),"rootProject.name = 'wrapper-bootstrap'\n");
            List<String> create=command(install);create.addAll(List.of("--no-daemon","-p",staging.toString(),"wrapper","--gradle-version",VERSION,"--distribution-type","bin"));
            int result=new ProcessBuilder(create).inheritIO().start().waitFor();if(result!=0)System.exit(result);
            for(String file:List.of("gradle/wrapper/gradle-wrapper.jar","gradle/wrapper/gradle-wrapper.properties")){
                Path target=project.resolve(file);Files.createDirectories(target.getParent());Files.copy(staging.resolve(file),target,StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(project.resolve("gradle/wrapper/gradle-wrapper.properties"),"\ndistributionSha256Sum="+Files.readString(ready).trim()+"\n",StandardOpenOption.APPEND);
            try(var paths=Files.walk(staging)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException ignored){}});}
        }
        List<String> command=command(install);
        command.addAll(Arrays.asList(args));
        System.exit(new ProcessBuilder(command).inheritIO().start().waitFor());
    }
    static List<String> command(Path install){boolean windows=System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");List<String> result=new ArrayList<>();if(windows){result.add("cmd");result.add("/c");result.add(install.resolve("bin/gradle.bat").toString());}else result.add(install.resolve("bin/gradle").toString());return result;}
    static InputStream download(String url)throws IOException{URLConnection c=new URL(url).openConnection();c.setConnectTimeout(20000);c.setReadTimeout(30000);return c.getInputStream();}
}
