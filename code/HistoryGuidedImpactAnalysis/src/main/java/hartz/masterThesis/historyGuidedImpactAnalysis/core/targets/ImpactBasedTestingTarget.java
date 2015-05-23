package hartz.masterThesis.historyGuidedImpactAnalysis.core.targets;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.MetricScaling;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.Core;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChangeType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph.CallGraph;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.special.SyntaxChangeTestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.special.TraceDivergenceTestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** A testing target based on impact of a syntax change. See thesis for more information! */
public class ImpactBasedTestingTarget extends TestingTarget {

    protected final String identifier;
    protected final List<SyntaxChange> syntaxChanges;

    protected final boolean hasDirectChanges; //if not, either change was not executed or its an equivalent section/mutant!
    protected final boolean isOnClassLevel;
    protected final boolean newClassTarget;

    protected final HashSet<String> methodsWithDifferentBehavior;
    protected final HashMap<String, List<TraceDivergence>> nonLocalDivergences;

    //all of these come from the *DIRECT IMPACT* only!
    protected final float local_averageReturnValDiff;
    protected final float local_averageParameterDiff; /** note: this models how this method called others and not its own parameters!*/
    protected final float local_averageDivergentSections;
    protected final float local_averageTraceDistance;
    protected final float local_averageCoverageChanges;
    protected final float local_averageMethodCallChanges;
    protected final float local_amountOfSyntaxChanges; //this is scaled, remember? Number would be 'syntaxChanges.size()'

    //all of these come from the *INDIRECT IMPACT* only!
    protected final double nonLocal_averageReturnValDiff;
    protected final double nonLocal_averageParameterDiff;/** note: this models how this method called others and not its own parameters!*/
    protected final double nonLocal_averageDivergentSections;
    protected final double nonLocal_averageTraceDistance;
    protected final double nonLocal_averageCoverageChanges;
    protected final double nonLocal_averageMethodCallChanges;
    protected final double nonLocal_affectedMethods; //this is scaled, remember? Number would be 'methodsWithDifferentBehavior.size()'

