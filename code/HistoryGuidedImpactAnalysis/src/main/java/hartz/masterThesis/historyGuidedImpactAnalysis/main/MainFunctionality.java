package hartz.masterThesis.historyGuidedImpactAnalysis.main;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configuration;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Behavior;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.Core;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileLevelChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.Trace;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph.CallGraph;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.TraceDivergence;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.ImpactBasedTestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.TestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.special.SyntaxChangeTestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.special.TraceDivergenceTestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * As the name suggests, this is the central class of the tool, controlling and invoking functionality from other classes.
 */
public class MainFunctionality {

    private final Configuration config;

    /** this will always contain at least two versions UNLESS the configuration
     * only holds the initial version of the repository, which is a special case! */
    private final Version[] versions;

    /**
     * Creates a new Core ready to analyze a given repository.
     *
     * @param config : defines what to do
     * @throws IllegalArgumentException if configuration object does not contain at least one version to mine
     */
    public MainFunctionality(Configuration config){
        this.config = config;

        if (config.getVersionIdentifiers().length==0)
            throw new IllegalArgumentException("No versions to mine have been provided!");

        Version[] v = new Version[config.getVersionIdentifiers().length];
        int i = 0;
        for (String identifier : config.getVersionIdentifiers()){
            Version ver = config.getAllVersionsOfRepo().getVersion(identifier);
            if (ver!=null){
                v[i] = ver;
                ++i;
            }
            else throw new IllegalArgumentException("Version with identifier '"+identifier+"' is part not of repository history!");
        }

        //will automatically detect and insert predecessor if necessary!
        if (v.length==1){
            int versionIndex = config.getAllVersionsOfRepo().indexOf(v[0]);
            if (versionIndex!= 0){
                Tool.print(" * only one version provided, will therefore compare given version against its immediate predecessor!'");
                Version tmp = v[0];
                v = new Version[2];
                v[0] = config.getAllVersionsOfRepo().getVersionByIndex(versionIndex-1);
                v[1] = tmp;
            }
            else{
                Tool.print(" * provided version is the first version of the repository, therefore there is no predecessor version to compare against!'");
            }
        }

        versions = v;
    }


