package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.TraceDivergence;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Container class for an execution trace. A trace has an entry point and terminates at some point, note that
 * a single 'observedTrace' file can contain multiple traces!
 */
public class Trace {

    private final String identifier;
    private final MethodData entryMethod;
    private final Version version;

    /**
     * Imports a trace from JSON.
     *
     * @param belongsTo : version this trace belongs to
     * @param traceData : JSONObject to import
     * @param syntaxChanges : syntactic changes detected in this version
     * @param identifier : unique identifier for this trace (unique for version, not globally)
     */
    public Trace(Version belongsTo, JSONObject traceData, HashMap<String, List<SyntaxChange>> syntaxChanges, String identifier) {
        version = belongsTo;
        entryMethod = new MethodData(version,traceData,null,syntaxChanges);
        this.identifier = identifier;
    }

    public String getEntryMethodIdentifier(){
        return entryMethod.getIdentifier();
    }

    public String getTraceIdentifier(){
        return identifier;
    }

    /**
     * Performs trace divergence analysis.
     *
     * @param traceInNewVersion : trace to compare against, supposed to be from the future version
     * @param filterObjectCharacteristics : set to 'true' to filter out parameter and return value
     *                                      divergences for objects that contained the '@' char.
     * @return list of divergences for this instance when compared to the given trace
     */
    public List<TraceDivergence> compareAgainstNewerTrace(Trace traceInNewVersion, boolean filterObjectCharacteristics){
        //relay my method data for comparison operation!
        LinkedList<TraceDivergence> traceDivergences = traceInNewVersion.compareMethodData(entryMethod, filterObjectCharacteristics);
        return traceDivergences;
    }

    protected LinkedList<TraceDivergence> compareMethodData(MethodData olderData, boolean filterObjectCharacteristics){
        LinkedList<TraceDivergence> traceDivergences = new LinkedList<>();

        if (entryMethod.isSameMethod(olderData)) {
            /** Only start comparing traces that start at the same place! Generate warnings if code does not start the same way...*/

            if (entryMethod.wasSyntacticallyModifiedInThisVersion()){
                Tool.printExtraInfo("        -> WARNING: Trace Entry point is was syntactically modified! It is recommended to generate" +
                        " traces that begin in unmodified code, e.g. a testsuite.");
            } else {
                if (entryMethod.getAmountOfParameters() != olderData.getAmountOfParameters()) {
                    //this should actually never happen since in that case, a syntax modification should be detected!
                    Tool.printExtraInfo("        -> WARNING: Trace Entry points take a different amount of parameters!" +
                            " It is recommended to generate traces that begin in unmodified code, e.g. a testsuite.");
                } else {
                    String differentParameters = "";
                    for (int i = 0; i < entryMethod.getAmountOfParameters(); ++i) {
                        String strRepOfParameter = entryMethod.getClassOfParameter(i) + " '[" + entryMethod.getToStringOfParameter(i)
                                + "]' hash: " + entryMethod.getHashCodeOfParameter(i);
                        String strRepOfOldParameter = olderData.getClassOfParameter(i) + " '[" + olderData.getToStringOfParameter(i) +
                                "]' hash: " + olderData.getHashCodeOfParameter(i);
                        if (!strRepOfParameter.equals(strRepOfOldParameter)) {
                            differentParameters+="\n        -> '"+strRepOfParameter+"' != '"+strRepOfOldParameter+"'";
                        }
                    }
                    if (!differentParameters.isEmpty()){
                        differentParameters = "        -> WARNING: Trace Entry points were invoked with different parameters!" +differentParameters +"\n"+
                                "It is recommended to generate traces that begin in unmodified code and are similarly invoked, e.g. by using a testsuite";
                        Tool.printExtraInfo(differentParameters);
                    }
                }
            }

            entryMethod.computeDifferences(olderData, traceDivergences, filterObjectCharacteristics);
        } else {
            Tool.printExtraInfo("        -> WARNING: Trace Entry point is different, check your trace matching file! Cannot start trace divergence analysis!");
        }

        return traceDivergences;
    }


}
