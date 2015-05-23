package hartz.masterThesis.historyGuidedImpactAnalysis.core;

import hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution.ExecutionResult;
import hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution.Executor;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.FileIOConfig;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileLevelChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.newClasses.NewClassDetection;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.ChangeDistillerAdapter;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.Trace;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.Instrumenter;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph.CallGraph;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodSource;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.TraceUtil;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.TraceDivergence;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONException;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.OutputType;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.*;
import java.util.*;

/**
 * This class statically implements the core functionality of the tool, that is extraction, compilation,
 * instrumentation, syntax analysis and trace divergence analysis. If you would use this tool for
 * something completely different, it is likely that you would still make use of some of this
 * functionality.
 */
public class Core {

    /**
     * Extracts full snapshots of specific versions from the repository onto the hard-drive.
     * Generates the file and directory structure assumed by other methods in this class!
     *
     * @param config : instance defining from what and where to extract
     * @param versions : versions to extract
     * @return map containing all file-level changes for the given versions
     * @throws IOException : if any of the involved directories cannot be accessed properly
     * @throws IllegalArgumentException : if an previous version is present and incompatible
     */
    public static HashMap<Version, List<FileLevelChange>> extractVersions(FileIOConfig config, Version[] versions)
            throws IOException, IllegalArgumentException {
        assert(config != null);
        assert(versions != null && versions.length >= 1);

        HashMap<Version, List<FileLevelChange>> fileLevelChanges;
        if (config.getRepoDir() == null && config.getRepositoryMiner() == null){
            fileLevelChanges = new HashMap<>();

            //make certain all versions are present!
            for (int i = 0; i<versions.length; ++i){
                Version v = versions[i];
                if (!v.getMainDirectory().exists())
                    throw new IOException("Version '"+v.identifier+"' was not present, do not use '"+Commands.noRepo+"' in this case!");

                File changes = new File(v.getMainDirectory().getAbsoluteFile()+"/codeFileLevelChanges");

                if (changes.exists()) {
                    //read the data from previous run:
                    assert (changes.exists()); //file must exist from previous run
                    String data = FileUtils.readData(changes);
                    JSONArray a = new JSONArray(data);
                    List<FileLevelChange> fileLevelChangesList = new ArrayList<FileLevelChange>(a.length());
                    for (int j = 0; j < a.length(); ++j) {
                        fileLevelChangesList.add(new FileLevelChange(a.getJSONObject(j)));
                    }
                    Tool.printExtraInfo("   - imported file-level changes of '" + v.identifier + "' from present data");
                    fileLevelChanges.put(v,fileLevelChangesList);
                } else throw new IOException("Version '"+v.identifier+"' had no file-level changes data, do not use '"+Commands.noRepo+"' in this case!");
            }

        } else {
            /** default cause, extract from network or local repository */
            assert(config.getRepoDir() != null && config.getRepositoryMiner() != null);

            if (versions.length!=1){
                Tool.print(" * extracting "+versions.length+" versions from the repository");
                Tool.printExtraInfo(" * the following versions will be extracted:");
                for (int i = 0; i<versions.length; ++i){
                    Tool.printExtraInfo("   - "+versions[i]);
                }
            }
            else Tool.print(" * extracting initial version from the repository");


            fileLevelChanges = config.getRepositoryMiner().checkoutCommits(
                    versions, config.isPresentDataToBeOverwritten());
            assert(fileLevelChanges.keySet().size() == versions.length);
        }

        return fileLevelChanges;
    }

    /**
     * Compiles given version if necessary and possible ( = if there is a build command)
     *
     * @param v : Version to compile
     * @param deleteBeforeCompilation : set to 'true' to delete the compiled sources directory
     *                                  before a compilation operation is started
     * @throws IOException
     * @throws IllegalArgumentException
     */

    public static void readyVersion(Version v, boolean deleteBeforeCompilation) throws IOException, IllegalArgumentException {
        assert(v.dataIsComplete());
        verifySources(v);
        verifyCompiledSources(v, deleteBeforeCompilation); //this actually compiles the program if necessary
    }

