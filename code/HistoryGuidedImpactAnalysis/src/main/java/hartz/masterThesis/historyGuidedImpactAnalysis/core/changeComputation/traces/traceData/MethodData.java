package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.Core;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChangeType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.util.*;

/**
 * Special data container for storing a method's execution during actual runtime of the instrumented project.
 * Implements comparison with other method data objects, as such contains pretty much the bulk of the trace
 * comparison code.
 */
public class MethodData {

    private static boolean coverageBasedTraceComparison = false;
    private static boolean  traceDistanceBasedComparison = false;

    private final String methodIdentifier;
    private final String classIdentifier;
    private final MethodType methodType; //just for easier access, you can actually see the type in the identifier
    private final MethodData calledFrom; //may be null
    private final MethodData[] calledMethods; //may be empty

    private final int[] executedInstrIndices; //contains the i-th  instruction index executed (also contains negative numbers, which refer to method calls)
    private final String[] traceDescription; //contains the i-th instruction executed

    private final TreeMap<Integer, Integer> coverageData; //counts which instruction index was executed how often...  (precomputed)

    //return value
    private final String classOfReturnedValue;
    private final String toStringOfReturnedValue;
    private final int hashCodeOfReturnedValue;

    private final String[] classOfParameter; //may be empty
    private final String[] toStringOfParameter; //may be empty
    private final int[] hashCodeOfParameter; //may be empty
    private final MethodSource source; //may be 'null'

    private boolean hasSyntaxModification;
    private final Version versionThisDataBelongsTo;
    private final List<SyntaxChange> syntacticChanges;


    /**
     * @param belongsTo : the version this trace was derived from
     * @param o : the JSON object from the trace file describing this particular method invocation
     * @param caller : the method calling this new instance, may be 'null'
     * @param allSyntaxChanges : map of all syntax changes OF THIS VERSION
     */
    public MethodData(Version belongsTo, JSONObject o, MethodData caller, HashMap<String, List<SyntaxChange>> allSyntaxChanges) {
        versionThisDataBelongsTo = belongsTo;

        /** Identifier of this method */
        methodIdentifier = o.getString("called");
        classIdentifier = methodIdentifier.substring(0,methodIdentifier.lastIndexOf("."));
        assert(methodIdentifier != null && !methodIdentifier.isEmpty());
        methodType = MethodType.valueOf(o.getString("type"));

        /** Parameters of method */
        JSONArray parameters = o.getJSONArray("calledWithParameters");
        this.classOfParameter = new String[parameters.length()];
        this.toStringOfParameter = new String[parameters.length()];
        this.hashCodeOfParameter = new int[parameters.length()];
        for (int i = 0; i<parameters.length(); ++i){
            JSONObject obj = parameters.getJSONObject(i);
            this.classOfParameter[i] = obj.optString("class", "---");
            this.toStringOfParameter[i] = obj.has("stringRepresentation")?obj.getString("stringRepresentation"):obj.getString("value");
            this.hashCodeOfParameter[i] = obj.optInt("hashCode", -1);
        }

        /** From which other method this one was called */
        calledFrom = caller;

        /** What the method returned */
        JSONObject returnedValue = o.getJSONObject("returned");
        classOfReturnedValue = o.optString("class", "---");
        toStringOfReturnedValue = returnedValue.has("stringRepresentation")?returnedValue.getString("stringRepresentation"):returnedValue.getString("value");
        hashCodeOfReturnedValue = returnedValue.optInt("hashCode", -1);

        /** What the method actually executed! */
        JSONArray trace = o.getJSONArray("trace");
        ArrayList<MethodData> calledMethods = new ArrayList<>();

        coverageData = coverageBasedTraceComparison ? new TreeMap<Integer, Integer>() /*= feature enabled */ : null /*= feature disabled */;

        this.executedInstrIndices = new int[trace.length()];
        this.traceDescription = new String[trace.length()];
        for (int i = 0; i<trace.length(); ++i){
            Object traceElement = trace.get(i);
            if (traceElement instanceof String){
                //format for a trace line is '[86]> INVOKESTATIC'
                String s = (String) traceElement;
                this.executedInstrIndices[i] = Integer.parseInt(s.substring(1,s.indexOf("]")));
                this.traceDescription[i] = s.substring(s.indexOf(">")+1);

                if (coverageBasedTraceComparison /* = feature enabled */){
                    if (coverageData.containsKey(executedInstrIndices[i]))
                        coverageData.put(executedInstrIndices[i], //overwrite
                                coverageData.get(executedInstrIndices[i])+1 );
                    else coverageData.put(executedInstrIndices[i], 1);
                }

            } else {
                //called another method here
                assert(traceElement instanceof JSONObject);
                MethodData calledMethod = new MethodData(belongsTo, (JSONObject)traceElement, this, allSyntaxChanges);
                calledMethods.add(calledMethod);
                this.executedInstrIndices[i] = (-calledMethods.size()); //negative numbers encode call index of call + 1 (a bit hacky, I know)
                this.traceDescription[i] = calledMethod.getIdentifier();
            }
        }
        //no need to import the java line numbers!


        this.calledMethods = calledMethods.toArray(new MethodData[calledMethods.size()]);
        this.source = Core.lookupSources(belongsTo, methodIdentifier);



        if (allSyntaxChanges.containsKey(methodIdentifier)){
            //this method was modified
            hasSyntaxModification = true;
            syntacticChanges = allSyntaxChanges.get(methodIdentifier);
            assert(syntacticChanges.size()>=1);

        } else if (allSyntaxChanges.containsKey(classIdentifier)){
            syntacticChanges = allSyntaxChanges.get(classIdentifier);
            hasSyntaxModification = true;
            //so it either has class-level changes, e.g. a field was added or it resides in a new class!!!
        } else{
            syntacticChanges = null;
        }
    }

