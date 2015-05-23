package hartz.masterThesis.historyGuidedImpactAnalysis.core.versions;

import hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution.Executor;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserverNoHook;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.junitAdapter.JUnitAdapter;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.MemorizingClassLoader;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.OutputType;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import org.junit.runner.Result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores the meta-data of version/commit/revision/... in the history of
 * a repository. This does not include the contents of the repository!
 */
public class Version implements Comparable<Version> {

    //version control related
    public final int index;
    public final String identifier;
    public final String author;
    public final String msg;
    public final String date;

    //tool-related paths (ignored for equals-behavior)
    private File pathToMainDir;
    private File pathToActualRepoDir;
    private File pathToNonInstrumentedDir;
    private File humanReadableBytecodeDir;
    private File additionalHumanReadableBytecodeDir;
    private File additionalNonInstrumentedDir;
    private File pathToSources;
    private File pathToCompiledSources;
    private File pathToAdditionalCompiledSources;
    private File buildSpot;
    private String commandToBuild = "";
    private final HashSet<String> ignoreByPrefix;
    private final HashSet<String> ignoreBySuffix;
    private final HashSet<String> excludedMethods;
    private final HashSet<File> excludedDirectories;
    private Version comparingAgainst;

    private String[] runCommands;
    private File directoryWithTests;
    private String[] testsToRun;
    private HashSet<String> excludedTestClasses;
    private String[] executeMainClassnames;
    private String[] executeMainArguments;

    private boolean includeRenamingInSyntaxAnalysis; //need to store that because otherwise syntax results might be corrupted!

    private MemorizingClassLoader classLoader;
    private boolean hasBeenEnriched = false; //will be set to 'true' if the corresponding method is called
    private boolean exportHumanReadableSources;


    /**
     * Creates a new version object. Note that this heralds a version that has not yet
     * been enriched with data entered by the user, e.g. where the source code is.
     * See {@link #enrichWithToolData(java.io.File, String, String, String, String, String, String[], String[], String[], String[], Version, boolean, boolean)}
     *
     * @param index : A unique numerical belonging only to this version. This is used to
     *                define a strict hierarchy and ordering. Can match be set in such a
     *                way as to match some index used the repository but this is not a
     *                requirement or a guarantee.
     * @param identifier : An identifier for this commit, like a hash or revision number.
     *                     Must match the way this commit is identified by the version
     *                     control system, otherwise access or mining will fail.
     * @param author : the version control system's data on the author of this commit
     * @param date : when this commit took place according to the version control system
     * @param msg : the commit's associated message
     */
    public Version(int index, String identifier, String author, String date, String msg) {
        assert(identifier!=null && !identifier.isEmpty());
        assert(author!=null);
        assert(date!=null);
        assert(msg!=null);
        assert(index>=0);

        this.identifier = identifier;
        this.author = author;
        this.msg = msg;
        this.date = date;
        this.index = index;

        ignoreByPrefix = new HashSet<>();
        ignoreBySuffix = new HashSet<>();
        excludedMethods = new HashSet<>();
        excludedDirectories = new HashSet<>();
        exportHumanReadableSources = true;

    }

    /**
     * Imports a version from JSON. Note that this heralds a version
     * that has not yet been enriched with tool data, see
     * {@link #enrichWithToolData(java.io.File, String, String, String, String, String, String[], String[], String[], String[], Version, boolean, boolean)}
     *
     * @param object : JSONObject to import
     */
    public Version(JSONObject object){
        //read from JSON
        this.identifier = object.getString("identifier");
        this.author = object.getString("author");
        this.msg = object.getString("msg");
        this.date = object.getString("date");
        this.index = object.getInt("index");

        //DO NOT LOAD ENRICHED DATA, THE USER WILL INPUT THAT ANEW!!!
        ignoreByPrefix = new HashSet<>();
        ignoreBySuffix = new HashSet<>();
        excludedMethods = new HashSet<>();
        excludedDirectories = new HashSet<>();

        assert(index>=0);
        assert(!identifier.isEmpty());
    }