    /**
     * Instruments a given version, that means injecting functionality related to the class
     * {@link hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserver}.
     * Should not be called on already instrumented bytecode!
     *
     * @param v : version to instrument
     * @param exportSource : set to 'true' to export a human-readable version of the bytecode. This exported
     *                       source may be accessed during later computation phases by using the method
     *                       {@link #lookupSources(hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version, String)}
     *                       It is *recommended* to export sources!
     * @param injectIntoAdditionalFolder : if the given version has an additional classpath, set this to 'true' to inject a
     *                                     Coverage Observation functionality into this classpath as well.
     * @param safeInstrumentation : set to 'true' to use a static max-stack increase instead of
     *                              recomputing this value
     * @return the call graph of the code, including the non-instrumented parts
     * @throws IOException
     */
    public static CallGraph instrumentVersion(Version v, boolean exportSource, boolean injectIntoAdditionalFolder,
                                              boolean safeInstrumentation) throws IOException {
        assert(v.dataIsComplete());
        assert(!v.getNonInstrumentedDir().exists());
        assert(!v.getHumanReadableBytecodeDir().exists());

        //create a copy of the compiled source before instrumenting, so that the user has the original files somewhere! :-)
        String compiledSourcesRelativePath = FileUtils.convertToRelativePath(v.getCompiledSourcesDir(), v.getActualExtractedRepo());
        File nonInstrumented = new File(v.getNonInstrumentedDir().getAbsolutePath()+"/"+compiledSourcesRelativePath);
        nonInstrumented.mkdirs();
        v.getHumanReadableBytecodeDir().mkdirs();
        FileUtils.copyWithSuffixFromAtoB(v.getCompiledSourcesDir(), nonInstrumented, ".class");
        HashMap<String, List<String>> callGraph = new HashMap<>();

        //start the actual instrumentation
        List<File> filesToInstrument = FileUtils.listAllFilesInDirectoryAsFile(v.getCompiledSourcesDir(), v.getIgnoredDirectories());
        Iterator<File> iter = filesToInstrument.iterator();
        while(iter.hasNext()){
            File f = iter.next();
            if (!f.getName().toLowerCase().endsWith(".class") || !FileUtils.isSubDirectoryOrFile(v.getCompiledSourcesDir(), f))
                iter.remove();
        }
        boolean instrumentedAnything = instrument(callGraph, filesToInstrument, v, v.getCompiledSourcesDir(),
                exportSource, v.getHumanReadableBytecodeDir(), safeInstrumentation);

        if (instrumentedAnything){
            //if we instrumented anything, it means we need to provide our coverage observer!
            File exportTracesTo =  new File(v.getMainDirectory()+"/observedTrace");
            FileUtils.injectCodeIntoProjectUnderTest(v.getCompiledSourcesDir(), exportTracesTo);
        } else {
                /* Since we did not instrument anything, we can remove the backup! While this will obviously make the tool
                 * attempt recompilation again in the next step, that is still a lot better than trying to execute this
                 * version. After all, the user is supposed to skip compilation for non-compiling versions by not giving
                 * a command! The other case where this might happen is if the user has ignored EVERYTHING, which is
                 * another completely silly case. When you exclude everything, there is no need to extract this version
                 * in the first place!
                 */
            FileUtils.removeDirectory(v.getNonInstrumentedDir());
            FileUtils.removeDirectory(v.getHumanReadableBytecodeDir());
        }

        //if the source code has an additional classpath, basically do the same actions again but for that classpath!
        if (v.getAdditionalCompiledSourcesDir().exists() && !v.getAdditionalCompiledSourcesDir().equals(v.getCompiledSourcesDir())
                && !v.getAdditionalCompiledSourcesDir().equals(v.getActualExtractedRepo())){

            //make certain that the copies are not shared!
            v.getAdditionalNonInstrumentedDir().mkdirs();
            v.getAdditionalHumanReadableBytecodeDir().mkdirs();
            FileUtils.copyWithSuffixFromAtoB(v.getAdditionalCompiledSourcesDir(), v.getAdditionalNonInstrumentedDir(), ".class");

            //start the actual instrumentation, DO NOT INCLUDE THIS IN THE CALL GRAPH!!!!
            filesToInstrument = FileUtils.listAllFilesInDirectoryAsFile(v.getAdditionalCompiledSourcesDir(), v.getIgnoredDirectories());
            iter = filesToInstrument.iterator();
            while(iter.hasNext()){
                File f = iter.next();
                if (!f.getName().toLowerCase().endsWith(".class") || !FileUtils.isSubDirectoryOrFile(v.getAdditionalCompiledSourcesDir(), f))
                    iter.remove();
            }

            //instrument the additional files as well, remember that we do not store its call graph!
            instrumentedAnything = instrument(callGraph, filesToInstrument, v, v.getAdditionalCompiledSourcesDir(),
                    exportSource, v.getAdditionalHumanReadableBytecodeDir(), safeInstrumentation);
            if (instrumentedAnything){
                if (injectIntoAdditionalFolder) {
                    //most of the time, we do not need to inject this code twice but the option exists...
                    File coverageOutput =  new File(v.getMainDirectory()+"/observedTrace");
                    FileUtils.injectCodeIntoProjectUnderTest(v.getAdditionalNonInstrumentedDir(), coverageOutput);
                }
            } else {
                FileUtils.removeDirectory(v.getAdditionalNonInstrumentedDir());
                FileUtils.removeDirectory(v.getAdditionalHumanReadableBytecodeDir());
            }
        }

        //export call graph!
        CallGraph result = new CallGraph(callGraph);
        File callGraphOutput = new File(v.getMainDirectory(),"callGraph");
        callGraphOutput.createNewFile(); //overwrite without warning!
        BufferedWriter writer = new BufferedWriter(new FileWriter(callGraphOutput));
        writer.write(result.toJSON().toString(Globals.jsonIndentFactor));//store call graph as JSON
        writer.close();


        return result;
    }