    /**
     * Runs the tool's main functionality, exact nature and behavior depends on the configuration given beforehand.
     */
    public void execute(){
        if (versions.length==0){
            Tool.print(" * no versions have been set");
            return; //we cannot continue!
        }

        //###################  STEP 1: Enrich version objects with supplied data #############################
        boolean weHaveExecutionCommands;
        try {
            weHaveExecutionCommands = step1_enrichVersionObjects(versions, config);
        } catch (IOException e){
            Tool.printError("User input is malformed, IOException occurred while building canonical paths to required files. Cannot continue!");
            Tool.printError(e.getMessage());
            return;
        }

        //###################  STEP 2: Extract versions to output folder #############################
        HashMap<Version, List<FileLevelChange>> allFileLevelChanges = null;
        try {
            allFileLevelChanges = step2_extractVersionsFromRepo(versions, config);
        } catch (Exception e){
                Tool.printError("Failed to extract all versions from the repository! Cannot continue!");
                Tool.printError(e.getMessage());
                return;
            }
        if (config.getBehavior()==Behavior.EXTRACT) return;

        //################### STEP 3: Compile extracted sources, store paths #########################
        step3_compileExtractedVersions(versions, config);
        if (config.getBehavior()==Behavior.COMPILE) return;

        //################### STEP 4: Instrument present code (all of it) #############################
        Tool.print(" * starting instrumentation of source code:");
        TreeMap<Version, CallGraph> callGraphsByVersion = step4_instrumentCode(versions,
                config.exportSourceCodeToDirectory(),config.needToInjectCoverageObserverTwice(), config.useMoreCompatibleInstrumentation());
        if (config.getBehavior()==Behavior.INSTRUMENT) return;

        //################### STEP 5: Compute syntactical differences between versions ################
        if (versions.length<2){
            //check for initial version case!
            Tool.print(" * NOTE: cannot execute change approximation heuristics for initial version, as there is nothing to compare against!");
            return;
        }
        Tool.print(" * processing syntactic source code changes");
        assert(allFileLevelChanges != null);
        TreeMap<Version, HashMap<String, List<SyntaxChange>>> syntaxChangesByVersion = step5_computeSyntaxChanges(
                versions, allFileLevelChanges, !config.areRenamedElementsToBeTreatedAsSyntaxChanges());

        //################### STEP 6: Generate execution traces #######################################
        if (weHaveExecutionCommands) step6_generateExecutionTraces(versions, config.areTracesToBeDeleted());
        else Tool.print(" * no means of generating execution traces have been provided");
        if (config.getBehavior()==Behavior.DATA_COMPUTATION) return;

        //################### STEP 7: Compute trace divergences #######################################
        Tool.print(" * importing trace data:");
        TreeMap<Version, Trace[]> tracesByVersion = Core.gatherTraces(//import trace data
                versions, syntaxChangesByVersion, config.compareTraceCoverage(), config.compareTraceDistance());
        Tool.print(" * analyzing trace divergences:");
        TreeMap<Version, TraceDivergence[][]> traceDivergencesByVersion =
                step7_compareTraces(versions, tracesByVersion, config.areTracesToBeExportedAsList(),
                        config.needToUseManualTraceMatchingFile(), config.filterObjectCharacteristics());

        //################### STEP 8: Generate testing targets #########################################


        if (config.generateTargetsForSyntax()) {
            TreeMap<Version, TestingTarget[]> syntaxTargets = generateTestingTargetsForSyntax(syntaxChangesByVersion);
            Tool.print(" * generating testing targets based only on syntactical changes");
            exportTargets(syntaxTargets, "syntacticalChanges");
        }

        if (config.generateTargetsForDivergences()) {
            Tool.print(" * generating testing targets based only on trace divergences");
            TreeMap<Version, TestingTarget[]> traceDivergenceTargets = generateTestingTargetsForDivergences(traceDivergencesByVersion);
            exportTargets(traceDivergenceTargets, "traceDivergences");
        }

        if (syntaxChangesByVersion.values().isEmpty() && traceDivergencesByVersion.values().isEmpty()){
            Tool.print(" * no data available to obtain testing targets from!");
        } else {
            Tool.print(" * generating testing targets:");
            TreeMap<Version, TestingTarget[]> impactBasedTestingTargets = step8_generateTestingTargets(versions,
                    syntaxChangesByVersion, traceDivergencesByVersion, config.mapOnlyToFirstSyntaxChange(), callGraphsByVersion);
            exportTargets(impactBasedTestingTargets, ""/* default */);
        }

    }

    private void exportTargets(TreeMap<Version, TestingTarget[]> targets, String typeOfTarget){
        assert(typeOfTarget != null);

        for (Version v : targets.keySet()){
            JSONArray results = new JSONArray();
            for (TestingTarget t : targets.get(v))
                results.put(t.toJSON());

            try {
                String s = typeOfTarget.trim().replace(" ", "");
                if (!s.isEmpty()) s="_"+s;
                File testingTargets = new File(v.getMainDirectory().getAbsoluteFile() + "/testingTargets"+s);
                if (testingTargets.exists()) testingTargets.delete(); //always remove previous results!
                BufferedWriter writer = new BufferedWriter(new FileWriter(testingTargets));
                writer.write(results.toString(Globals.jsonIndentFactor));//store as JSON
                writer.close();
                if (!s.isEmpty())
                    Tool.printExtraInfo("   - exported testing targets based on "+typeOfTarget.replace("T", " t")+" for version "+v.identifier);
                else Tool.printExtraInfo("   - exported impact-based testing targets for version "+v.identifier);
            } catch (IOException e) {
                Tool.printDebug(e);
                Tool.printError("Failed to export "+typeOfTarget.replace("T", " t")+" testing targets for version '" + v.identifier + "', printing results instead!");
                Tool.activateDebugMode();
                Tool.print("---------- TESTING TARGET OUTPUT ----------------------------");
                Tool.print(results.toString(Globals.jsonIndentFactor));
                Tool.print("---------- END OF TESTING TARGET OUTPUT ---------------------");
                Tool.deactivateDebugMode();
            }
        }
    }



