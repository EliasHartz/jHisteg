package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;


public class MetricDivergence extends TraceDivergence{

    public final int metricValue;

    public MetricDivergence(MethodData methodWithDifferentBehavior, DivergenceType type, int metricValue){
        super(methodWithDifferentBehavior, type);
        assert(type == DivergenceType.TRACE_DIVERGENT_SECTIONS || type == DivergenceType.TRACE_DISTANCE);
        this.metricValue = metricValue;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        superToJSON(o);
        o.put("metricValue", metricValue);
        return o;
    }
}