    /**
     * Instrument .class-files to output execution traces.
     *
     * @param callGraph: container which will hold call graph information when this method returns,
     *                   use 'null' to skip call graph generation
     * @param filesToInstrument : list of files to instrument (non .class entries are skipped)
     * @param version : version to which these files belong to
     * @param classpathBase : directory containing classes to instrument, explicitly not decided
     *                        through the also provided version
     * @param exportSource : set to 'true' to export a human-readable version of the bytecode
     * @param safeInstrumentation : set to 'true' to use a static max-stack increase instead of
     *                              recomputing this value
     * @return 'true' if anything was instrumented, false otherwise
     * @throws IOException : if any file cannot be accessed
     */
    private static boolean instrument(HashMap<String, List<String>> callGraph, List<File> filesToInstrument,
                                      Version version, File classpathBase, boolean exportSource, File exportTo,
                                      boolean safeInstrumentation) throws IOException {
        boolean instrumentedAnything = false;

        for (File f : filesToInstrument){
            assert(f.getName().toLowerCase().endsWith(".class") );

            String fullyQualifiedClassName = FileUtils.obtainFullyQualifiedNameFromDirectoryStructure(classpathBase, f);

            if (callGraph != null)
                //no matter if the class is ignored or not, we will parse its call graph (but not modify it)
                Instrumenter.updateCallGraph(new FileInputStream(f), fullyQualifiedClassName, callGraph);

            //make sure that this does not fit any suffix or prefix that is to be ignored!
            if (version.isClassToBeIgnored(f.getName().substring(0, f.getName().lastIndexOf(".")))) {
                Tool.printExtraInfo("     - skipping instrumentation of '" + fullyQualifiedClassName +
                        "' since its name matches a pre- or suffix that is to be ignored");
                continue;
            }

            HashMap<String, ArrayList<String>> sourceCodeContainer = exportSource ? new HashMap<String, ArrayList<String>>() : null;
            HashMap<String, HashMap<Integer, Integer> > lineMappingContainer = exportSource ? new HashMap<String, HashMap<Integer, Integer>>() : null;
            HashMap<String, Integer > maxJavaLinesContainer = exportSource ? new HashMap<String, Integer >() : null;

            //instrument this class:
            Tool.printExtraInfo("     - instrumenting class '"+fullyQualifiedClassName+"'");
            byte[] instrumentedCode = exportSource ?
                    Instrumenter.instrumentClass(new FileInputStream(f), fullyQualifiedClassName, version.getSignaturesOfIgnoredMethods(),
                            sourceCodeContainer, lineMappingContainer, maxJavaLinesContainer, version.getClassLoader(), safeInstrumentation) :
                    Instrumenter.instrumentClass(new FileInputStream(f), fullyQualifiedClassName, version.getSignaturesOfIgnoredMethods(),
                            version.getClassLoader(), safeInstrumentation);
            if (instrumentedCode!=null){
                File instrumentedClass = new File(f.toURI());
                instrumentedClass.createNewFile(); //overwrite old file

                //write the code
                DataOutputStream dout = new DataOutputStream(new FileOutputStream(instrumentedClass));
                dout.write(instrumentedCode);
                dout.close();

                //export sources in human-readable form if there are any!
                if (exportSource && !sourceCodeContainer.isEmpty()) {
                    instrumentedAnything = true; //we actually instrumented something!
                    File sourceOutput = new File(exportTo,fullyQualifiedClassName);
                    sourceOutput.createNewFile();

                    JSONArray methodCodeArray = new JSONArray();

                    for (String method : sourceCodeContainer.keySet()){
                        JSONObject methodObject = new JSONObject();
                        ArrayList<String> source = sourceCodeContainer.get(method);
                        methodObject.put("method", method);
                        JSONArray sourceCodeJSON = new JSONArray();

                        //we want it to look nice, that means a good indention :-)
                        int maxNumberOfDigits = Integer.toString(source.size()).length();
                        int bytecodeLineNumber = 0;

                        int maxNumberOfDigitsForJavaLines = -1;
                        if (!lineMappingContainer.get(method).isEmpty())
                            if (maxJavaLinesContainer.containsKey(method))
                                maxNumberOfDigitsForJavaLines = Integer.toString(maxJavaLinesContainer.get(method).intValue()).length();
                            else Tool.printError("Internal error occurred during instrumentation of class '"+fullyQualifiedClassName+
                                    "' when processing Java Line Numbers. Instrumentation was successful but java line numbers may be incorrect!");

                        //export each instruction by adding it to the JSON object
                        for (String inst : source){

                            //produce a nicely readable bytecode line number!
                            String s = Integer.toString(bytecodeLineNumber);
                            int zerosNeeded = maxNumberOfDigits - s.length();
                            for (int i = 0; i < zerosNeeded; ++i)
                                s = "0"+s;

                            //produce a nicely readable java line number if available!
                            HashMap<Integer, Integer> lineMapping = lineMappingContainer.get(method);
                            if (maxNumberOfDigitsForJavaLines >=0 /* = there are java line labels! */ && lineMapping != null && lineMapping.containsKey(bytecodeLineNumber)){
                                String javaLineNumber = Integer.toString(lineMapping.get(bytecodeLineNumber).intValue());
                                int zerosNeeded2 = maxNumberOfDigitsForJavaLines - javaLineNumber.length();
                                for (int j = 0; j < zerosNeeded2; ++j)
                                    javaLineNumber = "0"+javaLineNumber;
                                s += " ("+javaLineNumber+")";
                            }

                            //add the instruction and store it into the array
                            s += ": "+inst;
                            sourceCodeJSON.put(s);
                            ++bytecodeLineNumber;
                        }
                        methodObject.put("source", sourceCodeJSON);
                        methodCodeArray.put(methodObject); //store this method's code!
                    }

                    BufferedWriter writer = new BufferedWriter(new FileWriter(sourceOutput));
                    writer.write(methodCodeArray.toString(Globals.jsonIndentFactor));//store code as JSON
                    writer.close();

                } else {
                        /* Since we have no idea if any method was actually instrumented in this case, we simply need to assume that
                           we actually DID instrument something which is reasonable. In the worst case, we are making a senseless
                           backup here :-) */
                    instrumentedAnything = true;
                }
            } else Tool.printError("Instrumentation failed for class '"+fullyQualifiedClassName+"' (class on disc has not been changed)");
        }
        return instrumentedAnything;
    }