    /**
     * @return json-representation containing *ONLY* VCS-derived data!
     */
    public JSONObject toJSON(){
        JSONObject o = new JSONObject();
        o.put("identifier", identifier);
        o.put("index", index);
        o.put("author", author);
        o.put("msg", msg);
        o.put("date", date);

        //DO NOT STORE ENRICHED DATA!!!

        //ignore the class loader
        return o;
    }

    /**
     * Exports this Version's data to a version info file in the version's main directory.
     * Will store the data as JSON and includes data entered by the user obtained through
     * {@link #enrichWithToolData(java.io.File, String, String, String, String, String, String[], String[], String[], String[], Version, boolean, boolean)}
     *
     * @throws Exception : in case export fails due to IO problems
     */

    public void persistIntoMainDirectory() throws IOException{
        assert(pathToMainDir.isDirectory() && pathToMainDir.exists());
        File f = new File(pathToMainDir.getAbsolutePath()+"/versionInfo");
        f.createNewFile(); //overwrite what is there
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

        JSONObject o = toJSON();
        //store additional data in the versionInfo file!
        o.put("sourcesIn", pathToSources.getAbsolutePath());
        o.put("compiledSourcesIn", pathToCompiledSources.getAbsolutePath());
        o.put("additionalCompiledSourcesIn", pathToAdditionalCompiledSources.getAbsolutePath());
        //main, actual and non-instrumented are not necessary!

        o.put("commandToBuild", commandToBuild);
        o.put("buildLocation", buildSpot.getAbsolutePath());
        if (comparingAgainst !=null) o.put("comparingAgainstVersion", comparingAgainst.identifier);
        if (!ignoreBySuffix.isEmpty()) o.put("ignoredClassesBySuffix", new JSONArray(ignoreBySuffix));
        if (ignoreByPrefix.isEmpty()) o.put("ignoredClassesByPrefix", new JSONArray(ignoreByPrefix));
        if (excludedMethods.isEmpty()) o.put("ignoredMethodsBySignature", new JSONArray(excludedMethods));
        if (!excludedDirectories.isEmpty()) o.put("ignoredDirectories", new JSONArray(excludedDirectories));

        o.put("renamingOperationsIncluded", includeRenamingInSyntaxAnalysis);
        o.put("exportHumanReadableBytecode", exportHumanReadableSources);


        writer.write(o.toString(Globals.jsonIndentFactor));//store as JSON
        writer.close();
    }