    /**
     * @return 'true' if the invoked method is inside a class newly added in this version
     */
    public boolean doesResideInNewClass(){
        if (hasSyntaxModification){
            return syntacticChanges.size() == 1 && syntacticChanges.get(0).getTypeOfSyntaxChange() == SyntaxChangeType.NEW_CLASS;
        }
        return false;
    }

    public String getIdentifier() {
        return methodIdentifier;
    }

    public String getClassName() {
        return methodIdentifier.substring(methodIdentifier.lastIndexOf("."));
    }

    public MethodData getCaller() {
        return calledFrom;
    }

    protected MethodData[] getArrayOfCalledMethods() {
        return calledMethods;
    }

    /**
     * @return how many other methods were called, note that this is the number of invocations and not
     *         the number if distinct methods!
     */
    public int getAmountOfMethodCalls(){ return calledMethods.length; }

    public MethodData getCalledMethod(int i){
        assert(i>=0);
        assert(i<calledMethods.length);

        return calledMethods[i];
    }

    public String getClassOfReturnedValue() {
        return classOfReturnedValue;
    }

    public String getToStringOfReturnedValue() {
        return toStringOfReturnedValue;
    }

    public int getHashCodeOfReturnedValue() {
        return hashCodeOfReturnedValue;
    }

    public int getAmountOfParameters(){ return classOfParameter.length; }

    public String getClassOfParameter(int i) {
        return classOfParameter[i];
    }

    public String getToStringOfParameter(int i) {
        return toStringOfParameter[i];
    }

    public int getHashCodeOfParameter(int i) {
        return hashCodeOfParameter[i];
    }

    protected String[] getArrayOfParameterClasses() {
        return classOfParameter;
    }

    protected String[] getArrayofParameterToStrings() {
        return toStringOfParameter;
    }

    /**
     * @return the method identifiers of all invoked methods, in order of invocation
     */
    public String[] getCalledMethodHeaders() {
        String[] strArr = new String[calledMethods.length];
        for (int i = 0; i<calledMethods.length; ++i){
            strArr[i] = calledMethods[i].getIdentifier();
        }
        return strArr;
    }

    protected int[] getArrayOfParameterHashCodes() {
        return hashCodeOfParameter;
    }

    /**
     * @return string representation of the instructions that were executed, in order of execution
     */
    public String[] getTrace() {
        return traceDescription;
    }

    /**
     * @return the indices of the instructions that were executed, in order of execution
     */
    public int[] getBytecodeIndices() {
        return executedInstrIndices;
    }

    public boolean wasSyntacticallyModifiedInThisVersion(){ return hasSyntaxModification; }

    /**
     * Can be used to check if another MethodData object represents an invocation
     * of the same method. Note that it is obviously not necessarily the same
     * invocation!
     *
     * @param m : method invocation to check against
     * @return 'true' if the provided instance holds data from the same method
     */
    public boolean isSameMethod(MethodData m){
        if (m == null) return false;
        return m.getIdentifier().equals(getIdentifier());
    }