    /**
     * Creates a new impact-based testing target
     *
     * @param methodOrClassIdentifierOfTarget : identifier of object this target is for
     * @param syntaxChanges : syntactic changes INSIDE the object this target is for
     * @param localImpact : trace divergences in ALL OBSERVED TRACES for this version that
     *                      were detected INSIDE the object this target is for
     * @param nonLocalImpact : trace divergences in ALL OBSERVED TRACES for this version that
     *                         were detected OUTSIDE the object this target is for
     * @param callGraph : call graph of this version, may be 'null'
     * @param scaling : object defining how to scale the metrics derived from the data above
     */
    public ImpactBasedTestingTarget(String methodOrClassIdentifierOfTarget, List<SyntaxChange> syntaxChanges,
                                    List<TraceDivergence> localImpact, List<TraceDivergence> nonLocalImpact,
                                    CallGraph callGraph, MetricScaling scaling) {
        assert(methodOrClassIdentifierOfTarget != null && !methodOrClassIdentifierOfTarget.isEmpty());
        assert(syntaxChanges != null); //mightBeEmpty
        assert(localImpact != null); //might be empty
        assert(nonLocalImpact != null); //might be empty

        this.identifier = methodOrClassIdentifierOfTarget;
        this.syntaxChanges = syntaxChanges;
        this.hasDirectChanges = !localImpact.isEmpty();
        this.isOnClassLevel = syntaxChanges.isEmpty()
                ? false /* = comes from a trace divergence that could not be mapped */
                : !syntaxChanges.get(0).onMethodLevel();
        this.newClassTarget = syntaxChanges.isEmpty()
                ? false /* = same as above */
                : syntaxChanges.get(0).getTypeOfSyntaxChange() == SyntaxChangeType.NEW_CLASS;
        assert(!newClassTarget || syntaxChanges.size() == 1 /* = new class may only have one Syntax Change */);

        //compute local averages, which can give us an indication if this was actually executed!
        if (!localImpact.isEmpty()){
            HashMap<String, int[][]> localImpactData = computeNumbersForDivergences(localImpact);
            assert(localImpactData.keySet().size() == 1 /* only the syntactically modified method should be present */);

            int[][] data = localImpactData.get(methodOrClassIdentifierOfTarget);
            float divergentSectionAverage = computeAverage(data[0]);
            local_averageDivergentSections = divergentSectionAverage * scaling.getDivergendSectionsScaler(false);
            local_averageTraceDistance = computeAverage(data[1]) * ((divergentSectionAverage == 0.f) ?
                    scaling.getDistanceOnlyScaler(false) : scaling.getTraceDistanceScaler(false));
            local_averageReturnValDiff = computeAverage(data[2]) * scaling.getReturnValueScaler(false);
            local_averageParameterDiff = computeAverage(data[3]) * scaling.getParameterScaler(false);
            local_averageCoverageChanges = computeAverage(data[4]) * scaling.getCoverageScaler(false);
            local_averageMethodCallChanges = computeAverage(data[5]) * scaling.getMethodCallScaler(false);
        } else {
            /* Probably a class-level change. However, it might also be an equivalent section or or the change
             * was not executed or we have a target from a trace divergence that could not be mapped to a syntax
             * change. Either way, at least its local impact is nonexistent for our metric! */
            local_averageDivergentSections = 0f;
            local_averageTraceDistance = 0f;
            local_averageReturnValDiff = 0f;
            local_averageParameterDiff = 0f;
            local_averageCoverageChanges = 0f;
            local_averageMethodCallChanges = 0f;
        }
        local_amountOfSyntaxChanges = syntaxChanges.size() * scaling.getSyntaxChangesScaler(newClassTarget);

        //compute nonLocal averages, which (at least by default) are far more interesting!
        double nonLocal_averageDivergentSectionsTmp = 0f;
        double nonLocal_averageTraceDistanceTmp = 0f;
        double nonLocal_averageReturnValDiffTmp = 0f;
        double nonLocal_averageParameterDiffTmp = 0f;
        double nonLocal_averageCoverageChangesTmp = 0f;
        double nonLocal_averageMethodCallChangesTmp = 0f;
        methodsWithDifferentBehavior = new HashSet<>();

        if (!nonLocalImpact.isEmpty()){
            HashMap<String, int[][]> nonLocalImpactData = computeNumbersForDivergences(nonLocalImpact);
            
            for (String methodIdentifier : nonLocalImpactData.keySet()){
                int[][] data = nonLocalImpactData.get(methodIdentifier);

                /* Compute the minimal distance between the syntax change and the point of divergence in method calls. Note
                 * that we use the shortest distance in the entire program here and not necessarily what was executed in the
                 * trace. Note further that if this is models a class-level syntax change, the distance-call with return
                 * immediately with 'not found', as only methods are part of the call graph ;-)
                 */

                int minimalCallsBetweenThisAndSyntaxChange = callGraph.getDistanceToCaller(
                        methodIdentifier, methodOrClassIdentifierOfTarget);
                float distance = (minimalCallsBetweenThisAndSyntaxChange == Integer.MIN_VALUE) ? 0f :
                        minimalCallsBetweenThisAndSyntaxChange * scaling.getCallDistanceScaler();

                float divergentSectionAverage = computeAverage(data[0]);
                nonLocal_averageDivergentSectionsTmp += divergentSectionAverage * scaling.getDivergendSectionsScaler(true) + distance;
                nonLocal_averageTraceDistanceTmp += computeAverage(data[1]) * ((divergentSectionAverage == 0.f) ?
                        scaling.getDistanceOnlyScaler(true) : scaling.getTraceDistanceScaler(true)) + distance;
                nonLocal_averageReturnValDiffTmp += computeAverage(data[2]) * scaling.getReturnValueScaler(true) + distance;
                nonLocal_averageParameterDiffTmp += computeAverage(data[3]) * scaling.getParameterScaler(true) + distance;
                nonLocal_averageCoverageChangesTmp += computeAverage(data[4]) * scaling.getCoverageScaler(true) + distance;
                nonLocal_averageMethodCallChangesTmp += computeAverage(data[5]) * scaling.getMethodCallScaler(true) + distance;

                if(!methodIdentifier.equals(identifier) || !hasDirectChanges)
                    methodsWithDifferentBehavior.add(methodIdentifier);
            }

            nonLocalDivergences = Core.restructureTraceDivergences(nonLocalImpact);
            assert(nonLocalDivergences.keySet().equals(methodsWithDifferentBehavior));

        } else {
          //it has no non-local effects!
            nonLocalDivergences = new HashMap<>();
        }

        nonLocal_affectedMethods = methodsWithDifferentBehavior.size() * scaling.getNumberOfAffectedMethodsScaler();
        nonLocal_averageDivergentSections = nonLocal_averageDivergentSectionsTmp;
        nonLocal_averageTraceDistance = nonLocal_averageTraceDistanceTmp;
        nonLocal_averageReturnValDiff = nonLocal_averageReturnValDiffTmp;
        nonLocal_averageParameterDiff = nonLocal_averageParameterDiffTmp;
        nonLocal_averageCoverageChanges = nonLocal_averageCoverageChangesTmp;
        nonLocal_averageMethodCallChanges = nonLocal_averageMethodCallChangesTmp;



    }