    /**
     * This method allows to check the current configuration against what was
     * persisted on the drive from a previous run.
     *
     * @return 'null' if config matches, the difference otherwise
     */
    public VersionSettingDifference checkAgainstPrevious() throws IOException {
        File versionInfo = new File(pathToMainDir.getAbsolutePath()+"/versionInfo");
        if (!versionInfo.exists()) return VersionSettingDifference.MISSING;
        JSONObject o;
        try {
            o = new JSONObject(FileUtils.readData(versionInfo));
        } catch (IOException e) {
            Tool.printDebug(e);
            return VersionSettingDifference.MISSING;
        }
        assert(o != null);

        /** Files and directories for this version */
        if (Globals.performMachineDependentVersionChecks) {
            /* note that this can be disabled for testing purposes, to avoid binding JUnit tests to the developer's machine */
            String oldSources = o.getString("sourcesIn");
            String oldCompiledSources = o.getString("compiledSourcesIn");
            String oldAdditionalCompiledSources = o.getString("additionalCompiledSourcesIn");
            String oldBuildFile = o.getString("buildLocation");
            if (!pathToSources.getAbsolutePath().equals(oldSources)) return VersionSettingDifference.SOURCE;
            if (!pathToCompiledSources.getAbsolutePath().equals(oldCompiledSources)) return VersionSettingDifference.COMPILED;
            if (!pathToAdditionalCompiledSources.getAbsolutePath().equals(oldAdditionalCompiledSources)) return VersionSettingDifference.ADDITIONAL_COMPILED;
            if (!buildSpot.getAbsolutePath().equals(oldBuildFile)) return VersionSettingDifference.BUILDFILE;
        }

        /** How it was compiled */
        String oldBuildCommand = o.getString("commandToBuild");
        if ( !commandToBuild.equals("") && /* if the current build command is empty, we want to skip compilation and thats okay no matter what came before*/
                !commandToBuild.equals(oldBuildCommand)) return VersionSettingDifference.BUILDCOMMAND;


        /** Exclusion of classes and methods from instrumentation, as well as source code export */
        JSONArray oldSuffixArray = o.optJSONArray("ignoredClassesBySuffix");
        JSONArray oldPrefixArray = o.optJSONArray("ignoredClassesByPrefix");
        JSONArray oldMethodsArray = o.optJSONArray("ignoredMethodsBySignature");
        JSONArray oldDirsArrays = o.optJSONArray("ignoredDirectories");
        boolean oldExportSources = o.optBoolean("exportHumanReadableBytecode", true);

        if ( oldSuffixArray!=null ){
            HashSet<String> oldSuffixSet = new HashSet<>();
            for (int i = 0; i<oldSuffixArray.length(); ++i){
                oldSuffixSet.add(oldSuffixArray.getString(i));
            }
            if (!ignoreBySuffix.equals(oldSuffixSet))
                return VersionSettingDifference.IGNORESUFFIX;
            //else they are equal and everything is okay
        }
        if ( oldPrefixArray!=null ){
            HashSet<String> oldPrefixSet = new HashSet<>();
            for (int i = 0; i<oldPrefixArray.length(); ++i){
                oldPrefixSet.add(oldPrefixArray.getString(i));
            }
            if (!ignoreByPrefix.equals(oldPrefixSet))
                return VersionSettingDifference.IGNOREPREFIX;
            //else they are equal and everything is okay
        }
        if ( oldDirsArrays!=null ){
            HashSet<String> oldDirsSet = new HashSet<>();
            for (int i = 0; i<oldDirsArrays.length(); ++i){
                oldDirsSet.add(oldDirsArrays.getString(i));
            }
            if (!excludedDirectories.equals(oldDirsSet))
                return VersionSettingDifference.EXCLUDEDDIRECTORIES;
            //else they are equal and everything is okay
        }
        if ( oldMethodsArray!=null ){
            HashSet<String> oldMethodsSet = new HashSet<>();
            for (int i = 0; i<oldMethodsArray.length(); ++i){
                oldMethodsSet.add(oldMethodsArray.getString(i));
            }
            if (!excludedMethods.equals(oldMethodsSet))
                return VersionSettingDifference.EXCLUDEDMETHODS;
            //else they are equal and everything is okay
        }
        if (exportHumanReadableSources != oldExportSources)
            return VersionSettingDifference.BYTECODE_EXPORT;


        /** syntax change settings */
        boolean oldRenamingSetting = o.optBoolean("renamingOperationsIncluded", false);
        if (includeRenamingInSyntaxAnalysis != oldRenamingSetting)
            return VersionSettingDifference.RENAMING_SETTINGS;

        /* finally, the version that was used to compute syntax changes! This may trigger an overwrite of the file! */
        String oldCompareAgainst = o.optString("comparingAgainstVersion", null);
        if (oldCompareAgainst == null ){
            /* that means we did not compute syntax changes before */
            if (comparingAgainst != null){
                /* if we want to do that know, we need to update the versionInfo file, otherwise the
                 * version will become corrupt as there might be syntax changes but no version from
                 * which they were derived! */
                persistIntoMainDirectory();
            }
        } else /* okay so we did setup everything to compute syntax changes before*/
            if ( comparingAgainst != null && //we do want to do that know!
                 !comparingAgainst.identifier.equals(oldCompareAgainst)) //but its a different version!
                return VersionSettingDifference.PREDECESSOR;

        return null;
    }

    @Override
    public String toString(){
        return "(["+identifier+"] ["+date+"] ["+author+"] "+msg+")";
    }

    @Override
    public int compareTo(Version o) {
        return index-o.index;
    }

