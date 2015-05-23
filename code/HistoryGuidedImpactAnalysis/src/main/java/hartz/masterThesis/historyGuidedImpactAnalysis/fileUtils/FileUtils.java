package hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.Instrumenter;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserver;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserverNoHook;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Collection of helpful methods regarding files.
 * Who needs apache commons when you have Java 7 and can program the stuff yourself?
 */
public class FileUtils{

    /**
     * Copies a directory or file recursively to another location.
     *
     * @param src : file or directory which is to be copied (= duplicated)
     * @param dest : the target directory
     * @throws IOException : if the arguments are invalid
     */
    public static void copyAllFromAtoB(File src, File dest) throws IOException{
        copyFromAtoB(src, dest, "", "");
    }

    public static void copyWithSuffixFromAtoB(File src, File dest, String suffix) throws IOException {
        copyFromAtoB(src, dest, suffix, "");
    }

    public static void copyAllButSomeDirectoresFromAtoB(File src, File dest, String directoryToIgnore) throws IOException {
        copyFromAtoB(src, dest, "", directoryToIgnore);
    }

    /**
     * Copies a directory or file recursively to another location,
     * potentially excluding files that do not have a certain file ending.
     *
     * @param src : file or directory which is to be copied (= duplicated)
     * @param dest : the target directory
     * @param suffixFilter : if this string is empty, it will be ignored,
     *                       otherwise only files with this ending are copied
     * @param directoryToIgnore : if this string is empty, it will be ignored,
     *                            otherwise all directories with that name
     *                            will be ignored
     * @throws IOException : if the arguments are invalid
     */
    public static void copyFromAtoB(File src, File dest, String suffixFilter, String directoryToIgnore) throws IOException{
        if (src.getCanonicalFile().equals(dest.getCanonicalFile()))
            throw new IOException("Source and Destination are equal!");

        if(src.exists()){
            if (!directoryToIgnore.isEmpty() && src.isDirectory() && src.getName().equals(directoryToIgnore))
                return; //this one is skipped

            if(src.isDirectory()){
                if(!dest.exists())
                    //create destination if required
                    dest.mkdirs();

                for (String fileName : src.list()) {
                    //simply recurse for all files
                    File srcFile = new File(src, fileName);
                    File destFile = new File(dest, fileName);
                    copyFromAtoB(srcFile, destFile, suffixFilter, directoryToIgnore);
                }

            }else{
                if (suffixFilter.isEmpty() || src.getName().toLowerCase().endsWith(suffixFilter)){
                    if (!dest.isDirectory()){

                        InputStream stream = new FileInputStream(src);
                        Files.copy(stream, dest.toPath(), new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                        stream.close();
                        //file successfully copied if no exception was thrown
                    }
                    else throw new IOException("Cannot copy from file "+src.getAbsolutePath()+" to directory "+dest.getAbsolutePath());
                }
            }
        }
        else throw new IOException("Source '"+src.getAbsolutePath()+"' to copy from does not exist!");
    }


    /**
     * Removes a directory and all its sub-directories recursively.
     *
     * @param f : a directory
     * @throws IOException : if the arguments are invalid
     */
    public static void removeDirectory(File f) throws IOException{
        if (f.exists() && f.isDirectory()){
            Tool.printDebug("deleting directory '"+f.getAbsolutePath()+"'");

            Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException{
                    // try to delete the file anyway, even if its attributes
                    // could not be read, since delete-only access is
                    // theoretically possible
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException{
                    if (exc == null){
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                    else{
                        // directory iteration failed; propagate exception
                        throw exc;
                    }
                }
            });
        }
        else throw new IOException("'"+f.getAbsolutePath()+"' is not a valid directory!");
    }

    /**
     * Efficiently reads the data inside a file.
     *
     * @param f : the file to read
     * @return the data as a String
     * @throws java.io.IOException : in case the file is not accessible
     */
    public static String readData(File f) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        char[] buffer = new char[Globals.fileReaderBufferSize];

        StringBuilder builder = new StringBuilder();
        int i = reader.read(buffer);
        while(i>=0){
            if (i<buffer.length)
                builder.append(Arrays.copyOfRange(buffer, 0, i));
            else builder.append(buffer);
            i = reader.read(buffer);
        }
        reader.close();

        String s = builder.toString();
        return s;
    }

    /**
     * Lists the paths of all files and directories in a given directory recursively.
     *
     * @param directory : base to start from
     * @return absolute paths of all contained files
     */

    public static List<String> listAllFilesInDirectoryAsString(File directory) {
        assert(directory.isDirectory());
        List<File> list = listAllInDirectoryHelper(directory, new ArrayList<File>(), new HashSet<File>());
        List<String> result = new ArrayList<String>();
        for (File f : list){
            result.add(f.getAbsolutePath());
        }
        return result;
    }

    /**
     * Lists all files and directories in a given directory recursively.
     *
     * @param directory : base to start from
     * @return files inside given directory
     */

    public static List<File> listAllFilesInDirectoryAsFile(File directory) {
        assert(directory.isDirectory());
        return listAllInDirectoryHelper(directory, new ArrayList<File>(), new HashSet<File>());
    }

