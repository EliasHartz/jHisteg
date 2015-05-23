package hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants;

/**
 * Simply holds the execution constants.
 */
public class Globals {
	public static final int fileReaderBufferSize = 300;
    public static final int zipReaderBufferSize = 8192;
	public static final int jsonIndentFactor = 3;

    /** Some tests might disable this flag to make tests machine-independent! */
	public static boolean performMachineDependentVersionChecks = true;

	/** Sometimes, observed code does not terminate but we want to terminate at some point... */
	public static int maxTimeToWaitWhenObservedCodeDoesNotTerminate = 15000; //millis
}