    /**
     * Uses ChangeDistiller to compute syntactic changes between versions. For all versions in the given array,
     * the version's code is compared against the predecessor version (first entry is skipped) and the results are
     * stored inside a syntax change file in the version's directory. If such a file is already present, the data
     * will be imported instead.
     *
     * @param versions : versions to compare
     * @param coverRenaming : set to 'true' to treat detected renaming operations as changes, set to 'false' to ignore
     *                        such changes (which will cause this modification to be absent from the result)
     * @param fileLevelChanges : the result of a previous step, encoding which files were added/modified at all
     * @return a map that contains all syntax changes for all given versions (a version can have 0 syntax changes).
     *         The changes are contained in another map for more efficient access, the used keys are generated by
     *         {@link hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange#getUniqueAccessString()}
     * @throws IOException : if any class cannot be accessed for some reason
     */
    public static TreeMap<Version,HashMap<String, List<SyntaxChange>>> computeSyntaxChanges(
            Version[] versions, boolean coverRenaming, HashMap<Version, List<FileLevelChange>> fileLevelChanges) throws IOException {
        assert(versions.length >= 2);
        TreeMap<Version,HashMap<String, List<SyntaxChange>>> result = new TreeMap<>();

        //go over all versions (always working directly on the version we are iterating over!)
        for (int i = 0; i<versions.length; ++i){
            Version v = versions[i];
            assert(v.dataIsComplete());
            if ((v.getVersionComparingAgainst() == null)) {
                Tool.print("   - version '"+v.identifier+"' is the base to compare against ["+(i+1)+"/"+versions.length+"]");
                result.put(v, new HashMap<String, List<SyntaxChange>>() /* empty set */);
                continue;//this first version is the 'base', we compare against it but do NOT compare it against something, so skip!
            }

            File changes = new File(v.getMainDirectory().getAbsoluteFile()+"/syntacticCodeChanges");

            //import changes if they have already been computed!
            if (changes.exists()){
                //read the data from previous run:
                assert(changes.exists()); //file must exist from previous run
                String data = FileUtils.readData(changes);
                JSONArray a = new JSONArray(data);
                HashMap<String, List<SyntaxChange>> map = new HashMap<>();
                for (int j=0; j<a.length();++j){
                    SyntaxChange importedChange = SyntaxChange.fromJSON(a.getJSONObject(j));
                    if (map.containsKey(importedChange.getUniqueAccessString()))
                        map.get(importedChange.getUniqueAccessString()).add(importedChange);
                    else{
                        List<SyntaxChange> set = new LinkedList<>();
                        set.add(importedChange);
                        map.put(importedChange.getUniqueAccessString(), set);
                    }
                }
                Tool.print("   - imported "+a.length()+" syntactic changes in '" + v.identifier + "' from previous analysis [" + (i + 1) + "/" + versions.length + "]");
                result.put(v, map);
                continue; //go to next version
            }


            List<SyntaxChange> syntaxChanges = new LinkedList<>();
            Tool.print("   - starting syntax change analysis for version '" + v.identifier + "' [" + (i + 1) + "/" + versions.length + "]");

            //cover new classes for this commit
            syntaxChanges.addAll(NewClassDetection.createTargetsForNewClasses(v, fileLevelChanges.get(v), coverRenaming));

            //cover syntax changes
            syntaxChanges.addAll(ChangeDistillerAdapter.computeSyntaxChanges(
               /* (we actually compute for the version under iteration but and use the last one for comparison) */
                            fileLevelChanges.get(v), v.getVersionComparingAgainst(), v, coverRenaming)
            );

            assert(!changes.exists());
            changes.createNewFile();
            JSONArray array = new JSONArray();
            for (SyntaxChange s : syntaxChanges){
                array.put(s.toJSONObject());
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(changes));
            writer.write(array.toString(Globals.jsonIndentFactor));//store as JSON
            writer.close();
            Tool.printExtraInfo("   - exported syntactic changes to file '"+changes.getAbsolutePath()+"'");



            /** Convert the syntax changes to a more efficiently accessible format. While the conversion is
             * expensive, it would be even more expensive to have a slow access to this data later on.*/
            HashMap<String, List<SyntaxChange>> changesAccessibleByClassOrMethodFullyQualifiedName = new HashMap<>();
            for (SyntaxChange s : syntaxChanges){
                if (changesAccessibleByClassOrMethodFullyQualifiedName.containsKey(s.getUniqueAccessString()))
                    changesAccessibleByClassOrMethodFullyQualifiedName.get(s.getUniqueAccessString()).add(s);
                else{
                    List<SyntaxChange> set = new LinkedList<>();
                    set.add(s);
                    changesAccessibleByClassOrMethodFullyQualifiedName.put(s.getUniqueAccessString(), set);
                }
            }
            result.put(v, changesAccessibleByClassOrMethodFullyQualifiedName);
        }

        assert(result.containsKey(versions[0])); //first version MUST have an entry
        assert(result.get(versions[0]).isEmpty()); //but nothing has been computed!
        return result;
    }


