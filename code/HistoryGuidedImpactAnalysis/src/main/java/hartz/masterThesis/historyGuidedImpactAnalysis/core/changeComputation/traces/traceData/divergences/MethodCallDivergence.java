package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

public class MethodCallDivergence extends TraceDivergence {
    public final String methodCalledInOld; //callee
    public final String methodCalledInNew; //callee

    public MethodCallDivergence(MethodData methodWithDifferentBehavior, DivergenceType type, String dataInOld, String dataInNew){
        super(methodWithDifferentBehavior, type);
        assert(type == DivergenceType.DIFFERENT_METHOD_CALLED || type == DivergenceType.ADDITIONAL_METHOD_CALLED
                || type == DivergenceType.NOT_CALLED_METHOD);
        assert (dataInNew != null || dataInOld != null);
        this.methodCalledInOld = dataInOld;
        this.methodCalledInNew = dataInNew;
    }

    public String getInterestingMethodIdentifier(){
        return type == DivergenceType.NOT_CALLED_METHOD ? methodCalledInOld : methodCalledInNew;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        superToJSON(o);
        if (methodCalledInOld != null) o.put("calledBefore", methodCalledInOld);
        if (methodCalledInNew != null) o.put((methodCalledInOld != null)?"calledNow":"called", methodCalledInNew);
        return o;
    }
}
