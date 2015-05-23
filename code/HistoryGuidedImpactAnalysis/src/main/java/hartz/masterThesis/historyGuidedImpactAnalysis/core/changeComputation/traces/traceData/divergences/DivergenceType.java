package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences;


public enum DivergenceType {
    //CONCRETE
    RETURN_VALUE,
    PARAMETER,
    ADDITIONAL_METHOD_CALLED,
    DIFFERENT_METHOD_CALLED,
    NOT_CALLED_METHOD,

    //METRIC
    TRACE_DISTANCE,
    TRACE_DIVERGENT_SECTIONS,
    COVERAGE
}