    public TreeMap<Version, TestingTarget[]> step8_generateTestingTargets(
            Version[] versions, TreeMap<Version, HashMap<String, List<SyntaxChange>>> syntaxChangesByVersion,
            TreeMap<Version, TraceDivergence[][]> traceDivergencesByVersion, boolean mapOnlyFirstInCallChain,
            TreeMap<Version, CallGraph> callGraphsByVersion){
        assert (versions.length >= 2);

        TreeMap<Version, TestingTarget[]> result = new TreeMap<>();
        for (Version v : traceDivergencesByVersion.keySet()) {
            if ((v.getVersionComparingAgainst() == null)) {
                continue;//this first version is the 'base', we compare against it but do NOT compare it against something, so skip!
            } else {

                /** We can basically classify our divergences into two categories, the first being at syntactically
                 * code sections and the second one being *CAUSED* by those changes. We map those without a direct
                 * syntax change to *ALL* syntactically changed methods higher in their call chain! Finally, there
                 * is the possibility that we have a divergence that cannot be attributed to any syntax change! */

                HashMap<String, List<SyntaxChange>> syntaxChangesForMethodOrClass = syntaxChangesByVersion.get(v);
                HashMap<String, List<TraceDivergence>> divergencesAtSyntaxChanges =
                        new HashMap<>(); //stores divergences for each syntactically changed method
                HashMap<String, List<TraceDivergence>> divergencesMappedToSyntaxChanges =
                        new HashMap<>(); //stores divergences associated with a syntactically changed method
                List<TraceDivergence> divergencesWithNoSyntaxChange =
                        new LinkedList<>(); //stores those divergences without any syntax change
                CallGraph callGraph = callGraphsByVersion.get(v);

                //this holds the results!
                ArrayList<TestingTarget> testingTargets = new ArrayList<>(syntaxChangesForMethodOrClass.size());


                for (TraceDivergence[] divergencesForTrace : traceDivergencesByVersion.get(v)) {
                    if (divergencesForTrace == null) continue; // = trace could not be matched!

                    for (TraceDivergence d : divergencesForTrace) {
                        MethodData divergedMethod = d.getMethodData();

                        if (divergedMethod.wasSyntacticallyModifiedInThisVersion()){
                            //comes from a modified section, obtain the right key to access this (either a class or method name)
                            String syntaxChangeKey;
                            if (divergedMethod.doesResideInNewClass())
                                syntaxChangeKey = divergedMethod.getClassName();
                            else syntaxChangeKey = divergedMethod.getIdentifier();

                            //map this trace divergence to either a method or a class that was changed!
                            if (divergencesAtSyntaxChanges.containsKey(syntaxChangeKey)){
                                divergencesAtSyntaxChanges.get(syntaxChangeKey).add(d);
                            } else {
                                LinkedList<TraceDivergence> l = new LinkedList<>();
                                l.add(d);
                                divergencesAtSyntaxChanges.put(syntaxChangeKey, l);
                            }
                        } else {
                            //alright, this does not come from a modified section!
                            assert (!syntaxChangesForMethodOrClass.containsKey(divergedMethod.getClassName()));
                            assert (!syntaxChangesForMethodOrClass.containsKey(divergedMethod.getIdentifier()));

                            /* since we already know that this method is not syntactically modified, the result
                             * of this next function will never include the method itself. */
                            List<String> keys = d.getCallChainSyntaxChangeAccessors(mapOnlyFirstInCallChain);
                            if (keys.isEmpty()){
                                /** trace divergence *WITHOUT* a syntactically changed method higher in the call chain,
                                 *  can for instance occur if the modified section was excluded from instrumentation
                                 *  (or if the program does random or environment dependent stuff). */
                                divergencesWithNoSyntaxChange.add(d);
                              }
                            else for (String key : keys){
                                assert (syntaxChangesForMethodOrClass.containsKey(key));
                                if (divergencesMappedToSyntaxChanges.containsKey(key)){
                                    divergencesMappedToSyntaxChanges.get(key).add(d);
                                } else {
                                    LinkedList<TraceDivergence> l = new LinkedList<>();
                                    l.add(d);
                                    divergencesMappedToSyntaxChanges.put(key,l);
                                }
                            }
                        }
                    }
                }


                /** This assertion code basically verifies above loop in regards to syntactic changes! */
                boolean assertsEnabled = false;
                assert (assertsEnabled = true /* yes I actually want to do that ;-) */);
                if (assertsEnabled){
                    for (List<TraceDivergence> l : divergencesAtSyntaxChanges.values()){
                        for (TraceDivergence t : l) assert (t.getMethodData().wasSyntacticallyModifiedInThisVersion());
                    }
                    for (List<TraceDivergence> l : divergencesMappedToSyntaxChanges.values()){
                        for (TraceDivergence t : l) assert (!t.getMethodData().wasSyntacticallyModifiedInThisVersion());
                    }
                    for (TraceDivergence t : divergencesWithNoSyntaxChange) {
                        assert (!t.getMethodData().wasSyntacticallyModifiedInThisVersion());
                    }
                }


                /** Generate targets now */
                for (String methodOrClass : syntaxChangesForMethodOrClass.keySet()){
                    List<TraceDivergence> directImpact = null;
                    if (divergencesAtSyntaxChanges.containsValue(methodOrClass)){
                        directImpact = divergencesAtSyntaxChanges.get(methodOrClass);
                    }
                    if (directImpact == null)
                        /** That means we have a syntax change that produced *ABSOLUTELY ZERO* semantic changes!
                         *  Most likely, this means that we are dealing with a class-level change, e.g. a field
                         *  that got added. However, if this is on method-level, then either the changed section
                         *  was not executed or the programmer has produced an equivalent mutant/section, his or
                         *  her change had no effect!*/
                        directImpact = new LinkedList<>();

                    List<TraceDivergence> indirectImpact = null;
                    if (divergencesMappedToSyntaxChanges.containsValue(methodOrClass)){
                        indirectImpact = divergencesMappedToSyntaxChanges.get(methodOrClass);
                    }
                    if (indirectImpact == null)
                        /** That means we have a syntax change had no *OBSERVABLE* effects on the rest of
                         *  the execution. Keep in mind that our trace observation is an abstraction! */
                        indirectImpact = new LinkedList<>();

                    List<SyntaxChange> syntaxChanges = syntaxChangesForMethodOrClass.get(methodOrClass);


                    if (assertsEnabled){
                        /* Verify that all syntax changes do in fact belong to this method (or class) */
                        String s = null;
                        Boolean notAboutMethod = null;
                        for (SyntaxChange ch : syntaxChangesForMethodOrClass.get(methodOrClass)){
                            if (s == null){
                                s = ch.getUniqueAccessString();
                                notAboutMethod = !ch.onMethodLevel();
                            }
                            else{
                                assert(s.equals(ch.getUniqueAccessString()));
                                assert(notAboutMethod.equals(!ch.onMethodLevel()));
                            }
                        }
                    }

                    /** Generate a target based on the impact we have just assembled! */
                    ImpactBasedTestingTarget impactBasedTarget = new ImpactBasedTestingTarget(
                            methodOrClass, syntaxChanges, directImpact, indirectImpact, callGraph, config);
                    testingTargets.add(impactBasedTarget);
                }


                /** Finally, deal with those divergences that could not be mapped */
                if (!divergencesWithNoSyntaxChange.isEmpty()) {
                    HashMap<String, List<TraceDivergence>> divergencesWithNoSyntaxChangeByMethod =
                            Core.restructureTraceDivergences(divergencesWithNoSyntaxChange);
                    for (String methodIdent : divergencesWithNoSyntaxChangeByMethod.keySet()) {
                        ImpactBasedTestingTarget impactBasedTargetWithoutSyntaxChange = new ImpactBasedTestingTarget(methodIdent,
                                new LinkedList<SyntaxChange>(), new LinkedList<TraceDivergence>(), //nothing local
                                divergencesWithNoSyntaxChangeByMethod.get(methodIdent), //only non-local
                                callGraph, config);
                        testingTargets.add(impactBasedTargetWithoutSyntaxChange);
                    }
                }

                /** Sort our targets according to priority (implemented as Comparable) */
                Collections.sort(testingTargets);
                result.put(v, testingTargets.toArray(new TestingTarget[testingTargets.size()]));
            }
        }



        return result;
    }