    private float computeAverage(int[] array){
        if (array.length == 0) return 0f;
        float all = 0;
        for (int i : array) all+=i;
        return all/array.length;
    }

    /**
     * Aggregates data from a list of trace divergences, see {@link hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.TraceDivergence}
     * and {@link hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.DivergenceType} for more information.
     *
     * @param divs a list of trace divergences
     * @return HashMap that maps method identifiers onto an array of metric values derived from all the
     *         concrete method invocations. The inner arrays contain values for a particular metric taken
     *         from all found invocations of the method over all trace divergences given.
     *
     *         The outer array is always of length 6 and structures access to these metrics:
     *         {
     *            array containing all diverging section metric values encountered for this method,
     *            array containing all trace distance metric values encountered for this method,
     *            array containing all return-value difference metric values encountered for this method,
     *            array containing all parameter distance metric values encountered for this method,
     *            array containing all code coverage metric values encountered for this method,
     *            array containing all invocation changes metric values encountered for this method
     *         }
     */

    public HashMap<String, int[][]> computeNumbersForDivergences(List<TraceDivergence> divs){
        //TRACE_DIVERGENT_SECTIONS,  TRACE_DISTANCE and RETURN_VALUE can only occur once per Method Data
        HashMap<String, ArrayList<Integer>> divergingSections = new HashMap<>();
        HashMap<String, ArrayList<Integer>> distanceSections = new HashMap<>();
        HashMap<String, ArrayList<Integer>> returnValDiff = new HashMap<>();

        //there can be more than one divergence instance per Method Data for the other types!
        HashMap<String, HashMap<MethodData,Integer>> paramDiff = new HashMap<>();
        HashMap<String, HashMap<MethodData,Integer>> invocationChanges = new HashMap<>();
        HashMap<String, HashMap<MethodData,Integer>> coverageChanges = new HashMap<>();

        HashSet<String> keys = new HashSet<>();

        for (TraceDivergence d : divs){
            String outerKey;
            if (d instanceof MetricDivergence){
                MetricDivergence m = (MetricDivergence) d;
                outerKey = m.getMethodData().getIdentifier();
                int metricValue = m.metricValue;

                if (m.type == DivergenceType.TRACE_DIVERGENT_SECTIONS )
                    //per method data instance, only one of these exists!
                    storeMetricValueForUniqueMethodData(divergingSections, metricValue, outerKey);
                else{
                    assert( m.type == DivergenceType.TRACE_DISTANCE );
                    //per method data instance, only one of these exists!
                    storeMetricValueForUniqueMethodData(distanceSections, metricValue, outerKey);
                }

            } else if (d instanceof CoverageDivergence){
                CoverageDivergence c = (CoverageDivergence) d;
                MethodData innerKey = c.getMethodData();
                outerKey = innerKey.getIdentifier();
                //per method data instance, multiple such divergences can exist (wc: one for each instruction)
                storeMetricValueForNonUniqueMethodData(coverageChanges, c.getCoverageDifference(), innerKey, outerKey);

            } else if (d instanceof MethodCallDivergence){
                MethodCallDivergence m = (MethodCallDivergence) d;
                MethodData innerKey = m.getMethodData();
                outerKey = innerKey.getIdentifier();
                //per method data instance, multiple such divergences can exist (wc: one for each different call)
                storeMetricValueForNonUniqueMethodData(invocationChanges, 1, innerKey, outerKey);

            } else if (d instanceof ObjectValueDivergence){
                ObjectValueDivergence o = (ObjectValueDivergence) d;
                MethodData innerKey = o.getMethodData();
                outerKey = innerKey.getIdentifier();
                int metricValue = o.getNumberOfDifferencesInRecordedProperties();

                if (o.type == DivergenceType.RETURN_VALUE )
                    //per method data instance, only one of these exists!
                    storeMetricValueForUniqueMethodData(returnValDiff, metricValue, outerKey);
                else{
                    assert( o.type == DivergenceType.PARAMETER );
                    //per method data instance, multiple such divergences can exist (wc: ore for each parameter)
                    storeMetricValueForNonUniqueMethodData(paramDiff, metricValue, innerKey, outerKey);
                }

            } else throw new InternalError(
                    "Programmer has extended 'TraceDivergence' but forgotten to expand this piece of code as well!");

            keys.add(outerKey); //store all used keys for easier access later!
        }

        HashMap<String, int[][]> result = new HashMap<>();
        for (String key : keys){
            int[][] array = new int[6][];

            array[0] = divergingSections.containsKey(key) ? convertToIntArray(divergingSections.get(key)) : new int[0];
            array[1] = distanceSections.containsKey(key) ? convertToIntArray(distanceSections.get(key)) : new int[0];
            array[2] = returnValDiff.containsKey(key) ? convertToIntArray(returnValDiff.get(key)) : new int[0];
            array[3] = paramDiff.containsKey(key) ? convertToIntArray(paramDiff.get(key)) : new int[0];
            array[4] = coverageChanges.containsKey(key) ? convertToIntArray(coverageChanges.get(key)) : new int[0];
            array[5] = invocationChanges.containsKey(key) ? convertToIntArray(invocationChanges.get(key)) : new int[0];

            result.put(key, array);
        }


        return result;
    }