    /** Expensive operation! This is not an equals-method, it is used by tests mainly!*/
    public boolean equalsMethodData(MethodData m){
        if (!getIdentifier().equals(m.getIdentifier()))
            return false; //class name is covered by this, e.g. 'mainPackage.Class.<init>()V'
        if (!((getCaller() == null && m.getCaller() == null) || getCaller().equalsMethodData(m.getCaller()))) return false;
        if (!getClassOfReturnedValue().equals(m.getClassOfReturnedValue())) return false;
        if (!getToStringOfReturnedValue().equals(m.getToStringOfReturnedValue())) return false;
        if (hashCodeOfReturnedValue != m.getHashCodeOfReturnedValue()) return false;
        if (!Arrays.equals(getArrayOfParameterClasses(), m.getArrayOfParameterClasses())) return false;
        if (!Arrays.equals(getArrayofParameterToStrings(), m.getArrayofParameterToStrings())) return false;
        if (!Arrays.equals(getArrayOfParameterHashCodes(), m.getArrayOfParameterHashCodes())) return false;
        if (!Arrays.equals(getBytecodeIndices(), m.getBytecodeIndices())) return false;

        MethodData[] mine = getArrayOfCalledMethods();
        MethodData[] other = m.getArrayOfCalledMethods();
        if (mine.length != other.length) return false;
        for (int i = 0; i< mine.length; ++i)
            if (!mine[i].getIdentifier().equals(other[i].getIdentifier())) return false;

        return true;
    }

    @Override
    public String toString(){
        String calledMethods = "";
        if (this.calledMethods.length>0) {
            for (MethodData s : this.calledMethods) {
                calledMethods += s.getIdentifier() + ", ";
            }
            calledMethods = calledMethods.substring(0, calledMethods.length() - 2);
        }

        String trace = "";
        if (traceDescription.length>0) {
            for (String s : traceDescription) {
                trace += s + ", ";
            }
            trace = trace.substring(0, trace.length() - 2);
        }

        return methodIdentifier+ (calledFrom == null ? "" : "\n[called from "+calledFrom+"]") +
                "\n returned "+getToStringOfReturnedValue()+
                "\n called {"+calledMethods+"}"+
                "\n trace was ["+trace+"]";
    }


