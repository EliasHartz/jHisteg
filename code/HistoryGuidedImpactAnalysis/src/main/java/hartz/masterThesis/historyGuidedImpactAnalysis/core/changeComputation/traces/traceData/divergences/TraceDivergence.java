package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences;


import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class TraceDivergence {

    protected final MethodData methodWithDifferentBehavior;
    public final DivergenceType type;

    public TraceDivergence(MethodData methodWithDifferentBehavior, DivergenceType type){
        this.methodWithDifferentBehavior = methodWithDifferentBehavior;
        this.type = type;

    }

    /**
     * Converts a given trace divergence list given by iterator into the *FLAT* output
     * format based on a single JSONObject.
     *
     * @param iterOfDivergences : iterator on the trace's divergences
     * @return a single object containing all divergences of the given trace
     */
    public static JSONObject convertTraceDivToJSON(Iterator<TraceDivergence> iterOfDivergences){
        assert(iterOfDivergences.hasNext());

        JSONObject result = new JSONObject();
        boolean first = true;
        JSONArray traceDivergences = new JSONArray();

        while(iterOfDivergences.hasNext()){
            TraceDivergence t = iterOfDivergences.next();
            if (first) {
                result.put("entryPointOfTrace", t.getEntryPointMethodData().getIdentifier());
                first = false;
            }
            JSONObject o = t.toJSON();
            o.put("-> methodsThatDiverged",
                    t.getMethodData() == null ? "!-undetermined-!" : t.getMethodData().getIdentifier());
            addSyntaxChanges(o,t.methodWithDifferentBehavior);
            addCallChain(o,t.methodWithDifferentBehavior);
            traceDivergences.put(o);
        }
        result.put("traceDivergences",traceDivergences);
        return result;
    }

    /**
     * Converts a given trace divergence list given by iterator into the *COMPLEX* output
     * format based with a tree-like structure mimicking the original trace.
     *
     * @param iterOfDivergences : iterator on the trace's divergences
     * @param entryPoint : MethodData of the very first recorded call of this trace
     * @return a single object containing all divergences of the given trace
     */
    public static JSONObject convertTraceDivToJSON(Iterator<TraceDivergence> iterOfDivergences, MethodData entryPoint){
        JSONObject result = new JSONObject();
        result.put("entryPointOfTrace", entryPoint.getIdentifier());
        addSyntaxChanges(result, entryPoint); //add syntax modifications to entry point of trace
        // Note: It is actually a very bad sign if the entry point has syntax changes. User should generate better traces!

        JSONArray divergences = convertTraceDivToJSON(iterOfDivergences, new HashMap<MethodData, JSONObject>());
        result.put("methodsThatDiverged", divergences);

        return result;
    }

    private static JSONArray convertTraceDivToJSON(Iterator<TraceDivergence> iterOfDivergences,
                                                   HashMap<MethodData, JSONObject> container) {
        JSONArray result = new JSONArray();

        while (iterOfDivergences.hasNext()){
            TraceDivergence t = iterOfDivergences.next();
            MethodData m = t.methodWithDifferentBehavior;
            JSONObject tAsJSON = t.toJSON();


            if (!container.containsKey(m)){
                //never stored something for this before!
                JSONObject methodJSON = new JSONObject();
                methodJSON.put("-> methodWithDivergences",m.getIdentifier());
                addSyntaxChanges(methodJSON, m);
                addCallChain(methodJSON, m);

                //will store all divergence objects for this method!
                JSONArray a = new JSONArray();
                a.put(tAsJSON);
                methodJSON.put("traceDivergences", a);

                //find out where this fits in:
                MethodData methodToStoreThisIn = m;
                while (methodToStoreThisIn!=null){
                    if (container.containsKey(methodToStoreThisIn)) break;
                    methodToStoreThisIn = methodToStoreThisIn.getCaller();
                }

                //store the divergence!
                if (methodToStoreThisIn == null) {
                    result.put(methodJSON); //store under entry point
                }
                else{
                    //store under previous divergence
                    container.get(methodToStoreThisIn).getJSONArray(
                            "traceDivergences").put(methodJSON);
                }

                //wherever it ended up, make sure that we can now store under this!
                container.put(m,methodJSON);

            } else{
                container.get(m).getJSONArray("traceDivergences").put(tAsJSON);
            }
        }

        return result;
    }

    private static void addCallChain(JSONObject o, MethodData m) {
        JSONArray a = new JSONArray();
        MethodData tmp = m;
        boolean first = true;
        while(tmp != null){
            a.put(first?"-> "+tmp.getIdentifier():tmp.getIdentifier());
            tmp = tmp.getCaller();
            first = false;
        }
        o.put("callChain", a);
    }

    private static void addSyntaxChanges(JSONObject o, MethodData m) {
        if (m.wasSyntacticallyModifiedInThisVersion()){
            JSONArray a = new JSONArray();
            for (SyntaxChange s : m.getSyntacticChanges()){
                a.put(s.toJSONObjectWithoutIdentifier());
            }
            o.put("syntacticChanges", a);
        }
    }

    protected void superToJSON(JSONObject o){
        o.put("divergenceType", type.toString());
    }

    public abstract JSONObject toJSON();

    public MethodData getMethodData(){
        return methodWithDifferentBehavior;
    }

    public MethodData getEntryPointMethodData() {
        MethodData m = methodWithDifferentBehavior;
        do{
            if (m.getCaller() == null)
                return m;
            else
                m = m.getCaller();
        } while (m != null);
        throw new InternalError("Corrupted trace divergence for method '"+methodWithDifferentBehavior.getIdentifier()+"'!");
    }

    /**
     * Returns either the first of all identifiers of syntactically modified methods
     * higher up in the call chain. Note that this might include divergence's invocation
     * as well, if the method was syntactically modified.
     * The exact behavior depends on the provided boolean flag.
     *
     * @param onlyFirst : set to 'true' to return only the first instead of all and to
     *                    'false' to include all identifiers in the call chain.
     * @return first of all identifiers of syntactically modified callers
     */
    public LinkedList<String> getCallChainSyntaxChangeAccessors(boolean onlyFirst) {
        MethodData m = methodWithDifferentBehavior;
        HashSet<String> check = new HashSet<>(); //avoid duplicates!
        LinkedList<String> result = new LinkedList<>();

        while (m != null){
            if(m.wasSyntacticallyModifiedInThisVersion()){
                if (m.doesResideInNewClass()) {
                    String s = m.getClassName();
                    if (!check.contains(s)) {
                        result.add(s);
                        check.add(s);
                        if (onlyFirst) return result;
                    }
                } else {
                    String s = m.getIdentifier();
                    if (!check.contains(s)) {
                        result.add(s);
                        check.add(s);
                        if (onlyFirst) return result;
                    }
                }
            }

            m = m.getCaller(); //go higher up
        }
        return result;
    }
}
