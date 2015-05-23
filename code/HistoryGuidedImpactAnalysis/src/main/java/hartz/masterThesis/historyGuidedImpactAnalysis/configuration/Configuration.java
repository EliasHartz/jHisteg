package hartz.masterThesis.historyGuidedImpactAnalysis.configuration;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.VersionList;

/**
 * Container-object defining relevant environment variables for the tool's execution.
 * Contains methods to obtain the raw version-related data as entered by the user which should
 * be used to create {@link Version} objects!
 * 
 * NOTE: Even more user-entered data, do NOT trust it! Verify everything that you get from
 *       methods residing inside this class!
 */
public abstract class Configuration extends MetricScaling implements FileIOConfig {

	/**
	 * @return all versions supposed to be analyzed, may be empty!
	 */
	public abstract String[] getVersionIdentifiers();

	/**
	 * Returns the relative location of the sources INSIDE an extracted
	 * repository. If this data has not been provided by the user, this
	 * method will return the repository's main directory.
	 *
	 * @param identIndex: index of the corresponding version identifier
	 * @return relative-path of classpath for sources with respect to
	 *         repository
	 */
	public abstract String getRelativePathToSources(int identIndex);

	/**
	 * Returns the relative location of the compiled sources INSIDE an
	 * extracted repository. If this data has not been provided by the
	 * user, this method will return the repository's main directory.
	 *
	 * @return relative-path of compiled sources with respect
	 *         to the repository
	 */
	public abstract String getRelativePathToCompiledSources(int identIndex);

	/**
	 * Returns the relative location of the addition compiled sources
	 * INSIDE an extracted repository. If this data has not been
	 * provided by the user, this method will return the repository's
	 * main directory.
	 *
	 * @return relative-path of additional compiled sources with
	 *         respect to the repository
	 */
	public abstract String getRelativePathToAdditionalCompiledSources(int identIndex);


	/**
     * Returns the command to be used to compile the sources for a version.
     *
     * @param identIndex: index of the corresponding version identifier
	 * @return the command to be executed in order to produce compiled
	 *         sources!
	 */
	public abstract String getBuildCommand(int identIndex);

	/**
	 * Returns the relative location of the file to be used to build
	 * the project INSIDE an extracted repository.
	 *
     * @param identIndex: index of the corresponding version identifier
	 * @return relative-path (with respect to repository) to a
	 *         build file
	 */
	public abstract String getRelativePathToBuildFile(int identIndex);

    /**
     * Returns the contents of the history-file for the current data.
     *
     * @return a version list, containing ALL versions detected.
     */
	public abstract VersionList getAllVersionsOfRepo();

    /**
     * Returns an array of class name suffixes that are supposed to
     * exclude classes from instrumentation. At this point though,
     * it is only a list of strings...
     *
     * @param identIndex: index of the corresponding version identifier
     * @return suffixes to be excluded
     */
	public abstract String[] getIgnoreBySuffix(int identIndex);

    /**
     * Returns an array of class name prefixes that are supposed to
     * exclude classes from instrumentation. At this point though,
     * it is only a list of strings...
     *
     * @param identIndex: index of the corresponding version identifier
     * @return suffixes to be excluded
     */
	public abstract String[] getIgnoreByPrefix(int identIndex);

	/**
	 * Returns method signatures (not sanitized as usual) of methods that are
	 * to be ignored during the instrumentation phase, an example value would
	 * be 'hashCode()I'.
	 *
     * @param identIndex: index of the corresponding version identifier
	 * @return relative-paths in respect the repository that are to be ignored
	 */
	public abstract String[] getExcludeMethodsFromInstrumentationBySignature(int identIndex);

	/**
	 * Returns the relative locations of directories that are to be skipped
	 * during the instrumentation phase.
	 *
     * @param identIndex: index of the corresponding version identifier
	 * @return relative-paths in respect the repository that are to be ignored
	 */
	public abstract String[] getDirectoriesExcludedFromInstrumentation(int identIndex);

    /**
     * Returns a list of command to be used to generate traces for each version,
     * e.g. "ant test".
     *
     * @param identIndex: index of the corresponding version identifier
     * @return commands to execute in the program directory
     */
	public abstract String[] getRunCommands(int identIndex);

    /**
     * Returns a list of class names. These classes should have main methods
     * to call in order to generate execution traces for each version.
     *
     * @param identIndex: index of the corresponding version identifier
     * @return names of classes to run
     */
    public abstract String[] getExecuteMainClassnames(int identIndex);

    /**
     * Returns a list of parameters as string to be used along the result of
     * {@link #getExecuteMainClassnames(int)}. As usual, not sanitized!
     *
     * @param identIndex: index of the corresponding version identifier
     * @return arguments for main methods in classes to run
     */
	public abstract String[] getExecuteMainArguments(int identIndex);

	/**
	 * Returns the relative location of the directory which includes
	 * the compiled JUnit tests to run during trace generation.
	 *
     * @param identIndex: index of the corresponding version identifier
	 * @return relative-path of compiled tests with respect to repository
	 */
	public abstract String getRelativePathToTests(int identIndex);

    /**
     * Allows finer control which tests to run, returns an array containing
     * fully qualified class names or relative paths in each version's
     * directory (potentially both)
     *
     * @param identIndex: index of the corresponding version identifier
     * @return which tests to run
     */
	public abstract String[] getTestsToRun(int identIndex);

    /**
     * Allows finer control which tests not to run, returns an array
     * containing fully qualified class names of tests to ignore.
     *
     * @param identIndex: index of the corresponding version identifier
     * @return which tests not to run
     */
    public abstract String[] getTestsToSkip(int identIndex);

    /**
     * @return 'true' if our code observation functionality should be
     *         injected into the additional classpath as well
     */
	public abstract boolean needToInjectCoverageObserverTwice();

    /**
     * @return 'true' if frames should not be computed during the
     *         instrumentation and the stacks should be increased by
     *         a fixed amount instead of optimally
     */
    public abstract boolean useMoreCompatibleInstrumentation();

    /**
     * @return 'true' if parameters and return values that have '@' in
     *         their string-representation should be ignored during
     *         the trace divergence analysis
     */
    public abstract boolean filterObjectCharacteristics();
}
