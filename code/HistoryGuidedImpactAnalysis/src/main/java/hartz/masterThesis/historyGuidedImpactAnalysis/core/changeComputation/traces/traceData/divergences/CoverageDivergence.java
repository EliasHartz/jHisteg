package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;


public class CoverageDivergence extends TraceDivergence{

    public final int instrIndex;
    public final int numberOfExecutionsInOld;
    public final int numberOfExecutionsInNew;

    public CoverageDivergence(MethodData methodWithDifferentBehavior, int instrIndex, int timesInNew, int timesInOld){
        super(methodWithDifferentBehavior, DivergenceType.COVERAGE);
        this.instrIndex = instrIndex;
        this.numberOfExecutionsInOld = timesInNew;
        this.numberOfExecutionsInNew = timesInOld;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        superToJSON(o);
        o.put("instructionIndex", instrIndex);
        o.put("# executedBefore", numberOfExecutionsInOld);
        o.put("# executedNow", numberOfExecutionsInNew);
        return o;
    }

    public int getCoverageDifference(){
        return Math.abs(numberOfExecutionsInNew-numberOfExecutionsInOld);
    }
}
