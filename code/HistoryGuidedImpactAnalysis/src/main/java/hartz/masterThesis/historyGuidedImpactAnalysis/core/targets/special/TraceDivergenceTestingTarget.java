package hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.special;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.ImpactBasedTestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.TestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Special target used when one is only interested in trace divergences. Basically just aggregates divergence information
 *  and attempts to sort them in a meaningful way. */
public class TraceDivergenceTestingTarget extends TestingTarget {

    protected final String identifier;
    protected final TraceDivergence[] traceDivergenceneObjects;

    protected final int returnValDiffAll;
    protected final int parameterDiffAll;
    protected final int divergentSectionsAll;
    protected final int distanceOfAll;
    protected final float coverageChangesAll;
    protected final float methodCallChangesAll;

    protected final float averageReturnValDiff;
    protected final float averageParameterDiff;/** note: this models how this method called others and not its own parameters!*/
    protected final float averageDivergentSections;
    protected final float averageTraceDistance;
    protected final float averageCoverageChanges;
    protected final float averageMethodCallChanges;

    protected final boolean syntacticallyModified;
    protected final HashSet<MethodCallDivergence> methodsInvoked;


    public TraceDivergenceTestingTarget(String methodIdentifier, List<TraceDivergence> traceDivergences) {
        traceDivergenceneObjects = traceDivergences.toArray(new TraceDivergence[traceDivergences.size()]);
        this.identifier = methodIdentifier;

        int divSecAll = 0;
        int distAll = 0;
        int paramAll = 0;
        int returnValAll = 0;
        float averageParam = 0;
        float averageReturnVal = 0;
        float averageDivSections = 0;
        float averageDistance = 0;
        HashMap<MethodData, Integer> coverageChanges = new HashMap<>();
        HashMap<MethodData, Integer> invocationChanges = new HashMap<>();

        boolean syntacticallyModifiedTmp = false;
        HashSet<String> noDuplicatMethodCalls = new HashSet<>();
        methodsInvoked = new HashSet<>();

        for (TraceDivergence d : traceDivergenceneObjects){
            assert(d.getMethodData().getIdentifier().equals(identifier)); //otherwise, construction is broken!
            syntacticallyModifiedTmp = d.getMethodData().wasSyntacticallyModifiedInThisVersion() || syntacticallyModifiedTmp;

            if (d instanceof MetricDivergence){
                MetricDivergence m = (MetricDivergence) d;
                if (m.type == DivergenceType.TRACE_DIVERGENT_SECTIONS) {//there is only one of these per trace!
                    divSecAll += m.metricValue;
                    ++averageDivSections;
                }
                else{
                    assert( m.type == DivergenceType.TRACE_DISTANCE );//there is only one of these per trace!
                    distAll+= m.metricValue;
                    ++averageDistance;
                }

            } else if (d instanceof CoverageDivergence){
                /* Since we only deal with the same method in different traces here, we can use the MethodData as key! */
                CoverageDivergence c = (CoverageDivergence) d;
                MethodData key = c.getMethodData();
                if (coverageChanges.containsKey(key)){
                    coverageChanges.put(key, coverageChanges.get(key) + c.getCoverageDifference());
                } else{
                    coverageChanges.put(key, c.getCoverageDifference());
                }

            } else if (d instanceof MethodCallDivergence){
                MethodCallDivergence m = (MethodCallDivergence) d;
                MethodData key = m.getMethodData();
                if (invocationChanges.containsKey(key)){
                    invocationChanges.put(key, invocationChanges.get(key) + 1);
                } else{
                    invocationChanges.put(key, 1);
                }

                //also gather all the different functions that get called!
                String s = m.getInterestingMethodIdentifier();
                if (!noDuplicatMethodCalls.contains(s)){
                    noDuplicatMethodCalls.add(s);
                    methodsInvoked.add(m);
                }

            } else if (d instanceof ObjectValueDivergence){
                ObjectValueDivergence o = (ObjectValueDivergence) d;
                if (o.type == DivergenceType.PARAMETER) {
                    /* There is one of these for each difference per trace! */
                    paramAll += o.getNumberOfDifferencesInRecordedProperties();
                    ++averageParam;
                }
                else {
                    /* There is only one of these per trace! */
                    returnValAll+= o.getNumberOfDifferencesInRecordedProperties();
                    ++averageReturnVal;
                }

            } else throw new InternalError(
                    "Programmer has extended 'TraceDivergence' but forgotten to expand this piece of code as well!");

        }


        //now that we have computed all the values, store them in the final fields:
        divergentSectionsAll = divSecAll;
        distanceOfAll = distAll;
        parameterDiffAll = paramAll;
        returnValDiffAll = returnValAll;

        averageDivergentSections = (divergentSectionsAll != 0)? divergentSectionsAll /averageDivSections:  0f;
        averageTraceDistance =(distanceOfAll != 0)? distanceOfAll / averageDistance:  0f;
        averageParameterDiff = (parameterDiffAll != 0)? parameterDiffAll /averageParam:  0f;
        averageReturnValDiff = (returnValDiffAll != 0) ? returnValDiffAll /averageReturnVal:  0f;

        int c = 0;
        for (Integer i : coverageChanges.values()) c+=i;
        coverageChangesAll = c;
        averageCoverageChanges = (c != 0) ? c/coverageChanges.keySet().size(): 0f;

        int m = 0;
        for (Integer i : invocationChanges.values()) m+=i;
        methodCallChangesAll = m;
        averageMethodCallChanges = (m != 0) ? m/invocationChanges.keySet().size(): 0f;

        syntacticallyModified = syntacticallyModifiedTmp;
    }

