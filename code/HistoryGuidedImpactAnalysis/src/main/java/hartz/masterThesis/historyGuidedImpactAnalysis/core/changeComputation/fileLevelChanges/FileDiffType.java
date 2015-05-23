package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges;

/**
 * Defines the types of changes that can occur on file-level. We use a simple system to
 * abstract from the different capabilities of different version control systems.
 */
public enum FileDiffType {
	ADDED,
	DELETED,
	MODIFIED,
	RENAMED,
	UNKNOWN //for anything we cannot properly match of classify
}