    /**
     * Imports all 'observedTrace' files for a version and creates {@link hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.Trace} objects!
     *
     * @param versions : versions to construct traces for
     * @param syntaxChanges : syntax changes for these versions (a method inside a trace
     *                        knows if it was modified in this version)
     * @param coverageBasedComputation : set to 'true' to replace the divergent section
     *                                   metric with a coverage based one
     * @param traceDistanceBasedComputation : set to 'true' to replace the divergent
     *                                        section metric with a purely trace-distance
     *                                        based one. Can be combined with the above
     *                                        which causes both distance and coverage to
     *                                        be computed
     * @return map containing traces for each version (a version might have 0 traces though)
     */
    public static TreeMap<Version, Trace[]> gatherTraces(Version[] versions, TreeMap<Version,HashMap<String, List<SyntaxChange>>> syntaxChanges,
                                                         boolean coverageBasedComputation, boolean traceDistanceBasedComputation){
        TreeMap<Version, Trace[]> result = new TreeMap<>();
        MethodData.setCoverageBasedComparison(coverageBasedComputation);
        MethodData.setTraceDistanceBasedComparison(traceDistanceBasedComputation);

        for (int i = 0; i<versions.length; ++i){
            Version v = versions[i];
            assert(v.dataIsComplete());
            File mainDir = v.getMainDirectory();
            ArrayList<Trace> tmp = new ArrayList<>();

            int numberOfObservedTracesFiles = 0;
            File[] files = mainDir.listFiles();
            Arrays.sort(files); //lexical ordering
            for (File f : files) {
                if (f.isFile() && f.getName().startsWith("observedTrace")){
                    try{
                        assert(f.exists());
                        Tool.printExtraInfo("   - starting import of trace '" + f.getName() + "'");
                        String data = FileUtils.readData(f);
                        JSONObject o = new JSONObject(data);
                        JSONArray coverageData = o.getJSONArray("executionTraces");
                        if (o.has("errors") &&  o.getJSONArray("errors").length() != 0)
                            Tool.printExtraInfo("   - WARNING: Trace File '"+f.getName()+"' of version '"+v.identifier+"' contains errors!");

                        for (int j = 0; j < coverageData.length(); ++j){
                            HashMap<String, List<SyntaxChange>> syntaxChangesForVersion;
                            if (syntaxChanges.containsKey(v)) syntaxChangesForVersion = syntaxChanges.get(v);
                            else syntaxChangesForVersion = new HashMap<>();

                            JSONObject trace = coverageData.getJSONObject(j);
                            Trace t = new Trace(v, trace, syntaxChangesForVersion,
                                    "file"+numberOfObservedTracesFiles+"_trace"+trace.getInt("traceIndex"));

                            tmp.add(t); //since were was no exception thus far, this trace is valid and can be processed further...
                        }
                    }
                    catch (JSONException e){
                        Tool.print("   - import of trace '" + f.getName() + "' for version '"+v.identifier+
                                "' because the contained JSON is malformed: '"+e.getMessage()+"' ["+(i+1)+"/"+versions.length+"]");
                    }
		    catch (Throwable t){
                        Tool.print("   - import of trace '" + f.getName() + "' for version '"+v.identifier+
                                "' failed due to '"+t.getMessage()+"' ["+(i+1)+"/"+versions.length+"]");
                    }

                    ++numberOfObservedTracesFiles;
                }
            }

            if (tmp != null){
                if (tmp.isEmpty()){
                    Tool.print("   - no traces found for version '"+v.identifier+"' ["+(i+1)+"/"+versions.length+"]");
                    result.put(v, new Trace[0]);
                }
                else{
                    Tool.print("   - obtained " + tmp.size()+" trace"+(tmp.size()==1?"":"s")
                            +" for version '"+v.identifier+"'["+(i+1)+"/"+versions.length+"] ");
                    result.put(v, tmp.toArray(new Trace[tmp.size()]));
                }
            } //else import failed!

        }
        return result;
    }

    /**
     * Matches all traces from two versions onto each other, which yields trace divergences.
     *
     * @param tracesInNew : traces in the 'newer' version, the one for which we want to compute targets
     * @param tracesInOld : traces in the version we compare the current one against
     * @param lookForMatchFile : 'true' if a trace matching file is to be imported instead of matching
     *                           traces automatically based on entry point
     * @param v : version we want to compute targets for, is only necessary if we want to use a match
     *            file, aka the parameter above is 'true'.
     * @param filterObjectCharacteristic : 'true' to filter out parameter and return value divergences
     *                                     if their string representation contains the '@' character
     * @return list containing for each newer trace, either a list of divergences (which may be empty) or
     *         'null' if this trace could not be matched to any of the older ones
     */
    public static ArrayList<List<TraceDivergence>> obtainTraceDivergences(Trace[] tracesInNew, Trace[] tracesInOld,
                                                                          boolean lookForMatchFile, Version v, boolean filterObjectCharacteristic) {
        ArrayList<List<TraceDivergence>> differencesByNewerTrace = new ArrayList<>(Math.max(tracesInOld.length, tracesInNew.length));

        /* First, use our MethodCall Matcher to match traces onto another by their entry function. Note that this basically
         * assumes that the starting points for the traces are similar in both versions! */
        String[] entryMethodOld = new String[tracesInOld.length];
        String[] entryMethodNew = new String[tracesInNew.length];
        int counter = 0;
        for (Trace t : tracesInOld) entryMethodOld[counter++] = t.getEntryMethodIdentifier();
        counter = 0;
        for (Trace t : tracesInNew) entryMethodNew[counter++] = t.getEntryMethodIdentifier();


        /** The user can provide a matching for traces manually! */
        int[] traceMatching = null;
        if (lookForMatchFile) {
            try {
                //import matching by user
                File f = new File(v.getMainDirectory(), "traceMatching");
                HashMap<String, String> userDefinedMatching;
                if (f.exists() && f.isFile()) {
                    JSONArray a = new JSONArray(FileUtils.readData(f));
                    userDefinedMatching = new HashMap<>();
                    for (int i = 0; i < a.length(); ++i) {
                        JSONObject o = a.getJSONObject(i);
                        String newer = o.getString("traceInThis");
                        String older = o.getString("matchTo");
                        userDefinedMatching.put(newer, older);
                    }
                } else throw new IOException("");


                //generate matching array for later computation
                traceMatching = new int[tracesInNew.length];
                Arrays.fill(traceMatching, Integer.MIN_VALUE);
                assert(userDefinedMatching != null);
                for (int i = 0; i < tracesInNew.length; ++i) {
                    String traceIdent = tracesInNew[i].getTraceIdentifier();
                    if (userDefinedMatching.containsKey(traceIdent)) {
                        String identOfOther = userDefinedMatching.get(traceIdent);
                        for (int j = 0; j < tracesInOld.length; ++j) {
                            if (tracesInOld[j].getTraceIdentifier().equals(identOfOther)) {
                                traceMatching[i] = j;
                                break;
                            }
                        }
                    }
                    //if not found, value Integer.MIN_VALUE will not be overwritten
                }
            } catch (IOException e) {
                Tool.printError("Failed to find *valid* Trace Matching File for version '" + v.identifier +
                        "', using default matching instead!");
            }
        }

        boolean usedTraceMatchingFile = (traceMatching != null);

        ArrayList<TraceDivergence> nonMatchable = null;
        if (!usedTraceMatchingFile){
            /** Default Matcher, Levenshtein over entry points! */
            nonMatchable = new ArrayList<>(Math.max(entryMethodNew.length, entryMethodOld.length));
            traceMatching = TraceUtil.matchMethodCalls(entryMethodNew, entryMethodOld, null, nonMatchable);
        }


        /** Compute Divergences! */
        int matched = 0;
        for (int indexOfTraceInNew = 0; indexOfTraceInNew < traceMatching.length; ++indexOfTraceInNew) {
            int indexOfTraceInOld = traceMatching[indexOfTraceInNew];
            if (indexOfTraceInOld != Integer.MIN_VALUE) {
                Trace newTrace = tracesInNew[indexOfTraceInNew];
                Trace oldTrace = tracesInOld[indexOfTraceInOld];
                Tool.printExtraInfo("        -> comparing newer " + newTrace.getTraceIdentifier() + " to "
                        + oldTrace.getTraceIdentifier());
                List<TraceDivergence> divergences = oldTrace.compareAgainstNewerTrace(newTrace, filterObjectCharacteristic);
                if (Tool.isExtraInfoEnabled() && divergences.isEmpty())
                    Tool.printExtraInfo("        -> found " + divergences.size() + " divergences between these traces");
                else Tool.printExtraInfo("        -> traces " + divergences.size() + " are identical, no divergences detected");
                differencesByNewerTrace.add(divergences);
                matched++; //remember this for output
            } else {
                /** Could not be matched! This happens. Rather than comparing incomparable data, we decide to report
                 *   these further down below, it makes no sense anyway... */
                differencesByNewerTrace.add(null /* to differentiate from 'did not find anything */);
            }
        }

        if (matched != 0) {
            Tool.print("     ~> matched and compared " + matched + " traces for this version"+
                    (usedTraceMatchingFile?" (trace matching file was used)":""));
            if (nonMatchable!= null && !nonMatchable.isEmpty())
                Tool.print("     ~> " + nonMatchable.size() / 2 +/* because we have two for every difference!*/
                        " traces could not be matched (generate traces that share entry points for best results).");
        } else
            Tool.print("     ~> could not match *ANY* traces for this version! Generate traces that start both in this version and in the one you compare against to obtain results!");

        return differencesByNewerTrace;
    }




