package hartz.masterThesis.historyGuidedImpactAnalysis.configuration;

/** Holds our scalar factors used to weight different metrics for impact testing targets */
public class MetricScaling {

    private float localDivSectionScaler = 0.1f;
    private float localDistanceScaler = 0f; //ignore distance locally
    private float localDistanceOnlyScaler = 0.1f; //unless we are in distance-only mode
    private float localReturnValueScaler = 0.1f;
    private float localParameterScaler = 0.1f;
    private float localCoverageScaler = 0.1f;
    private float localMethodCallScaler = 0.1f;

    private float nonLocalDivSectionScaler = 10f;
    private float nonLocalDistanceScaler = 0.3f;
    private float nonLocalDistanceOnlyScaler = 1f; //use full distance when there is no data about sections!
    private float nonLocalReturnValueScaler = 1f;
    private float nonLocalParameterScaler = 1f;
    private float nonLocalCoverageScaler = 1f;
    private float nonLocalMethodCallScaler = 1f;

    private float callDistanceScaler = 3.5f; //distance is worth more than one return value or parameter!
    private float numberOfAffectedMethodsScaler = 0.01f; //we really do care much about this number
    private float syntaxChangeScaler = 1f;
    private float newClassMetricValue = 12.f;


    public float getDivergendSectionsScaler(boolean nonLocal) {
        return nonLocal ? nonLocalDivSectionScaler : localDivSectionScaler;
    }

    public float getTraceDistanceScaler(boolean nonLocal) {
        return nonLocal ? nonLocalDistanceScaler : localDistanceScaler;
    }

    public float getDistanceOnlyScaler(boolean nonLocal) {
        return nonLocal ? nonLocalDistanceOnlyScaler : localDistanceOnlyScaler;
    }

    public float getReturnValueScaler(boolean nonLocal) {
        return nonLocal ? nonLocalReturnValueScaler : localReturnValueScaler;
    }

    public float getParameterScaler(boolean nonLocal) {
        return nonLocal ? nonLocalParameterScaler : localParameterScaler;
    }

    public float getCoverageScaler(boolean nonLocal) {
        return nonLocal ? nonLocalCoverageScaler : localCoverageScaler;
    }

    public float getMethodCallScaler(boolean nonLocal) {
        return nonLocal ? nonLocalMethodCallScaler : localMethodCallScaler;
    }

    public float getCallDistanceScaler() {
        return callDistanceScaler;
    }

    public float getSyntaxChangesScaler(boolean newClassTarget) {
        return newClassTarget ? newClassMetricValue : syntaxChangeScaler;
    }

    public float getNumberOfAffectedMethodsScaler() {
        return numberOfAffectedMethodsScaler;
    }


    public void setDivergentSectionScaler(float nonLocal, float local) {
        this.nonLocalDivSectionScaler = nonLocal;
        this.localDivSectionScaler = local;
    }

    public void setTraceDistanceScaler(float nonLocal, float local) {
        this.nonLocalDistanceScaler = nonLocal;
        this.localDistanceScaler = local;
    }

    public void setReturnValueDifferenceScaler(float nonLocal, float local) {
        this.nonLocalReturnValueScaler = nonLocal;
        this.localReturnValueScaler = local;
    }

    public void setParameterDifferenceScaler(float nonLocal, float local) {
        this.nonLocalParameterScaler = nonLocal;
        this.localParameterScaler = local;
    }

    public void setCoverageDifferenceScaler(float nonLocal, float local) {
        this.nonLocalCoverageScaler = nonLocal;
        this.localCoverageScaler = local;
    }

    public void setMethodCallDifferenceScaler(float nonLocal, float local) {
        this.nonLocalMethodCallScaler = nonLocal;
        this.localMethodCallScaler = local;
    }

    public void setCallDistanceScaler(float scaler) {
        callDistanceScaler = scaler;
    }
}


