package hartz.masterThesis.historyGuidedImpactAnalysis.configuration;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Behavior;
import hartz.masterThesis.historyGuidedImpactAnalysis.repoMining.Miner;

import java.io.File;

/**
 * Container-object defining relevant environment variables for the tool'sexecution.
 *
 * NOTE: Since the data stored in here is user-given and most cannot
 *       be verified prior to actual execution, the comments here are
 *       describing the INTENDED return values. E.g. a method returning
 *       a particular file might in fact return some bullshit temporary
 *       directory with no relationship to the task at hand. It is up
 *       to the user after all!
 */
public interface FileIOConfig{

	/**
	 * @return desired behavior of the tool
	 */
	public Behavior getBehavior();

	/**
	 * @return the directory where all of the tool's output shall be placed
	 */
	public File getOutputDirectory();

	/**
	 * @return the directory the checked-out/cloned repository resides
	 *         OR 'null' if such a local copy does not exist or was not
	 *         specified
	 */
	public File getRepoDir();

    /**
     * @return miner on the current repository, 'null' if there is no repo!
     */
	public Miner getRepositoryMiner();

    /**
     * @return 'true' if renaming should not be an ignored status
     */
	public boolean areRenamedElementsToBeTreatedAsSyntaxChanges();

    /**
     * @return 'true' if present versions should be extracted anew
     */
	public boolean isPresentDataToBeOverwritten();

    /**
     * @return 'true' if a cloned repository should be deleted when
     *         this tool terminates
     */
	public boolean isRepoToBeDeletedAtTheEnd();

    /**
     * @return 'true' if all observed traces are to be removed before
     *         creating new ones. If no traces are to be created,
     *         nothing should be done no matter this value!
     */
	public boolean areTracesToBeDeleted();

    /**
     * @return 'true' if the coverage metric is to replace the divergent
     *         section based metric
     */
    public boolean compareTraceCoverage();

    /**
     * @return 'true' if the trace distance is to replace the divergent
     *         section based metric
     */
	public boolean compareTraceDistance();

    /**
     * @return 'true' if all instrumented code should be exported into
     *         each versions directory in human-readable form
     */
	public boolean exportSourceCodeToDirectory();

    /**
     * @return 'true' if the directories containing compiled code should
     *         be deleted before starting the compilation phase.
     */
	public boolean areCompiledSourcesToBeDeletedBeforeCompilation();

    /**
     * @return 'true' if the flat exportation format for trace divergences
     *         is to be used
     */
	public boolean areTracesToBeExportedAsList();

    /**
     * @return 'true' if testing targets based only on syntax changes only
     *         should be generated in addition to impact-based targets
     */
	public boolean generateTargetsForSyntax();

    /**
     * @return 'true' if testing targets based only on trace divergences only
     *         should be generated in addition to impact-based targets
     */
	public boolean generateTargetsForDivergences();

    /**
     * @return 'true' if a detected trace divergence in unmodified source code
     *         should only be mapped to the first syntactically modified method
     *         higher up in the call chain instead of all such methods.
     */
	public boolean mapOnlyToFirstSyntaxChange();

    /**
     * @return 'true' if the program should look for a trace matching file before
     *         attempting auto-matching based on the entry point
     */
    public boolean needToUseManualTraceMatchingFile();
}