    private static final HashMap<Version, HashMap<String, MethodSource>> sourceLookupCache = new HashMap<>();
    /**
     * Returns the sources of a given class if that source is available for the given version. Does cache,
     * so repeated requests are efficient and not limited by FileIO.
     *
     * @param version : version to lookup sources for
     * @param fullyQualifiedMethodName : class to obtain sources of
     * @return 'null' or the requested sources
     */
    public static MethodSource lookupSources(Version version, String fullyQualifiedMethodName) {
        HashMap<String, MethodSource> source = sourceLookupCache.get(version);
        String className = fullyQualifiedMethodName.substring(0,fullyQualifiedMethodName.lastIndexOf("."));

        if (source == null){
            HashMap<String, MethodSource> newCache = new HashMap<>();
            sourceLookupCache.put(version, newCache);
            source = newCache;
        }


        if (source.containsKey(fullyQualifiedMethodName))
            return source.get(fullyQualifiedMethodName);
        else {
            //load all methods of the class while we are at it!
            File sourceLookup = version.getHumanReadableBytecodeDir();
            while (sourceLookup != null && sourceLookup.exists()) {
                File f = new File(sourceLookup, className);
                if (f.exists()) {
                    try {
                        String data = FileUtils.readData(f);
                        JSONArray a = new JSONArray(data);
                        for (int j = 0; j < a.length(); ++j) {
                            MethodSource methodSource = new MethodSource(a.getJSONObject(j));
                            source.put(methodSource.identifier, methodSource);
                        }

                        //now, the method should be present
                        assert(source.containsKey(fullyQualifiedMethodName));
                        return source.get(fullyQualifiedMethodName);
                    } catch (IOException e) {
                        Tool.printError("Human-readable source code file '" + f.getAbsolutePath() + "' could not be accessed!");
                        Tool.printDebug(e);
                        return null;
                    }
                } else //okay, attempt to find it in the additional exported directory
                    sourceLookup = version.getAdditionalHumanReadableBytecodeDir();
            }
            Tool.printError("Human-readable source code file for '" + fullyQualifiedMethodName +
                    "' does not exist despite the class being instrumented! Skipping source code lookup (ignore this error if '"+Commands.doNotExportSource+"' was used)");
            return null;
        }
    }




    /**
     * For a output of the of trace divergence analysis, this method constructs a map to make
     * all divergences inside the same method accessible by the that method's identifier!
     * *NOTE* that the mapping is based on the method's identifier only, that means if your
     * divergences come from different traces, they will be mashed together nonetheless!
     * Does not modify anything, simply returns given data in another structure.
     *
     * @param traceDivergences : all trace divergences for a version (includes unmatchable entries)
     * @return a Map associating method identifiers with divergences
     */
    public static HashMap<String, List<TraceDivergence>> restructureTraceDivergences(TraceDivergence[][] traceDivergences) {
        HashMap<String, List<TraceDivergence>> divergencesByMethod = new HashMap<>();

        for (TraceDivergence[] divergencesForTrace : traceDivergences) {
            if (divergencesForTrace == null) continue; // = trace could not be matched!

            for (TraceDivergence d : divergencesForTrace) {
                String methodIdentifier = d.getMethodData().getIdentifier();
                if (divergencesByMethod.containsKey(methodIdentifier))
                    divergencesByMethod.get(methodIdentifier).add(d);
                else {
                    LinkedList<TraceDivergence> l = new LinkedList<>();
                    l.add(d);
                    divergencesByMethod.put(methodIdentifier, l);
                }
            }
        }
        return divergencesByMethod;
    }