    /**
     * Will compare previously gathered execution traces and compute the divergences.
     *
     * @param versions : all versions to work on
     * @param tracesByVersion : contains the gathered traces for each version, all versions must be
     *                          represented as a key, their trace array may be empty but not 'null'
     *
     * @param exportAsList : set to 'true' to use the flat-export method instead of the tree-based one
     * @param manualMatching : set to 'true' to search for a user-provided trace matching file and use
     *                        'false' to disable that feature and always use the entry point matcher.
     * @param filterObjectCharacteristics : set to 'true' to stop comparing object stringOf and hashCodes
     *                                      when their string-representation contained the '@' character!
     * @return a map that contains for each version, the trace divergences as TraceDivergence[][].
     *         The outer array encodes the index of the *newer* trace and the inner holds the detected
     *         divergences (which may be an empty array). If the trace could not be matched, the inner
     *         array will be 'null' (to differentiate between "no divergences" and "no matching trace").
     */
    public static TreeMap<Version, TraceDivergence[][]> step7_compareTraces(
            Version[] versions, TreeMap<Version, Trace[]> tracesByVersion, boolean exportAsList, boolean manualMatching,
            boolean filterObjectCharacteristics) {

        assert(versions.length>=2);
        TreeMap<Version, TraceDivergence[][]> traceDivergencesByVersion = new TreeMap<>();
        for (int i = 0; i<versions.length; ++i) {
            Version v = versions[i];
            if ((v.getVersionComparingAgainst() == null)) {
                Tool.print("   - version '"+v.identifier+"' is the base to compare against ["+(i+1)+"/"+versions.length+"]");
                continue;//this first version is the 'base', we compare against it but do NOT compare it against something, so skip!
            }

            Trace[] tracesInOld = tracesByVersion.get(v.getVersionComparingAgainst());
            Trace[] tracesInNew = tracesByVersion.get(v);

            if (tracesInNew == null || tracesInOld == null || tracesInOld.length == 0 || tracesInNew.length == 0){
                Tool.print("   - cannot compute testing targets based on trace data without execution traces present in both this version and the one comparing against!'" +
                        v.identifier + "' [" + (i + 1) + "/" + versions.length + "]");
            } else {
                Tool.print("   - analyzing trace differences for version '" +
                        v.identifier + "' [" + (i + 1) + "/" + versions.length + "]");

                /* Compute the differences between the traces, results in a list holding each newer trace differences when
                 * compared to a matching older. Some of the lists contained may be empty of course... */
                ArrayList<List<TraceDivergence>> divergencesByTrace = Core.obtainTraceDivergences(tracesInNew, tracesInOld,
                                             manualMatching, v, filterObjectCharacteristics);

                TraceDivergence[][] result = new TraceDivergence[divergencesByTrace.size()][];
                int copyIndex = 0;
                for (List<TraceDivergence> l : divergencesByTrace){
                    if (l != null){
                        //could be matched
                        result[copyIndex] = l.toArray(new TraceDivergence[l.size()]);
                    }
                    ++copyIndex;
                }


                traceDivergencesByVersion.put(v,result);

                //Export the results!
                JSONArray output = new JSONArray();
                for (int div = 0; div<divergencesByTrace.size(); ++div){
                    List<TraceDivergence> divergences = divergencesByTrace.get(div);
                    JSONObject traceDivJSON;

                    //convert the list of divergences to a JSON tree structure!
                    if (divergences == null){
                        /** could not be matched */
                        traceDivJSON = new JSONObject();
                        traceDivJSON.put("entryPoint",tracesInNew[div].getEntryMethodIdentifier());
                        traceDivJSON.put("traceDivergences", "!-- none, trace could not be matched --!");
                        output.put(traceDivJSON);
                    } else if (!divergences.isEmpty()){
                        /** could be matched */
                        if (exportAsList)
                            traceDivJSON = TraceDivergence.convertTraceDivToJSON(divergences.iterator());
                        else
                            traceDivJSON = TraceDivergence.convertTraceDivToJSON(
                                    divergences.iterator(), divergences.get(0).getEntryPointMethodData());
                        output.put(traceDivJSON);
                    }
                }
                try {
                    File divergenceFile = new File(v.getMainDirectory().getAbsoluteFile() + "/traceDivergences");
                    if (divergenceFile.exists()) divergenceFile.delete(); //always remove previous results!
                    BufferedWriter writer = new BufferedWriter(new FileWriter(divergenceFile));
                    writer.write(output.toString(Globals.jsonIndentFactor));//store as JSON
                    writer.close();
                    Tool.printExtraInfo("   - exported trace divergences changes to file '" + divergenceFile.getAbsolutePath() + "'");
                } catch (IOException e) {
                    Tool.printDebug(e);
                    Tool.printError("Failed to export trace divergences for version '"+v.identifier+"', printing results instead!");
                    Tool.print("---------- TRACE DIVERGENCE OUTPUT ---------------------");
                    Tool.print(output.toString(Globals.jsonIndentFactor));
                    Tool.print("---------- END OF TRACE DIVERGENCE OUTPUT --------------");
                }
            }
        }
        return traceDivergencesByVersion;
    }


