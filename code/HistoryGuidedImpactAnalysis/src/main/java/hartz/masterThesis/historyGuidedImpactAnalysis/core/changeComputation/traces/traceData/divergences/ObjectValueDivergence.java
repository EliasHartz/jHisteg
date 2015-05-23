package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

public class ObjectValueDivergence extends TraceDivergence {
    public final String newClassOfReturnedValue;
    public final String oldClassOfReturnedValue;
    public final String newToStringOfReturnedValue;
    public final String oldToStringOfReturnedValue;
    public final int newHashCodeOfReturnedValue;
    public final int oldHashCodeOfReturnedValue;
    public final MethodData methodCalledOrReturning;


    public ObjectValueDivergence(MethodData methodWithDifferentBehavior, MethodData methodCalledOrReturning, DivergenceType type,
                                 String newClassOfReturnedValue, String newToStringOfReturnedValue, int newHashCodeOfReturnedValue,
                                 String oldClassOfReturnedValue, String oldToStringOfReturnedValue, int oldHashCodeOfReturnedValue) {
        super(methodWithDifferentBehavior, type);
        assert(type == DivergenceType.RETURN_VALUE || type == DivergenceType.PARAMETER);

        this.newClassOfReturnedValue = newClassOfReturnedValue;
        this.oldClassOfReturnedValue = oldClassOfReturnedValue;
        this.newToStringOfReturnedValue = newToStringOfReturnedValue;
        this.oldToStringOfReturnedValue = oldToStringOfReturnedValue;
        this.newHashCodeOfReturnedValue = newHashCodeOfReturnedValue;
        this.oldHashCodeOfReturnedValue = oldHashCodeOfReturnedValue;

        this.methodCalledOrReturning = methodCalledOrReturning;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        superToJSON(o);

        if (methodWithDifferentBehavior != methodCalledOrReturning){
            assert(type == DivergenceType.PARAMETER);
            o.put("methodInvocationOf", methodCalledOrReturning.getIdentifier());
        }

        if (!oldClassOfReturnedValue.equals(newClassOfReturnedValue)) {
            o.put("classOfObjectBefore", oldClassOfReturnedValue);
            o.put("classOfObjectNow", newClassOfReturnedValue);
        }
        if (!oldToStringOfReturnedValue.equals(newToStringOfReturnedValue)) {
            o.put("toStringOfObjectBefore", oldToStringOfReturnedValue);
            o.put("toStringOfObjectNow", newToStringOfReturnedValue);
        }
        if (oldHashCodeOfReturnedValue != newHashCodeOfReturnedValue) {
            o.put("hashCodeOfBefore", oldHashCodeOfReturnedValue);
            o.put("hashCodeOfNow", newHashCodeOfReturnedValue);
        }
        return o;
    }

    public int getNumberOfDifferencesInRecordedProperties(){
        int i = 0;
        if (!oldClassOfReturnedValue.equals(newClassOfReturnedValue)) {
            ++i;
        }
        if (!oldToStringOfReturnedValue.equals(newToStringOfReturnedValue)) {
            ++i;
        }
        if (oldHashCodeOfReturnedValue != newHashCodeOfReturnedValue) {
            ++i;
        }
        return i;
    }
}