    /**
     * Lists all files and directories in a given directory recursively.
     *
     * @param directory : base to start from
     * @param directoriesToIgnore : do not list these directories or files inside them!
     * @return files inside given directory
     */

    public static List<File> listAllFilesInDirectoryAsFile(File directory, Set<File> directoriesToIgnore) {
        assert(directory.isDirectory());
        return listAllInDirectoryHelper(directory, new ArrayList<File>(), directoriesToIgnore);
    }


    private static List<File> listAllInDirectoryHelper(File directory, List<File> result, Set<File> ignore) {
        assert(directory.isDirectory());
	if (!directory.exists())
		return result;

        File[] listOfFiles = directory.listFiles();
        Arrays.sort(listOfFiles); //File is comparable, so this works!

        for (File f : listOfFiles) {
            if (f.isFile()) {
                try{
                    if (!ignore.contains(f.getParentFile().getCanonicalFile()))
                        result.add(f);
                }
                catch (IOException e){
                    Tool.printError("Failed to access '"+f.getAbsolutePath()+"', operation in progress might be incomplete!");
                }

            } else if (f.isDirectory()) {
                try{
                    if (!ignore.contains(f.getCanonicalFile()))
                        listAllInDirectoryHelper(f, result, ignore);
                }
                catch (IOException e){
                    Tool.printError("Failed to access '"+f.getAbsolutePath()+"', operation in progress might be incomplete!");
                }
            }
        }
        return result;
    }

    /**
     * Checks if a given directory or file is a sub-directory of
     * another given directory. This method does NOT check for
     * existence of any of the files!
     *
     * @param supposedParent : the supposed super-directory
     * @param toCheck : the supposed child directory or file
     * @return 'true' if the later is a sub-directory of the former
     */
    public static boolean isSubDirectoryOrFile(File supposedParent, File toCheck){
        if (!supposedParent.isDirectory()) return false;

        try{
            supposedParent = supposedParent.getCanonicalFile();
            toCheck = toCheck.getCanonicalFile();
        }
        catch(Exception e){ Tool.printDebug(e); return false; }

        File parentFile = toCheck;
        while (parentFile != null) {
            if (supposedParent.equals(parentFile)) {
                return true;
            }
            parentFile = parentFile.getParentFile();
        }
        return false;
    }

    public static String standardizeRelativePaths(String path){
        while (path.startsWith("/"))
            path = path.substring(1);

        while (path.endsWith("/"))
            path = path.substring(0,path.length()-1);

        return path;
    }

    public static String convertToRelativePath(String absolutePath, String baseDir) throws IOException {
        if (absolutePath.startsWith(baseDir)){
            File parent = new File(baseDir);
            File child = new File(absolutePath);

            String s = standardizeRelativePaths(parent.getCanonicalPath());
            String s2 = standardizeRelativePaths(child.getCanonicalPath());

            return standardizeRelativePaths(s2.substring(s.length(), s2.length()));
        }
        throw new IOException("Path '"+absolutePath+"' is not sub-directory or file of '"+baseDir+"'");
    }

    public static String convertToRelativePath(File file, File baseDir) throws IOException {
        assert(baseDir.isDirectory());
        return convertToRelativePath(file.getAbsolutePath(), baseDir.getAbsolutePath());
    }


    /**
     * Simply reads a .java-file's package declaration.
     *
     * @param javaSourceFile : a .java-file containing a java-class.
     * @return package-name found inside file
     * @throws IOException : if the '.java'-file cannot be accessed!
     */
    public static String obtainPackageNameFromJava(File javaSourceFile) throws IOException {
        assert(javaSourceFile.isFile());
        assert(javaSourceFile.getName().toLowerCase().endsWith(".java"));

        //read the file
        String data = FileUtils.readData(javaSourceFile);

        //do some String magic to obtain the package name:
        int packageIndex = data.indexOf("package ");
        data = data.substring(packageIndex+"package ".length()); //cut off 'package '
        return data.substring(0,data.indexOf(";")); //extract package name
    }


    /**
     * Given a directory acting as classpath, this method constructs a fully qualified classname
     * for a given file based on the directory structure.
     *
     *
     * @param clazzpath : classpath aka parent-directory of the package structure
     * @param clazz : the .class-file in question
     * @return a fully qualified class name
     */

    public static String obtainFullyQualifiedNameFromDirectoryStructure(File clazzpath, File clazz) throws IOException {
        assert(clazz.isFile());
        assert(clazz.exists());
        assert(clazz.getName().toLowerCase().endsWith(".class"));
        //evil hack! Replace slashes in the relative path with dots and hope it works for all OS variants out there!
        String relativePath = FileUtils.convertToRelativePath(clazz.getAbsolutePath(), clazzpath.getAbsolutePath());
        if (relativePath.toLowerCase().endsWith(".class"))
            relativePath = relativePath.substring(0,relativePath.length()-6);
        if (relativePath.toLowerCase().endsWith(".java"))
            relativePath = relativePath.substring(0,relativePath.length()-5);
        return relativePath.replace("/", ".");
    }