    public JSONObject toJSON(){
        JSONObject result = new JSONObject();
        result.put("-> testingTarget", identifier);

        if (includeDetails) {
            JSONArray testBecause = new JSONArray();
            if (averageTraceDistance != 0f) {
                JSONObject o = new JSONObject();
                o.put("averageAmountOfDifferentInstructions", averageTraceDistance);
                o.put("overallNumberOfDifferentInstructionsExecutedOverAllTraces", distanceOfAll);
                testBecause.put(o);
            }
            if (averageDivergentSections != 0f) {
                JSONObject o = new JSONObject();
                o.put("averageNumberOfDivergentCodeSections", averageDivergentSections);
                o.put("overallNumberOfDivergentSectionsOverAllTraces", divergentSectionsAll);
                testBecause.put(o);
            }
            if (averageParameterDiff != 0f) {
                JSONObject o = new JSONObject();
                o.put("averageParameterDifference", averageParameterDiff);
                o.put("parameterDifferenceMetricValueOverAllTraces", parameterDiffAll);
                testBecause.put(o);
            }
            if (averageReturnValDiff != 0f) {
                JSONObject o = new JSONObject();
                o.put("averageReturnValueDifference", averageReturnValDiff);
                o.put("returnValueDifferenceMetricValueOverAllTraces", returnValDiffAll);
                testBecause.put(o);
            }
            if (averageCoverageChanges != 0f) {
                JSONObject o = new JSONObject();
                o.put("averageCoverageDifference", averageCoverageChanges);
                o.put("coverageDifferenceMetricValueOverAllTraces", coverageChangesAll);
                testBecause.put(o);
            }
            if (averageMethodCallChanges != 0f) {
                JSONObject o = new JSONObject();
                o.put("averageDifferentMethodCalls", averageMethodCallChanges);
                o.put("overallNumberOfDifferentMethodCallsOverAllTraces", methodCallChangesAll);
                JSONArray differentMethods = new JSONArray();
                for (MethodCallDivergence change : methodsInvoked)
                    differentMethods.put(change.toJSON());
                o.put("typesOfDifferentMethodCalls", differentMethods);

                testBecause.put(o);
            }
            result.put("testBecause", testBecause);
        } else {
            result.put("combinedMetricValue", getCombinedValue());
        }


        return result;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int compareTo(TestingTarget testingTarget) {
        if (testingTarget instanceof ImpactBasedTestingTarget)
            return 1; //impact based targets are the real deal! Should actually never be compared to those!

        if (testingTarget instanceof SyntaxChangeTestingTarget) return 1; //targets from syntax are more important
        assert(testingTarget instanceof TraceDivergenceTestingTarget);
        TraceDivergenceTestingTarget t = (TraceDivergenceTestingTarget) testingTarget;

        float metric = getCombinedValue();
        float otherMetric = t.getCombinedValue();

        float result = otherMetric - metric; //if I have more, then the number is negative!
        if (Math.abs(result)<=0.01f)
            return 0;
        else return result  > 0.f ? 1 : -1 ;
    }

    private float getCombinedValue() {
        return averageDivergentSections +
                     (syntacticallyModified ? //ignore distance and the like when there are syntactic modifications!
                             0f : averageTraceDistance + averageCoverageChanges + methodsInvoked.size()) +
                     averageReturnValDiff+
                     averageParameterDiff;
    }
}