    /**
     * Basically implements trace divergence analysis on a single method.
     * Potentially recurses into methods called by this one.
     *
     * @param olderData : trace to compare this against, might be an entirely different method!
     * @param traceDivergenceContainer : detected trace divergences, everything detected by this method
     *                                   will be added to this list!
     * @param filterObjectsOnAtChar : set to 'true' to impose a stricter comparison on parameters and
     *                                return values to filter out false positives
     */
    public void computeDifferences(MethodData olderData, List<TraceDivergence> traceDivergenceContainer, boolean filterObjectsOnAtChar) {
        //################     Check that we are comparing the same method!   #########################################
        {

            if (calledFrom != null && !calledFrom.isSameMethod(olderData.calledFrom)){
                /* This means we screwed up, since the caller is not identical! */
                Tool.printError("Internal Error during Trace Analysis! Method '"+methodIdentifier+"' and '"+olderData.getIdentifier()+
                        " should not be compared, their caller is different!");
                return;
            }

            /* caller if there is any is correct, now what's with the actual method ?*/
            if (!isSameMethod(olderData)) {

                if (olderData.calledFrom == null || calledFrom == null /* = one is an entry point */) {
                    //This means that we are comparing two completely different traces, which makes no sense! The entry point is different!
                    Tool.printError("Internal Error during Trace Analysis! Method '" + methodIdentifier + "' and '" + olderData.getIdentifier() +
                            " should not be compared since they are obviously not the same method!");
                    return;
                }

                /** If a method calls a different other without a syntax change compared to before, we usually see a difference in the trace prior to the
                 *  method invocation but there are exceptions! This may occur for example when the dynamic type of an object causes a different method
                 *  to be called (like a method overwritten in a sub-class). */

                assert (olderData.calledFrom != null); //these cases should have been dealt with before
                assert (calledFrom != null);

                traceDivergenceContainer.add(new MethodCallDivergence(calledFrom /* this is a difference in the CALLER!!! */,
                        DivergenceType.DIFFERENT_METHOD_CALLED, olderData.getIdentifier(), methodIdentifier));
                return; /* We return immediately since comparing things that are found to be different already makes
                          no sense and would only serve to blur the result.*/
            }
        }



        //################      Compare the return value!   ############################################################
        {
            String classOf = getClassOfReturnedValue();
            String stringOf = getToStringOfReturnedValue();
            int hashOf = getHashCodeOfReturnedValue();
            String oldClassOf = olderData.getClassOfReturnedValue();
            String oldStringOf = olderData.getToStringOfReturnedValue();
            int oldHashOf = olderData.getHashCodeOfReturnedValue();

            if (filterObjectsOnAtChar) {
                if (stringOf.contains("@")) {
                    stringOf = "";
                    hashOf = 0;
                }
                if (oldStringOf.contains("@")) {
                    oldStringOf = "";
                    oldHashOf = 0;
                }
            }

            String strRepOfReturnValue = classOf + " '[" + stringOf+ "]' hash: " + hashOf;
            String oldStrRepOfReturnValue = oldClassOf + " '[" + oldStringOf +"]' hash: " + oldHashOf;
            if (!strRepOfReturnValue.equals(oldStrRepOfReturnValue)) {
                traceDivergenceContainer.add(new ObjectValueDivergence(this, this, DivergenceType.RETURN_VALUE,
                        classOf, stringOf, hashOf, oldClassOf, oldStringOf, oldHashOf
                ));
            }
        }


        //################      Handle the tracked parameters   ########################################################
        {
            if (getAmountOfParameters() != olderData.getAmountOfParameters()) {
                assert (hasSyntaxModification); //otherwise this cannot be!
                //TODO: We could attempt to match the values by leveraging that we know the exact syntax changes here, for
                //the moment however, this is being ignored!
            } else {
                for (int i = 0; i < getAmountOfParameters(); ++i) {
                    String classOf = getClassOfParameter(i);
                    String stringOf = getToStringOfParameter(i);
                    int hashOf = getHashCodeOfParameter(i);
                    String oldClassOf = olderData.getClassOfParameter(i);
                    String oldStringOf = olderData.getToStringOfParameter(i);
                    int oldHashOf = olderData.getHashCodeOfParameter(i);

                    if (filterObjectsOnAtChar) {
                        if (stringOf.contains("@")) {
                            stringOf = "";
                            hashOf = 0;
                        }
                        if (oldStringOf.contains("@")) {
                            oldStringOf = "";
                            oldHashOf = 0;
                        }
                    }

                    String strRepOfParameter = classOf + " '[" + stringOf + "]' hash: " + hashOf;
                    String strRepOfOldParameter = oldClassOf  + " '[" + oldStringOf + "]' hash: " + oldHashOf;
                    if (!strRepOfParameter.equals(strRepOfOldParameter) &&
                            olderData.calledFrom != null && calledFrom != null) {
                        /* note that we ignore parameter differences for the entry point, we generate warnings for that in the Trace-class though*/
                        traceDivergenceContainer.add(new ObjectValueDivergence(calledFrom/* this is a difference in the CALLER!!! */, this,
                                DivergenceType.PARAMETER, classOf, stringOf, hashOf, oldClassOf, oldStringOf, oldHashOf
                        ));
                    }
                }
            }
        }


        //################      Deal with coverage or control flow changes!   ###########################################
        if (coverageBasedTraceComparison || traceDistanceBasedComparison) {
            if (coverageBasedTraceComparison) {
                /** Coverage-based approach like Javalanche, simply check how often which line was executed. */
                TreeMap<Integer, Integer> olderCoverageData = olderData.getCoverageData();
                int maxInstrIndex = Math.max(coverageData.lastKey(), olderCoverageData.lastKey());

                for (int i = 0; i < maxInstrIndex; ++i) {
                    Integer newInstrCount = coverageData.get(i);
                    Integer oldInstrCount = olderCoverageData.get(i);
                    if (newInstrCount == null && oldInstrCount == null) continue; //no trace visited this
                    else if (newInstrCount == null && oldInstrCount != null)
                        //one trace visited it!
                        traceDivergenceContainer.add(new CoverageDivergence(this, i , 0, oldInstrCount));
                    else if (newInstrCount != null && oldInstrCount == null)
                        //the other trace visited it!
                        traceDivergenceContainer.add(new CoverageDivergence(this, i , newInstrCount, 0));
                    else if (!newInstrCount.equals(oldInstrCount))
                        //both traces visited this instruction!
                        traceDivergenceContainer.add(new CoverageDivergence(this, i , newInstrCount, oldInstrCount));
                    //else it is equal and there is no difference in coverage!
                }
            }
            if (traceDistanceBasedComparison) {
                /** Levenshtein-based approach that gives the edit distance between the two traces  */
                final int[] olderExecutedTraceStatement = olderData.getBytecodeIndices();
                final String[] olderTraceDesc = olderData.getTrace();
                int index = 0;
                int differenceMetric = TraceUtil.computeTraceDistanceQuick( //how different was this actually? Do compare methods!
                        executedInstrIndices, traceDescription, olderExecutedTraceStatement, olderTraceDesc, index);
                traceDivergenceContainer.add(new MetricDivergence(this, DivergenceType.TRACE_DISTANCE, differenceMetric));
            }
        } else { /* ---------- FULL METRIC EXECUTION ------------------------------------------------*/
            /** Try to compute the minimal difference between the two traces (based on new data).
             *  Tries to obtain the points were it actually diverged, that means it outputs how
             *  *OFTEN* it was different first while coverage can only give you a rough estimate. */
            final int[] olderExecutedTraceStatement = olderData.getBytecodeIndices();
            int lengthOfNewTrace = executedInstrIndices.length;
            int lengthOfOldTrace = olderExecutedTraceStatement.length;

            int index = 0;
            boolean differenceDetected = false;

            /* Since most of the time the trace is probably equal, do a pre-processing step to save time! */
            if (lengthOfNewTrace == lengthOfOldTrace) {
                while (index < lengthOfNewTrace /* their lengths are equal, no worries */) {
                    int valNew = executedInstrIndices[index];
                    int valOld = olderExecutedTraceStatement[index];
                    if (!( (valOld == valNew) || (valOld < 0 && valNew < 0) /* we treat all method calls as equal here */)) {
                        differenceDetected = true;
                        break;
                    }
                    ++index;
                }
            } else differenceDetected = true;

            if (differenceDetected) {

                TraceUtil.TraceInstrDiff data = TraceUtil.computeTraceDistanceFull(
                        /* note that we use the "method-calls are equal"-mode here */
                        executedInstrIndices, olderExecutedTraceStatement, index
                );
                int differenceMetric = data.distanceMetricValue;
                assert (differenceMetric != 0); //otherwise it should not have been called!

                /** If no full source lookup is available, we can only look at the numbers or risk jumping to
                 *  false conclusions because we have never seen a branch or something. However if we have
                 *  the possibility to check the source code, we can put the diverging section of the trace
                 *  into context!  */

                traceDivergenceContainer.add(new MetricDivergence(this, DivergenceType.TRACE_DISTANCE, differenceMetric));
                traceDivergenceContainer.add(new MetricDivergence(this, DivergenceType.TRACE_DIVERGENT_SECTIONS,
                        data.differences.keySet().size() /* = how often it diverged */));

                if (source != null) {
                    //TODO: We could now employ our context information to obtain some more interesting information,
                    // e.g. which branch it was!
                }
            }


            //################      Recurse into other method calls!   ###########################################
            int[] matchedMethodCalls = TraceUtil.matchMethodCalls(getCalledMethodHeaders(),
                    olderData.getCalledMethodHeaders(), this, traceDivergenceContainer);

            for (int indexOfTraceInNew = 0; indexOfTraceInNew < matchedMethodCalls.length ; ++indexOfTraceInNew){
                int indexOfTraceInOld = matchedMethodCalls[indexOfTraceInNew];
                if (indexOfTraceInOld != Integer.MIN_VALUE){
                    MethodData calledMethodInThis = getCalledMethod(indexOfTraceInNew);
                    calledMethodInThis.computeDifferences(olderData.getCalledMethod(indexOfTraceInOld), traceDivergenceContainer, filterObjectsOnAtChar); //--> recurse!
                }
                else continue; //could not be matched!
            }
        }
    }