    public static void step6_generateExecutionTraces(Version[] versions, boolean deletePresentTraces) {
            Tool.print(" * generating execution traces");
            for (int i = 0; i<versions.length; ++i) {
                Version v = versions[i];
                Tool.print("   - generating trace data for version '" + v.identifier + "' [" + (i + 1) + "/" + versions.length + "]");
                if (deletePresentTraces){//remove old traces before generation of new ones
                    boolean purgedSomething = false;
                    for (File f : v.getMainDirectory().listFiles())
                        if (f.isFile() && f.getName().startsWith("observedTrace")) {
                            f.delete(); //purge this trace!
                            purgedSomething = true;
                        }

                    if (purgedSomething) Tool.printExtraInfo("     ~> removed trace data that was already present!");
                    else Tool.printExtraInfo("     ~> all observed traces will be persisted into '"+v.getMainDirectory().getAbsolutePath()+"'");
                }

                //execute everything we have (version will do nothing if data is lacking)
                if (v.executeRunCommands()) Tool.printExtraInfo("     ~> executed given run commands");
                if (v.executeMainMethods()) Tool.printExtraInfo("     ~> called provided main methods");
                if (v.executeTests()) Tool.printExtraInfo("     ~> executed JUnit tests");
            }
    }

    public static TreeMap<Version, HashMap<String, List<SyntaxChange>>> step5_computeSyntaxChanges(
            Version[] versions, HashMap<Version, List<FileLevelChange>> allFileLevelChanges, boolean ignoreRenaming) {
        assert(versions.length>=2);

        TreeMap<Version,HashMap<String, List<SyntaxChange>>> syntaxChangesByVersion = null;
        try{
            syntaxChangesByVersion = Core.computeSyntaxChanges(
                    versions, !ignoreRenaming, allFileLevelChanges);
        }
        catch (Exception e){
            Tool.printError(e.getMessage());
            Tool.printDebug(e);
            Tool.print(" * WARNING: Information about syntactic source code changes may be incomplete or corrupted!");
            if (syntaxChangesByVersion == null) syntaxChangesByVersion = new TreeMap<>();
        }
        return syntaxChangesByVersion;
    }

