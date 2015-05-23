package hartz.masterThesis.historyGuidedImpactAnalysis.core.targets;

import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

/** A testing target, this tool's final output object and ultimately its purpose. */
public abstract class TestingTarget implements Comparable<TestingTarget> {

    protected static boolean includeDetails = true;
    protected static boolean includeNotes = false;
    public static void disableDetailedOutput(){ includeDetails = false; }
    public static void enableNotes(){ includeNotes = false; }

    public abstract String getIdentifier();

    public abstract JSONObject toJSON();
}
