package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.ObservedMethod;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

/**
 * Special version of the coverage observer that does not register itself as a shutdown hook and instead
 * relies on the fact that this class is in fact already loaded in the same JVM. Is used ONLY by tests
 * or when the JUnit Runner is used instead of calling tests externally! 
 * This whole feature is setup in such a way that an instrumented version can be executed both externally
 * or make use of this class in the context of tool application, you only need to call the constructor
 * each time before you start an execution!
 * This hook will NOT export its data automatically! You need to call this functionality after
 * the test or JUnitRunner is finished!
 */
public class CoverageObserverNoHook extends CoverageObserver {

    public static File exportTargetWhileAlreadyLoaded;
    protected static CoverageObserverNoHook noHookInstance = null;

    /**
     * Constructs and initializes a new instance of the CoverageObserver without a hook!
     * Call this only when using tests or the JUnit Runner and NOT when the intention
     * is to create coverage data using external measures!
     *
     * @param coverageFile : where to export coverage data
     */
    public CoverageObserverNoHook(File coverageFile) {
        super();
        exportTargetWhileAlreadyLoaded = coverageFile;
        noHookInstance = this;
    }




    @Override
    public synchronized void export(){
        Tool.printDebug(" * exporting coverage data to file system");

        if (!currentMethods.isEmpty()){
            errors.add("Execution Trace Data Export to file was requested even though not all methods have returned! Method(s) still on callstack:");
            for (ObservedMethod c : currentMethods.values()){
                errors.add("  - "+c.getIdentifier());
            }
        }

        JSONObject toExport = new JSONObject();
        JSONArray callTreeJSON = new JSONArray();

        boolean needToGetData = true;
        int waited = 0; //used to escape observed code that does not terminate
        while(needToGetData){
            try{
                LinkedList<ObservedMethod> copyOfCallTree = new LinkedList<>(callTree);
                for (ObservedMethod c : copyOfCallTree){
                    callTreeJSON.put(c.toJSON());
                }
                toExport.put("executionTraces", callTreeJSON);
                if (!errors.isEmpty())
                    toExport.put("errors", new JSONArray(errors));

                needToGetData =  false; //success, stop trying!
            }
            catch (ConcurrentModificationException e){
            /* something was still executing, drop incomplete data if there is any... */
                toExport = new JSONObject();
                callTreeJSON = new JSONArray();
                Tool.printDebug(e);

                try {
                    if (waited <= Globals.maxTimeToWaitWhenObservedCodeDoesNotTerminate){
                        Tool.printError("Execution seems to be incomplete, code is still being observed and data is written! Retrying in 5 seconds...");
                        Thread.sleep(5000);
                        waited+=5000;
                    } else {
                        String s = "Observed execution did not terminate, data might be incomplete or corrupted!";
                        if (toExport.has("errors"))
                            toExport.getJSONArray("errors").put(s);
                        else toExport.put("errors", new JSONArray(errors));
                        Tool.printError(s);
                        needToGetData = false; //stop trying
                    }
                } catch (InterruptedException e1) { /* we do not care*/ }
            }
        }

        try{
            exportTargetWhileAlreadyLoaded.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(exportTargetWhileAlreadyLoaded));
            writer.write(toExport.toString(Globals.jsonIndentFactor));//store as JSON
            writer.close();
        }
        catch(IOException e){
            Tool.printError("Failed to store Runtime Coverage Data!");
            Tool.printDebug(e);
        }

        callTree.clear();
        errors.clear();
        currentMethods.clear();
        ObservedMethod.resetTraceIndexCounter(); //need to reset this for next execution!
    }
}