    public static TreeMap<Version, CallGraph> step4_instrumentCode(Version[] versions, boolean exportSource,
                                                                   boolean injectFunctionalityIntoAdditionalClasspathToo,
                                                                   boolean useStaticMaxStackIncrease) {
        //create class loaders now that the source has been build and verified :-)
        for (Version v : versions){
            v.createFreshClassLoader();
        }

        TreeMap<Version, CallGraph> callGraphs = new TreeMap<>();

        //Write the output to a class file
        for (int i = 0; i<versions.length; ++i) {
            Version v = versions[i];
            if (v.getNonInstrumentedDir().exists()){
                /* since the directory is deleted by the compilation phase, we do not need to check for the flag here. If it exits, we instrumented! */
                Tool.print("   - skipped instrumentation of '"+v.identifier+"', because code has been instrumented before ["+(i+1)+"/"+versions.length+"]");

                //import call graph!
                File callGraphFile = new File(v.getMainDirectory(),"callGraph");
                if (!callGraphFile.exists()) {
                    Tool.printError("Failed to find call graph for version '"+v.identifier+"', version could be corrupted! Using empty call graph for later steps!");
                    callGraphs.put(v, new CallGraph(new HashMap<String, List<String>>()) /* no null entries! */);
                } else {
                    try {
                        CallGraph g = new CallGraph(new JSONArray(FileUtils.readData(callGraphFile)));
                        callGraphs.put(v, g);
                    } catch (IOException e) {
                        Tool.printError("Failed to import call graph for version '"+v.identifier+"', data could not be accessed! Using empty call graph for later steps!");
                        callGraphs.put(v, new CallGraph(new HashMap<String, List<String>>()) /* no null entries! */);
                    }
                }
                continue;
            }

            Tool.printExtraInfo("   - starting instrumentation of '"+v.identifier+"' ["+(i+1)+"/"+versions.length+"]:");
            try{


                /* The user should select build commands that clean before compilation but hey, it is the user.
                 * Therefore, try to remove potentially injected code before instrumenting and injecting anew.
                 * Otherwise executing instrumentation twice may have undesired effects, namely instrumenting
                 * our own observation code! Cannot do anything about the bytecode though, need to trust the
                 * compilation phase here...
                 */
                FileUtils.clearCodeFromProjectUnderTest(v.getCompiledSourcesDir());

                callGraphs.put(v, Core.instrumentVersion(v, exportSource, injectFunctionalityIntoAdditionalClasspathToo, useStaticMaxStackIncrease));
                Tool.print("   - completed instrumentation of compiled source code for version '"+v.identifier+"' ["+(i+1)+"/"+versions.length+"]");
            }
            catch (Exception e){
                Tool.print("   - instrumentation failed for '" + v.identifier + "' [" + (i + 1) + "/" + versions.length + "], no traces will be produced! Reason: ");
                Tool.printError(e.getMessage());
                Tool.printDebug(e);
                callGraphs.put(v, new CallGraph(new HashMap<String, List<String>>()) /* no null entries! */);
            }
        }

        return callGraphs;
    }