    private int[] convertToIntArray(HashMap<MethodData, Integer> h) {
        int[] result = new int[h.keySet().size()];
        int counter = 0;
        for (MethodData m : h.keySet()){
            result[counter++] = h.get(m);
        }
        return result;
    }

    private int[] convertToIntArray(ArrayList<Integer> a){
        int[] result = new int[a.size()];
        int counter = 0;
        for (Integer i : a){
            result[counter++] = i;
        }
        return result;
    }

    private void storeMetricValueForNonUniqueMethodData(HashMap<String, HashMap<MethodData, Integer>> metricContainer,
                                                        int value, MethodData innerKey, String outerKey) {
        if (metricContainer.containsKey(outerKey)){
            HashMap<MethodData, Integer> innerMap = metricContainer.get(outerKey);
            if (innerMap.containsKey(innerKey))
                innerMap.put(innerKey, innerMap.get(innerKey) + value);
            else innerMap.put(innerKey, value);
        } else{
            HashMap<MethodData, Integer> innerMap = new HashMap<>();
            innerMap.put(innerKey, value);
            metricContainer.put(outerKey, innerMap);
        }
    }

    private void storeMetricValueForUniqueMethodData(HashMap<String, ArrayList<Integer>> metricContainer,
                                                     int value, String key) {
        if (metricContainer.containsKey(key)){
            metricContainer.get(key).add(value);
        } else{
            ArrayList<Integer> innerList = new ArrayList<>();
            innerList.add(value);
            metricContainer.put(key, innerList);
        }
    }