    @Override
    public int hashCode(){
        return identifier.hashCode() + date.hashCode() + msg.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Version){
            return ((Version) o).date.equals(date) &&
                    ((Version) o).identifier.equals(identifier) &&
                    ((Version) o).msg.equals(msg) &&
                    ((Version) o).author.equals(author);
        }
        return false;
    }

    /**
     * Mega-setter to store data entered by the user that is required to actually
     * run our tool.
     *
     * @param pathToMainDir : location of the numbered directory, containing all
     *                        all other directories listed here
     * @param pathToSources : relative path to uncompiled in the version's snapshot
     * @param pathToCompiledSources : relative path to uncompiled in the version's
     *                                snapshot
     * @param additionalCompiledSources : relative path to additional uncompiled in
     *                                    the version's snapshot
     * @param pathToBuildSpot : relative path to where this version can be compiled
     * @param commandToBuild : command that is to be executed to compile this version
     * @param ignoreBySuffix : which classes to exclude from instrumentation by suffix
     * @param ignoreByPrefix : which classes to exclude from instrumentation by prefix
     * @param excludedMethods : method signatures of methods that are to be excluded
     *                          from instrumentation
     * @param excludedDirectories : directories that are to be be excluded from
     *                              instrumentation
     * @param versionToCompareThisOneAgainst : which version the analysis phases should
     *                                         compare this instance to, may be 'null'
     * @param includeRenaming : 'false' if renaming is to be ignored during syntax
     *                           analysis phases
     * @param exportHumanReadableSourcesForLookup : 'true' if all instrumented code
     *                                              is to be persited inside this
     *                                              version's directory
     */

    public void enrichWithToolData(File pathToMainDir, String pathToSources, String pathToCompiledSources, String additionalCompiledSources,
                                   String pathToBuildSpot, String commandToBuild,
                                   String[] ignoreBySuffix, String[] ignoreByPrefix, String[] excludedMethods, String[] excludedDirectories,
                                   Version versionToCompareThisOneAgainst,
                                   boolean includeRenaming, boolean exportHumanReadableSourcesForLookup) throws IOException {
        assert(!hasBeenEnriched);

        /* Note that the version may not have been extracted yet, so no verification (exists() and so on) can be performed! */

        this.pathToMainDir = pathToMainDir.getCanonicalFile();
        this.pathToActualRepoDir = new File(this.pathToMainDir.getAbsolutePath()+"/version");
        this.pathToNonInstrumentedDir = new File(this.pathToMainDir.getAbsolutePath()+"/code_before_instrumentation");
        this.humanReadableBytecodeDir = new File(this.pathToMainDir.getAbsolutePath()+"/code_under_observation");
        this.additionalNonInstrumentedDir = new File(this.pathToMainDir.getAbsolutePath()+"/code_before_instrumentation_additional");
        this.additionalHumanReadableBytecodeDir = new File(this.pathToMainDir.getAbsolutePath()+"/code_under_observation_additional");


        this.pathToSources = pathToSources.isEmpty()?pathToActualRepoDir:
                new File(pathToActualRepoDir.getAbsolutePath()+"/"+pathToSources).getCanonicalFile();
        this.pathToCompiledSources = pathToCompiledSources.isEmpty()?pathToActualRepoDir:
                new File(pathToActualRepoDir.getAbsolutePath()+"/"+pathToCompiledSources).getCanonicalFile();
        this.pathToAdditionalCompiledSources = additionalCompiledSources.isEmpty()?pathToActualRepoDir:
                new File(pathToActualRepoDir.getAbsolutePath()+"/"+additionalCompiledSources).getCanonicalFile();

        this.commandToBuild = commandToBuild;
        this.buildSpot = new File(pathToActualRepoDir.getAbsolutePath()+"/"+pathToBuildSpot).getCanonicalFile();
        this.comparingAgainst = versionToCompareThisOneAgainst;
        this.includeRenamingInSyntaxAnalysis = includeRenaming;
        this.exportHumanReadableSources = exportHumanReadableSourcesForLookup;

        //convert exclusion settings, do another sanitizing run (its user input after all)
        HashSet<String> tmp = new HashSet<>();
        for (String s : ignoreByPrefix){
            if (!s.isEmpty()) tmp.add(s);
        }
        if (!tmp.isEmpty()) this.ignoreByPrefix.addAll(tmp);
        tmp.clear();

        for (String s : ignoreBySuffix){
            if (!s.isEmpty()) tmp.add(s);
        }
        if (!tmp.isEmpty()) this.ignoreBySuffix.addAll(tmp);
        tmp.clear();

        for (String s : excludedMethods){
            if (!s.isEmpty()) tmp.add(s);
        }
        if (!tmp.isEmpty()) this.excludedMethods.addAll(tmp);
        tmp.clear();

        for (String s : excludedDirectories){
            if (!s.isEmpty()) {
                File f = new File(pathToActualRepoDir.getAbsolutePath() + "/" + s).getCanonicalFile();
                this.excludedDirectories.add(f);
            }
        }

        hasBeenEnriched = true; //remember this
    }

    /**
     * Creates a fresh class loader, capable of loading classes from the compiled and additional
     * compiled sources directory of this version (should the later not exist, it is ignored).
     * The class loader can be accessed with {@link #getClassLoader()}.
     *
     * Note: Do not execute this method UNTIL the compiled classes are actually present!
     */
    public void createFreshClassLoader(){
        assert(pathToCompiledSources!=null && pathToCompiledSources.exists());

        File additional = null;
        if (pathToAdditionalCompiledSources != pathToActualRepoDir &&
                !pathToAdditionalCompiledSources.equals(pathToCompiledSources))
            additional = pathToAdditionalCompiledSources;

        this.classLoader = MemorizingClassLoader.getClassLoader(pathToCompiledSources,additional);
    }

    /**
     * Another mega-setter, this one for data on how to derive execution traces.
     * Note that is NOT being persisted or accessible from the outside!
     *
     * @param runCommands : commands to run inside the version's snapshot!
     * @param relativePathToTests : directory containing JUnit tests to run
     * @param testsToRun : which tests to run, see {@link hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configuration}
     * @param testsToSkip  : which tests to skip, see {@link hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configuration}
     * @param executeMainClassnames : which classes to run, see {@link hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configuration}
     * @param executeMainArguments : which arguments to provide to those classes,
     *                               see {@link hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configuration}
     * @throws IOException : in case the test directory does not exist!
     */
    public void storeRunCommands(String[] runCommands, String relativePathToTests, String[] testsToRun, String[] testsToSkip,
                                 String[] executeMainClassnames, String[] executeMainArguments) throws IOException {
        this.runCommands = runCommands;
        this.directoryWithTests = new File(pathToActualRepoDir.getAbsolutePath()+"/"+relativePathToTests).getCanonicalFile();
        this.testsToRun = testsToRun;
        this.excludedTestClasses = new HashSet<>();
        excludedTestClasses.addAll(Arrays.asList(testsToSkip));
        excludedTestClasses.remove(""); //just another sanitizing attempt...
        this.executeMainClassnames = executeMainClassnames;
        this.executeMainArguments = executeMainArguments;
    }

    /**
     * @return 'true' if any means of trace generation are available,
     * see {@link #storeRunCommands(String[], String, String[], String[], String[], String[])}
     */
    public boolean canGenerateTraces(){
        return runCommands.length != 0 || executeMainClassnames.length != 0
                || !directoryWithTests.equals(getCompiledSourcesDir()) || testsToRun.length != 0;
    }

    /**
     * Executes all available run commands inside the version's snapshot,
     * see {@link #storeRunCommands(String[], String, String[], String[], String[], String[])}
     *
     * @return 'true' if any commands were executed
     */
    public boolean executeRunCommands(){
        assert (runCommands != null);
        if ( runCommands.length == 0) return false;

        for (String command : runCommands){
            try{
                Tool.printToolOutput("     - ------------- output by executed command ---------------------", OutputType.OUTPUT);
                Executor.execute(command, getActualExtractedRepo());
                Tool.printToolOutput("     - ------------- end of output by executed command --------------", OutputType.OUTPUT);
            }
            catch (IOException e){ Tool.printError("Execution of command '"+command+"' failed due to '"+e.getMessage()+"'!"); }
        }

        return true;
    }

    /**
     * Executes all available classes with main methods (using the provided arguments),
     * see {@link #storeRunCommands(String[], String, String[], String[], String[], String[])}
     *
     * @return 'true' if any classes were run
     */
    public boolean executeMainMethods() {
        assert (executeMainClassnames != null);
        assert (executeMainArguments != null);

        if ( executeMainClassnames.length == 0) return false;
        for (String fullyQualifiedClassName : executeMainClassnames){
            Class c = classLoader.loadClass(fullyQualifiedClassName); //load the class to make sure it exists and can be loaded!
            if (c == null){
                Tool.printError("Argument '"+fullyQualifiedClassName+"' is not valid classpath for a class with a main-method to run (or compilation failed earlier)! No trace has been generated!");
                continue;
            }
            try{
                String args = " ";
                for (String s : executeMainArguments){
                    args+=s+" ";
                }
                args = args.substring(0,args.length()-1);


                Tool.printToolOutput("     - ------------- output by executed program -----------------------", OutputType.OUTPUT);
                Executor.execute("java -classpath . "+fullyQualifiedClassName+args, getCompiledSourcesDir());
                Tool.printToolOutput("     - ------------- end of output by executed program ----------------", OutputType.OUTPUT);


            }
            catch (IOException e){ Tool.printError("Execution of command ' java "+fullyQualifiedClassName+"' failed due to '"+e.getMessage()+"'!"); }
        }
        return true;
    }

    private File getFileToExportTraceTo(){
        File f;
        File tmpFile = new File (getMainDirectory(), "observedTrace");
        if (tmpFile.exists()){
            int i = 1;
            f = new File(tmpFile.getAbsolutePath()+"_"+i);
            while (f.exists())
                f = new File(tmpFile.getAbsolutePath()+"_"+(++i));
        } else f = tmpFile;

        return f;
    }

    /**
     * Executes all available JUnit tests, see {@link #storeRunCommands(String[], String, String[], String[], String[], String[])}
     *
     * @return 'true' if any tests were executed
     */
    public boolean executeTests(){
        assert (testsToRun != null);
        assert (directoryWithTests != null);
        if (!(testsToRun.length != 0 || !directoryWithTests.equals(getCompiledSourcesDir()))) return false;


        /** If we run JUnit tests from our tool, we must of course NOT wait until the JVM quits for
         *  exporting our traces, as we are running on the JVM as well. Instead, use the special
         *  Coverage Observer designed for this case!    */

        if (testsToRun.length == 0 && !directoryWithTests.equals(getCompiledSourcesDir())) {
            Tool.printExtraInfo("     - searching for JUnit tests at '"+ directoryWithTests.getAbsolutePath()+"'");
            File exportToFile = getFileToExportTraceTo();
            new CoverageObserverNoHook(exportToFile); //use special observer!
            Result result = JUnitAdapter.runJUnitTests(directoryWithTests, this, excludedTestClasses);
            if (result.getRunCount() != 0) CoverageObserverNoHook.getCurrentInstance().export(); //export results!
            Tool.printExtraInfo("     - exporting traces from JUnit tests, this may take a while...");
            System.gc(); //attempt clean-up
            Tool.printExtraInfo("     - JUnit test execution has been completed");
        } else if (testsToRun.length != 0) {
            HashSet<Class> classes = new HashSet<>();
            HashSet<File> directories = new HashSet<>();

            for (String test : testsToRun) {
                if (test.contains("/")) {
                    String relativePath = FileUtils.standardizeRelativePaths(test);
                    File f = new File(getActualExtractedRepo().getAbsolutePath() + "/" + relativePath);

                    /** While I would expect a relative path PER DEFINITION to start in the actual repo, I am supporting
                     *  giving a relative path from the test directory here as well. Convenience feature only!  */
                    if (!f.exists() || !f.isDirectory() || !FileUtils.isSubDirectoryOrFile(directoryWithTests, f))
                        f = new File(directoryWithTests.getAbsolutePath() + "/" + relativePath);

                    if (f.exists() && f.isDirectory() && FileUtils.isSubDirectoryOrFile(directoryWithTests, f)) {
                        directories.add(f);

                    } else {
                        Tool.printError("Argument '" + test + "' for JUnit test to run is not a valid a relative path leading to a directory. No trace has been generated!");
                        continue;
                    }
                } else if (!excludedTestClasses.contains(test)){
                        //attempt to load this class!
                        Class c = classLoader.loadClass(test, null);
                        if (c != null) classes.add(c);
                        else Tool.printError("Argument '" + test +
                                "' for JUnit test to run is not a valid classpath! No trace has been generated!");
                }
            }

            if (!classes.isEmpty() || !directories.isEmpty()){
                File exportToFile = getFileToExportTraceTo();
                new CoverageObserverNoHook(exportToFile); //use special observer!
                int runCount = 0;

                if (!classes.isEmpty()) {
                    Result r = JUnitAdapter.runJUnitTests(classes);
                    runCount+= r.getRunCount();
                }

                if (!directories.isEmpty()) {
                    Result[] results = JUnitAdapter.runJUnitTests(this, directories, excludedTestClasses);
                    for (Result r : results) {
                        runCount+= r.getRunCount();
                    }
                }

                if (runCount != 0){
                    Tool.printExtraInfo("     - exporting traces from JUnit tests, this may take a while...");
                    CoverageObserverNoHook.getCurrentInstance().export(); //export results!
                    System.gc(); //attempt clean-up
                }
            }
            Tool.printExtraInfo("     - JUnit test execution has been completed");
        }
        //else no JUnit tests to run have been defined!

        return true;
    }

    /**
     * Returns true if a given class name (not fully qualified class name, mind that!)
     * matches any of the prefixes or suffixes which are to be ignored.
     *
     * @param className : class name to check
     * @return 'true' if the name is to be ignored, 'false' otherwise
     */
    public boolean isClassToBeIgnored(String className) {
        assert(!className.contains(".")); //we are talking class names, not fully qualified class names here

        if (ignoreBySuffix.contains(className)||ignoreByPrefix.contains(className)) return true;
        for (String prefix : ignoreByPrefix) if (className.startsWith(prefix)) return true;
        for (String suffix : ignoreBySuffix) if (className.endsWith(suffix)) return true;
        return false;
    }

    /**
     * @return 'true' if this version contains the required user-input
     */
    public boolean dataIsComplete() {return hasBeenEnriched;}

    public Set<File> getIgnoredDirectories() {
        return excludedDirectories;
    }

    public HashSet<String> getSignaturesOfIgnoredMethods(){
        return excludedMethods;
    }

    public File getActualExtractedRepo() {
        return pathToActualRepoDir;
    }

    public File getNonInstrumentedDir() {
        return pathToNonInstrumentedDir;
    }

    public File getHumanReadableBytecodeDir() { return humanReadableBytecodeDir;}

    public File getAdditionalHumanReadableBytecodeDir() { return additionalHumanReadableBytecodeDir;}

    public File getSourcesDir() {
        return pathToSources;
    }

    public File getCompiledSourcesDir() {
        return pathToCompiledSources;
    }

    public File getAdditionalCompiledSourcesDir() {
        return pathToAdditionalCompiledSources;
    }

    public File getMainDirectory() {
        return pathToMainDir;
    }

    public File getBuildSpot() {
        return buildSpot;
    }

    public String getCommandToBuild() {
        return commandToBuild;
    }

    public Version getVersionComparingAgainst() {
        return comparingAgainst;
    }

    public File getAdditionalNonInstrumentedDir() { return additionalNonInstrumentedDir; }

    /**
     * Attempts to load a class by name using this versions class loader. Note
     * that {@link #createFreshClassLoader()} must have been called at least once!
     *
     * @param fullyQualifiedClassName : name of class to load
     * @return 'null' or the loaded class if loading was successful
     */
    public Class<?> loadClass(String fullyQualifiedClassName) {
        return classLoader.loadClass(fullyQualifiedClassName);
    }

    /**
     * @return current loader of this version or 'null'
     */
    public MemorizingClassLoader getClassLoader() { return classLoader;}

}