    public static void step3_compileExtractedVersions(Version[] versions, Configuration config) {
        for (int i = 0; i<versions.length; ++i){
            Version v = versions[i];
            Tool.print(" * preparing extracted commit [" + (i + 1) + "/" + versions.length + "]: version '" + v.identifier + "'");
            try {
                Core.readyVersion(v, config.areCompiledSourcesToBeDeletedBeforeCompilation());
            } catch (Exception e) {
                Tool.printError(e.getMessage());
            }
        }
    }

    public static HashMap<Version, List<FileLevelChange>> step2_extractVersionsFromRepo(Version[] versions, Configuration config) throws IOException {
        HashMap<Version, List<FileLevelChange>> allFileLevelChanges = null;
        return Core.extractVersions(config, versions);
    }

    public static boolean step1_enrichVersionObjects(Version[] versions, Configuration config) throws IOException {
        int versionCounter = 0;
        boolean weHaveExecutionCommands = false;

        for (Version v : versions){

            File mainDir = new File(config.getOutputDirectory().getAbsolutePath()+"/"+v.index);

            //store all the paths in the version container
            v.enrichWithToolData(
                    mainDir,
                    config.getRelativePathToSources(versionCounter),
                    config.getRelativePathToCompiledSources(versionCounter),
                    config.getRelativePathToAdditionalCompiledSources(versionCounter),
                    config.getRelativePathToBuildFile(versionCounter),
                    config.getBuildCommand(versionCounter),
                    config.getIgnoreBySuffix(versionCounter),
                    config.getIgnoreByPrefix(versionCounter),
                    config.getExcludeMethodsFromInstrumentationBySignature(versionCounter),
                    config.getDirectoriesExcludedFromInstrumentation(versionCounter),
                    (versionCounter == 0) ? null /* = we do not want to compute anything */ : versions[versionCounter-1],
                    config.areRenamedElementsToBeTreatedAsSyntaxChanges(),
                    config.exportSourceCodeToDirectory()
            );

            v.storeRunCommands(
                    config.getRunCommands(versionCounter),
                    config.getRelativePathToTests(versionCounter),
                    config.getTestsToRun(versionCounter),
                    config.getTestsToSkip(versionCounter),
                    config.getExecuteMainClassnames(versionCounter),
                    config.getExecuteMainArguments(versionCounter)
            );

            weHaveExecutionCommands = weHaveExecutionCommands || v.canGenerateTraces();

            ++versionCounter;
        }
        return weHaveExecutionCommands;
    }