    public JSONObject toJSON(){
        JSONObject result = new JSONObject();

        result.put("-> testingTarget", identifier);
        if (!syntaxChanges.isEmpty())
            result.put("numberOfSyntacticChanges", syntaxChanges.size());
        result.put("numberOfAffectedMethods", methodsWithDifferentBehavior.size());
        result.put("impactMetricValue", getImpactMetricValue());

        if (includeNotes) {
            if (syntaxChanges.isEmpty())
                result.put("*NOTE*", "No syntactical modified section higher in the call chain! Cause of divergence is most likely a different object state caused by a previous trace divergence.");
            else if (!isOnClassLevel && !hasDirectChanges)
                result.put("*NOTE*", "No local impact has been detected despite the method being syntactically modified (could have modified object state)!");
        }

        if (includeDetails){
            JSONArray metricDetails = new JSONArray();
            if (local_averageTraceDistance != 0f || nonLocal_averageTraceDistance != 0f){
                JSONObject o = new JSONObject();
                o.put("distanceMetricValue (at Syntax Change)", local_averageTraceDistance);
                o.put("distanceMetricValue (rest of program)", nonLocal_averageTraceDistance);
                metricDetails.put(o);
            }
            if (local_averageDivergentSections != 0f || nonLocal_averageDivergentSections != 0f){
                JSONObject o = new JSONObject();
                o.put("divergentSectionsMetricValue (at Syntax Change)", local_averageDivergentSections);
                o.put("divergentSectionsMetricValue (rest of program)", nonLocal_averageDivergentSections);
                metricDetails.put(o);
            }
            if (local_averageParameterDiff != 0f || nonLocal_averageParameterDiff != 0f){
                JSONObject o = new JSONObject();
                o.put("parameterDifferenceMetricValue (at Syntax Change)", local_averageParameterDiff);
                o.put("parameterDifferenceMetricValue (rest of program)", nonLocal_averageParameterDiff);
                metricDetails.put(o);
            }
            if (local_averageReturnValDiff != 0f || nonLocal_averageReturnValDiff != 0f){
                JSONObject o = new JSONObject();
                o.put("returnedObjectsDifferenceMetricValue (at Syntax Change)", local_averageReturnValDiff);
                o.put("returnedObjectsDifferenceMetricValue (rest of program)", nonLocal_averageReturnValDiff);
                metricDetails.put(o);
            }
            if (local_averageCoverageChanges != 0f || nonLocal_averageCoverageChanges != 0f){
                JSONObject o = new JSONObject();
                o.put("coverageDifferenceMetric (at Syntax Change)", local_averageCoverageChanges);
                o.put("coverageDifferenceMetric (rest of program)", nonLocal_averageCoverageChanges);
                metricDetails.put(o);
            }
            if (local_averageMethodCallChanges != 0f || nonLocal_averageTraceDistance != 0f){
                JSONObject o = new JSONObject();
                o.put("methodInvocationDifferenceMetricValue (at Syntax Change)", local_averageMethodCallChanges);
                o.put("methodInvocationDifferenceMetricValue (rest of program)", nonLocal_averageMethodCallChanges);
                metricDetails.put(o);
            }
            if (!methodsWithDifferentBehavior.isEmpty()) {
                JSONObject o = new JSONObject();
                o.put("affectedMethodsMetricValue", nonLocal_affectedMethods);
                metricDetails.put(o);
            }
            result.put("individualMetricValues", metricDetails);


            if (!syntaxChanges.isEmpty()) {
                JSONArray listOfSyntaxChanges = new JSONArray();
                for (SyntaxChange change : syntaxChanges) {
                    listOfSyntaxChanges.put(change.toJSONObjectWithoutIdentifier());
                }
                result.put("syntacticChanges", listOfSyntaxChanges);
            }


            if (!nonLocalDivergences.isEmpty()) {
                JSONArray listOfDivergences = new JSONArray();
                for (String method : nonLocalDivergences.keySet()) {
                    JSONObject divObject = new JSONObject();
                    JSONArray divArray = new JSONArray();
                    for (TraceDivergence divergence : nonLocalDivergences.get(method)) {
                        divArray.put(divergence.toJSON());
                    }
                    divObject.put("method", method);
                    divObject.put("divergences", divArray);

                    listOfDivergences.put(divObject);
                }
                result.put("methodsWithDetectedDivergences", listOfDivergences);
            }
        }


        return result;
        
    }

    /** Overall ranking value! */
    public double getImpactMetricValue(){
        return localValue() +  nonLocalValue();
    }

    private double nonLocalValue() {
        return nonLocal_averageReturnValDiff + nonLocal_averageParameterDiff + //objects
                nonLocal_averageDivergentSections+ nonLocal_averageTraceDistance + //distance
                nonLocal_averageCoverageChanges + //coverage (if any)
                nonLocal_averageMethodCallChanges + //method invocations
                nonLocal_affectedMethods;
    }

    protected double localValue() {
        return local_averageReturnValDiff + local_averageParameterDiff + //objects
                local_averageDivergentSections+ local_averageTraceDistance + //distance
                local_averageCoverageChanges + //coverage (if any)
                local_averageMethodCallChanges + //method invocations
                local_amountOfSyntaxChanges; //number of syntax changes
    }

    @Override
    public int compareTo(TestingTarget other) {
        if (other instanceof SyntaxChangeTestingTarget || other instanceof TraceDivergenceTestingTarget)
            return -1; //impact based targets are the real deal! Should actually never be compared to those!
        assert(other instanceof ImpactBasedTestingTarget);
        ImpactBasedTestingTarget t = (ImpactBasedTestingTarget) other;

        if (t.hasDirectChanges != hasDirectChanges)
            return hasDirectChanges ? -1 : 1; //if a change was actually executed, that is always more interesting!

        double result = t.getImpactMetricValue() - getImpactMetricValue(); //if I have more, then the number is negative!
        if (Math.abs(result)<=0.01f)
            return 0;
        else return result  > 0.f ? 1 : -1 ;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}
