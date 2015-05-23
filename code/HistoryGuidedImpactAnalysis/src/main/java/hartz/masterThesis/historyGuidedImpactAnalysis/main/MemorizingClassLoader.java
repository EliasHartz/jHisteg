package hartz.masterThesis.historyGuidedImpactAnalysis.main;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/** Custom classloader of our tool, reports errors and remembers if a class loaded has failed. Comes with up to two
 * separate classpaths. */
public class MemorizingClassLoader extends ClassLoader{
    private final ClassLoader l;
    private final File classpath;
    private final File additionalClasspath;

    private static final HashSet<String> errorsAlreadyReported = new HashSet<>();

    public static MemorizingClassLoader getClassLoader(File classpath) throws IllegalArgumentException{
        return getClassLoader(classpath, null);
    }

    public File getClasspath(){
        return classpath;
    }

    public File getAdditionalClasspath(){
        return additionalClasspath;
    }

    /**
     * Obtains a new loader.
     *
     * @param classpath : primary classpath to load classes from
     * @param additionalClasspath : additional classpath, may be 'null'
     * @return new loader
     * @throws IllegalArgumentException if directory does not exist or
     *         cannot be accessed!
     */
    public static MemorizingClassLoader getClassLoader(File classpath, File additionalClasspath) throws IllegalArgumentException{
        assert(classpath != null);

        MemorizingClassLoader loader = null;
        try{
            Tool.printExtraInfo(" * Loading classes from '"+classpath.getAbsolutePath()+"'");
            if (classpath.isDirectory() && (additionalClasspath == null || additionalClasspath.isDirectory())){
                URL url = classpath.toURI().toURL();
                if (additionalClasspath == null)
                    loader = new MemorizingClassLoader( new URLClassLoader(new URL[]{url} ), classpath, null);
                else loader = new MemorizingClassLoader( new URLClassLoader(new URL[]{url, additionalClasspath.toURI().toURL()}),
                        classpath, additionalClasspath);
            } else throw new IllegalArgumentException("Classpath '"+classpath.getAbsolutePath()+"' is not a directory!");

        }
        catch(Throwable e){
            throw new IllegalArgumentException("Failed to initiate class loader for "+classpath.getAbsolutePath()+"'");
        }
        return loader;
    }

    private MemorizingClassLoader(ClassLoader l, File classpath, File additionalClasspath){
        this.classpath = classpath;
        this.additionalClasspath = additionalClasspath;
        this.l = l;
    }

    /**
     * Loads a class by name, will print any errors.
     *
     * @param fullyQualifiedClassName : class to load
     * @return stream on the class or 'null' on error
     */
    public InputStream loadClassAsStream(String fullyQualifiedClassName){
        LinkedList<String> l = new LinkedList<>();
        InputStream i = loadClassAsStream(fullyQualifiedClassName, l);

        if (!l.isEmpty() && !l.contains(fullyQualifiedClassName)){
            for ( String s : l ) Tool.printError(s);
            errorsAlreadyReported.add(fullyQualifiedClassName);
        }
        return i;
    }

    /**
     * Loads a class by name, will add any errors that occur to the given list.
     *
     * @param fullyQualifiedClassName : class to load
     * @param errors : list into which to append errors, may to 'null' which disables the feature
     * @return stream on the class or 'null' on error
     */
    public InputStream loadClassAsStream(String fullyQualifiedClassName, List<String> errors){
        String s = fullyQualifiedClassName.replace(".", "/")+".class";
        InputStream i = l.getResourceAsStream(s);
        if (i == null){
            if (errors != null){
                Tool.printDebug("Failed to load class '"+fullyQualifiedClassName+ "'");
                errors.add("Failed to load class '"+fullyQualifiedClassName+ "'");
            }
        }
        return i;
    }

    /**
     * Loads a class by name, will print any errors.
     *
     * @param fullyQualifiedClassName : class to load
     * @return class or 'null' on error
     */
    public Class<?> loadClass(String fullyQualifiedClassName){
        LinkedList<String> l = new LinkedList<>();
        Class c = loadClass(fullyQualifiedClassName, l);

        if (!l.isEmpty() && !l.contains(fullyQualifiedClassName)){
            for ( String s : l ) Tool.printError(s);
            errorsAlreadyReported.add(fullyQualifiedClassName);
        }
        return c;
    }


    /**
     * Loads a class by name, will add any errors that occur to the given list.
     *
     * @param fullyQualifiedClassName : class to load
     * @param errors : list into which to append errors, may to 'null' which disables the feature
     * @return class or 'null' on error
     */
    public Class<?> loadClass(String fullyQualifiedClassName, List<String> errors){
        try{
            Class c = l.loadClass(fullyQualifiedClassName);
            Tool.printDebug("loading class '"+fullyQualifiedClassName+"'");
            return c;
        } catch (Exception e) {
            if (errors != null){
                Tool.printDebug(e);
                errors.add("Failed to load class '"+fullyQualifiedClassName+ "'");
            }
            return null;
        }
    }
}