    public TreeMap<Version, TestingTarget[]> generateTestingTargetsForSyntax(
            TreeMap<Version, HashMap<String, List<SyntaxChange>>> syntaxChangesByVersion) {
        assert(versions.length>=2);

        TreeMap<Version, TestingTarget[]> result = new TreeMap<>();

        for (Version v : syntaxChangesByVersion.keySet()) {
            if ((v.getVersionComparingAgainst() == null)){
                continue;//this first version is the 'base', we compare against it but do NOT compare it against something, so skip!
            } else {
                ArrayList<TestingTarget> testingTargets = new ArrayList<>();

                HashMap<String, List<SyntaxChange>> syntaxChanges = syntaxChangesByVersion.get(v);
                //generate targets for each syntax change
                for (String changedEntity : syntaxChanges.keySet())
                    testingTargets.add(new SyntaxChangeTestingTarget(syntaxChanges.get(changedEntity)));

                Collections.sort(testingTargets); //sort according to priority (see Target Class)

                result.put(v, testingTargets.toArray(new TestingTarget[testingTargets.size()]));
            }
        }

        return result;
    }

    public TreeMap<Version, TestingTarget[]> generateTestingTargetsForDivergences(
            TreeMap<Version, TraceDivergence[][]> traceDivergencesByVersion) {
        assert(versions.length>=2);

        TreeMap<Version, TestingTarget[]> result = new TreeMap<>();

        for (Version v : traceDivergencesByVersion.keySet()) {
            if ((v.getVersionComparingAgainst() == null)){
                continue;//this first version is the 'base', we compare against it but do NOT compare it against something, so skip!
            } else {
                LinkedList<TestingTarget> testingTargets = new LinkedList<>();

                TraceDivergence[][] traceDivergences = traceDivergencesByVersion.get(v);
                HashMap<String, List<TraceDivergence>> divergencesByMethod = Core.restructureTraceDivergences(traceDivergences);

                //generate targets for each method divergence
                for (String methodWithDivergence : divergencesByMethod.keySet())
                    testingTargets.add(new TraceDivergenceTestingTarget(methodWithDivergence, divergencesByMethod.get(methodWithDivergence)));

                Collections.sort(testingTargets); //sort according to priority (see Target Class)

                result.put(v, testingTargets.toArray(new TestingTarget[testingTargets.size()]));
            }
        }

        return result;
    }




    //TODO
        /*
        Tool.print(" * starting test target computation");
        TreeMap<Version, List<Target>> targets = Core.changeApproximation(versions, traces);

        int targetVersionCounter = 0;
        if (!targets.keySet().isEmpty()) Tool.print(" * exporting computed testing targets");
        for (Version v : targets.keySet()){
            try{
                //export computed targets
                File testTargetFile = new File (v.getMainDirectory().getAbsolutePath()+"/testTargets");
                if (testTargetFile.exists()){
                    Tool.printExtraInfo("   - overwriting test targets file for '"+v.identifier+"' ["+(targetVersionCounter+1)+"/"+targets.keySet().size()+"]");
                    testTargetFile.createNewFile();
                }

                //create a JSON-object
                List<Target> targetsForVersion = targets.get(v);
                JSONArray array = new JSONArray();
                for (Target t : targetsForVersion){
                    array.put(t.toJSONObject());
                }

                    //store to drive
                    if(testTargetFile.exists()) testTargetFile.createNewFile();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(testTargetFile));
                    writer.write(array.toString(Globals.jsonIndentFactor));//store as JSON
                    writer.close();
                    Tool.printExtraInfo("   - exported test targets for '"+v.identifier+"' to file '"+testTargetFile.getAbsolutePath()+"' ["+(targetVersionCounter+1)+"/"+targets.keySet().size()+"]");
            }
            catch (Exception e){
                Tool.printExtraInfo("   - failed to exported test targets for '" + v.identifier + "' due to '" + e.getMessage() + "' [" + (targetVersionCounter + 1) + "/" + targets.keySet().size() + "] Printing results to console instead:");
                Tool.printError("-----------------------  TARGETS  -------------------------------");
                List<Target> targetsForVersion = targets.get(v);
                for (Target t : targetsForVersion){
                    Tool.printError(t.toJSONObject().toString(Globals.jsonIndentFactor));
                }
                Tool.printError("------------------- END OF TARGETS ------------------------------");
            }
            ++targetVersionCounter;
        }
        */


}