    public Version getVersionThisDataBelongsTo() {
        return versionThisDataBelongsTo;
    }

    public List<SyntaxChange> getSyntacticChanges() {
        assert(hasSyntaxModification);
        return syntacticChanges;
    }

    protected TreeMap<Integer,Integer> getCoverageData() {
        return coverageData;
    }

    /** Activates a different mode of storing and comparing trace data, must be called BEFORE any MethodData objects
     *  are instantiated, otherwise comparisons will likely be corrupt or NullPointerExceptions with be thrown! Can
     *  be combined with 'enableTraceDistnaceBasedComparison()' to include both techniques in the result.
     *  @param b : 'true' to activate, 'false' to deactivate (which is the default state)
     */
    public static void setCoverageBasedComparison(boolean b){
        coverageBasedTraceComparison = b;
    }

    /** Activates a different mode of storing and comparing trace data, must be called BEFORE any MethodData objects
     *  are instantiated, otherwise comparisons will likely be corrupt or NullPointerExceptions with be thrown! Can
     *  be combined with 'setCoverageBasedComparison()' to include both techniques in the result.
     *  @param b : 'true' to activate, 'false' to deactivate (which is the default state)
     */
    public static void setTraceDistanceBasedComparison(boolean b){traceDistanceBasedComparison = b;}
}