    public static final void zipDirectory(File directory, File zip) throws IOException {
        ZipOutputStream stream = new ZipOutputStream( new FileOutputStream( zip ) );
        zip(directory, directory, stream);
        stream.close();
    }

    private static final void zip(File directory, File base,
                                  ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[Globals.zipReaderBufferSize];
        int read;
        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()){
                zip(files[i], base, zos);
            } else {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getPath().substring(
                        base.getPath().length() + 1));
                zos.putNextEntry(entry);
                while (-1 != (read = in.read(buffer))){
                    zos.write(buffer, 0, read);
                }
                in.close();
            }
        }
    }

    public static final void unzip(File zip, File extractTo) throws IOException {
        ZipFile archive = new ZipFile(zip);
        Enumeration<? extends ZipEntry> e = archive.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            File file = new File(extractTo, entry.getName());
            if (entry.isDirectory() && !file.exists()) {
                file.mkdirs();
            } else {
                if (file.isFile() && file.exists())
                    file.delete(); //overwrite present data
                if (!file.getParentFile().exists())
                    file.getParentFile().mkdirs();//make sure directory structure exists

                //unzip!
                InputStream in = archive.getInputStream(entry);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[8192];
                int read;
                while (-1 != (read = in.read(buffer))) out.write(buffer, 0, read);

                in.close();
                out.close();
            }
        }
        archive.close();
    }

    /**
     * Helper-method to inject parts of this tool's source code into another project, namely
     * the dependencies for coverage observation and exporting!
     *
     * @param compiledSources : classpath folder of the project under test
     * @param outputTargetFile : path to a file where to store output in the future (which may
     *                           not exist at the time of this call)
     * @throws Exception : if anything goes wrong...
     */
    public static void injectCodeIntoProjectUnderTest(File compiledSources, File outputTargetFile) throws IOException{
        Tool.printExtraInfo("     - injecting mechanisms to observe instrumented code");

        try {
          /*################################################################################
           *##### Inject all Coverage Observation functionality into target project!  ######
           *################################################################################*/
            Class[] toInject = new Class[]{
                    //the main coverage observer class, which is instrumented before injecting it!
                    CoverageObserver.class,

                    ObservedMethod.class,
                    SpecialReturnValue.class,
                    NotReturnedYetSingleton.class,
                    ShutdownHook.class,
                    VoidSingleton.class,
                    NullSingleton.class,
                    JSONArray.class,
                    JSONObject.class,
                    JSONNull.class,
                    JSONException.class,
                    JSONString.class,
                    JSONStringer.class,
                    JSONTokener.class,
                    JSONWriter.class,
                    OpcodeTranslator.class,
                    NonBoxed.class,
                    NonBoxedDouble.class,
                    NonBoxedFloat.class,
                    NonBoxedInteger.class,
                    NonBoxedLong.class,
                    NonBoxedBoolean.class,
                    NonBoxedChar.class,
                    NonBoxedShort.class,
                    NonBoxedByte.class,

                    //special instance of the coverage observer that is only called during a tool run
                    CoverageObserverNoHook.class
            };

            //create all the directories necessary!
            File injectedPackageMain = new File(compiledSources + "/" + CoverageObserver.coverageObserverPackageStructure);
            injectedPackageMain.mkdirs();

            for (int dep = 0; dep < toInject.length; ++dep) {
                String name = toInject[dep].getCanonicalName();
                File classToInject = new File(compiledSources + "/" + name.replace(".","/") + ".class");
                assert(!classToInject.exists());

                byte[] codeToInject = (dep == 0) ? Instrumenter.instrumentCoverageObserver(name, outputTargetFile) : Instrumenter.getBytecodeOfClass(name);
                classToInject.createNewFile();

                Tool.printDebug("   -> injecting '"+name+"' into subject version");
                DataOutputStream dout = new DataOutputStream(new FileOutputStream(classToInject));
                dout.write(codeToInject);
                dout.close();
            }

        }
        catch (Exception e){
            Tool.printDebug(e);
            throw new IOException("Failed to inject coverage functionality into version, it seems like '"+compiledSources.getAbsolutePath()+"' was not accessible!");
        }
    }

    /**
     * Removes a previously injected package structure from a compiled sources directory. Note that
     * this assumes that the subject project does not use a package named 'hartz' ;-)
     *
     * @param compiledSources : compiled sources directory into which we injected functionality!
     */
    public static void clearCodeFromProjectUnderTest(File compiledSources) {
        File injectedPackage = new File(compiledSources + "/" + CoverageObserver.coverageObserverPackageStructure);
        try {
            if (injectedPackage.exists())
                while (true) {
                    removeDirectory(injectedPackage);
                    injectedPackage = injectedPackage.getParentFile();
                    if (injectedPackage.getName().equals("hartz")) {
                        removeDirectory(injectedPackage);
                        break; //this was the last package!
                    }
                }
        } catch (IOException e){
            Tool.printError("Failed to clean up previously injected execution observation packages, instrumentation may be corrupted!");
        }
    }
}