    /**
     * For a list of trace divergences, this method constructs a map to make all divergences
     * inside the same method accessible by the that method's identifier! *NOTE* that the
     * mapping is based on the method's identifier only, that means if your divergences
     * come from different traces, they will be mashed together nonetheless!
     * Does not modify anything, simply returns given data in another structure.
     *
     * @param traceDivergences : list of trace divergences
     * @return a Map associating method identifiers with divergences
     */
    public static HashMap<String, List<TraceDivergence>> restructureTraceDivergences(List<TraceDivergence> traceDivergences) {
        HashMap<String, List<TraceDivergence>> divergencesByMethod = new HashMap<>();

        for (TraceDivergence d : traceDivergences) {
            String methodIdentifier = d.getMethodData().getIdentifier();
            if (divergencesByMethod.containsKey(methodIdentifier))
                divergencesByMethod.get(methodIdentifier).add(d);
            else {
                LinkedList<TraceDivergence> l = new LinkedList<>();
                l.add(d);
                divergencesByMethod.put(methodIdentifier, l);
            }
        }
        return divergencesByMethod;
    }



    /** Only used for scaffolding testing and debugging purposes, DO NOT CALL THIS METHOD UNLESS YOU ARE THE
     *  AUTHOR OF THIS TOOL AND KNOW EXACTLY WHAT YOU ARE DOING! User, you have been warned! Take care. */
    public static void DEBUG_ONLY_injectSourceCode(Version v, String methodIdentifier, MethodSource source){
        if (sourceLookupCache.containsKey(v)){
            sourceLookupCache.get(v).put(methodIdentifier,source);
        }else {
            HashMap<String, MethodSource> h = new HashMap<>();
            h.put(methodIdentifier, source);
            sourceLookupCache.put(v,h);
        }
    }

    private static void verifySources(Version v) throws IllegalArgumentException{
        File sources = v.getSourcesDir();
        if (sources.exists() && sources.isDirectory())
            Tool.printExtraInfo("   - assuming location of sources is '"+sources.getAbsolutePath()+"'");
        else{
            String s = "Given relative path to sources for version '"+v.identifier+"' does not exist with respect to directory!";
            Tool.printError(s);
            throw new IllegalArgumentException(s);
        }
    }


    private static File verifyCompiledSources(Version v, boolean deleteBeforeCompilation) throws IllegalArgumentException, IOException {
        File binFolder = v.getCompiledSourcesDir();
        Tool.printExtraInfo("   - assuming base classpath for compiled sources is '" + binFolder.getAbsolutePath() + "'");

        //first, ATTEMPT to build the sources (will return if this is impossible or not necessary)
        buildSources(v, deleteBeforeCompilation);

        //then, get the compiled sources :
        if (binFolder.exists()){
            if (binFolder.isDirectory()){
                return binFolder;
            }
            else{
                String s = "Path to compiled sources is invalid, '"+binFolder.getAbsolutePath()+"' is not a directory (or compilation failed)!";
                Tool.printError(s);
                throw new IllegalArgumentException(s);
            }
        }
        else{
            String s = "Path to compiled sources is invalid, '"+binFolder.getAbsolutePath()+"' does not exist or cannot be accessed (or compilation failed)!";
            Tool.printError(s);
            throw new IllegalArgumentException(s);
        }
    }

    private static boolean gaveHintAlready = false;

    private static void buildSources(Version v, boolean deleteBeforeCompilation) throws IllegalArgumentException, IOException {
        File buildFileOrDir = v.getBuildSpot();
        String buildTargetString = v.getCommandToBuild();
        boolean instrumentedCodeEarlier = v.getNonInstrumentedDir().exists();
        assert(v.getHumanReadableBytecodeDir().exists() == instrumentedCodeEarlier);

        if ((buildTargetString == null || buildTargetString.isEmpty())){
            //user does not want to compile the project (or has no idea what he or she is doing)!

            if(!instrumentedCodeEarlier) {
                /** User-friendly assistance feature:
                 *  This loop basically alerts the user to the fact that you might want to use ANT or Maven.
                 *  It serves as a helpful reminder, nothing more. User experience for the win! */

                if (buildFileOrDir.exists() && buildFileOrDir.isDirectory() /* which likely means the default "." was used */) {
                    boolean warnAboutBuildFile = false;
                    for (File f : buildFileOrDir.listFiles()) {
                        String xml = f.getName().toLowerCase();
                        if (xml.endsWith("build.xml") || xml.endsWith("pom.xml")) {
                            warnAboutBuildFile = true;
                            break;
                        }
                    }
                    if (warnAboutBuildFile)
                        Tool.print("   - WARNING: Found an .xml-file in '" + buildFileOrDir.getAbsolutePath() + "' that is likely a Maven or ANT file but '" +
                                Commands.buildCommand + "' has not been used! Nothing was compiled, directory content is assumed to contain correct version of compiled sources!");
                }
            }
            return; //no build command -> no sources are build! (User still needs to provide compiled sources)
        }

        //we HAVE a command, user wants to use ANT or Maven!
        if (buildFileOrDir.exists()){
            if (instrumentedCodeEarlier){
                if (deleteBeforeCompilation /* is also set active in overwrite-mode */){
                    Tool.printExtraInfo("   - overwriting previously instrumented code upon compilation, re-instrumentation will be necessary!");

                    FileUtils.removeDirectory(v.getCompiledSourcesDir());//delete compiled sources!
                    FileUtils.removeDirectory(v.getAdditionalCompiledSourcesDir()); //delete additional compiled source codes too!

                    FileUtils.removeDirectory(v.getHumanReadableBytecodeDir());//delete the folder housing exported bytecode during instrumentation as well!
                    FileUtils.removeDirectory(v.getAdditionalHumanReadableBytecodeDir()); //dito

                    FileUtils.removeDirectory(v.getNonInstrumentedDir()); //delete the non-instrumented code
                    FileUtils.removeDirectory(v.getAdditionalNonInstrumentedDir()); //dito

                    new File(v.getMainDirectory(),"callGraph").delete(); //delete the CallGraph file

                } else {
                    Tool.print("   - detected that instrumented sources for this version are already present! Skipping compilation of version!");
                    if(!gaveHintAlready) {
                        Tool.print("     NOTE: Use '" + Commands.forceOverwrite + "' or '" + Commands.cleanBeforeCompile + "' to force re-compilation but be aware that this");
                        Tool.print("           will also make re-instrumentation of the source code necessary! *DO NOT* simply");
                        Tool.print("           delete any of the instrumentation directories, as this will likely lead to corrupted");
                        Tool.print("           results due to faulty instrumentation. Use the *FLAGS ABOVE* unless you are the author");
                        Tool.print("           of this tool and know exactly what you are doing. User, you have been warned!");
                        gaveHintAlready = true;
                    }
                    return; //skip compilation!
                }
            }
            else Tool.print("   - preparing version for source code compilation");


            File directoryToExecuteIn;
            //check if user supplied the file directly
            if (buildFileOrDir.isDirectory()) {
                directoryToExecuteIn = buildFileOrDir;

                if (!buildTargetString.startsWith("mvn ") && !buildTargetString.startsWith("ant ")) {
                    //attempt to derive used build system if user has not provided command manually!
                    for (File f : directoryToExecuteIn.listFiles()) {
                        if (f.getName().equals("pom.xml")) {
                            buildTargetString = "mvn " + buildTargetString;
                            Tool.printExtraInfo("   - assuming this is an Maven project due to presence of 'pom.xml'!");
                            break;
                        } else if (f.getName().equals("build.xml")) {
                            Tool.printExtraInfo("   - assuming this is an ANT project due to presence of 'build.xml'!");
                            buildTargetString = "ant " + buildTargetString;
                            break;
                        }
                    }
                }
                if (!buildTargetString.startsWith("mvn ") && !buildTargetString.startsWith("ant "))
                    Tool.printError("Unknown build system used (fully supported are ANT and Maven)! Attempting compilation in directory '"+
                            directoryToExecuteIn+"' nonetheless!");
            }
            else{
                directoryToExecuteIn = buildFileOrDir.getParentFile(); //set to directory of build file!

                if (!buildTargetString.startsWith("mvn ") && !buildTargetString.startsWith("ant ")) {
                    //attempt to derive used build system if user has not provided command manually!
                    if (buildFileOrDir.getName().equals("pom.xml")) {
                        buildTargetString = "mvn " + buildTargetString;
                    }
                    else if (buildFileOrDir.getName().equals("build.xml")) {
                        buildTargetString = "ant " + buildTargetString;
                    } else Tool.printError("Unknown build system used (fully supported are ANT and Maven)! Attempting compilation in directory '"+
                            directoryToExecuteIn+"' nonetheless!");
                }
            }

            //a build file has been supplied, now invoke the build system (typically ANT or Maven):
            assert(directoryToExecuteIn.isDirectory());

            Tool.printExtraInfo("   - executing '" + buildTargetString + "' in '" + directoryToExecuteIn.getAbsolutePath() + "'");
            if (buildTargetString.startsWith("ant ")){
                //use ANT to compile the project
                Tool.printToolOutput("   -------------- output by ANT ----------------------------", OutputType.OUTPUT);
                ExecutionResult r = Executor.execute(buildTargetString, directoryToExecuteIn);
                Tool.printToolOutput("   -------------- end of output by ANT ---------------------", OutputType.OUTPUT);
                if (!r.hadErrors()) Tool.print("   - compiled sources using ANT, expecting directory '"+v.getCompiledSourcesDir().getAbsolutePath()+"' to contain the compiled code now!");
                else Tool.printError("ANT compilation process had errors, use '"+Commands.toolOut+"' to debug this problem!");
            }
            else if (buildTargetString.startsWith("mvn ")){
                //use Maven to compile the project
                Tool.printToolOutput("   -------------- output by Maven ----------------------------", OutputType.OUTPUT);
                ExecutionResult r = Executor.execute(buildTargetString, directoryToExecuteIn);
                Tool.printToolOutput("   -------------- end of output by Maven ---------------------", OutputType.OUTPUT);
                if (!r.hadErrors()) Tool.print("   - compiled sources using Maven, expecting directory '"+v.getCompiledSourcesDir().getAbsolutePath()+"' to contain the compiled code now!");
                else Tool.printError("Maven compilation process had errors, use '"+Commands.toolOut+"' to debug this problem!");
            } else {
                //simply execute whatever the user entered, maybe it is a different type of build system...
                Tool.printToolOutput("   -------------- output by build system ----------------------------", OutputType.OUTPUT);
                ExecutionResult r = Executor.execute(buildTargetString, directoryToExecuteIn);
                Tool.printToolOutput("   -------------- end of output by build system ---------------------", OutputType.OUTPUT);
                if (!r.hadErrors()) Tool.print("   - compiled sources using unknown build system, expecting directory '"+v.getCompiledSourcesDir().getAbsolutePath()+"' to contain the compiled code now!");
                else Tool.printError("Unknown compilation process had errors, use '"+Commands.toolOut+"' to debug this problem! Note that you can also simply provide the compiled sources by hand by not using '"+Commands.buildCommand+"'.");
            }
        }
        else{
            String s = "Failed to find '"+v.getBuildSpot().getAbsolutePath()+"', cannot compile version '"+v.identifier+"'!";
            Tool.printError(s);
            throw new IllegalArgumentException(s);
        }
    }
}
