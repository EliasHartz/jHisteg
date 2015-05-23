package hartz.masterThesis.historyGuidedImpactAnalysis.configuration;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Behavior;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.VersionList;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.repoMining.Miner;

import java.io.File;

/**
 * Actual container-object to configure how this tool is supposed to be executed!
 *
 * TODO: This class is in desperate need of refactorization. It grew from its humble and clean beginnings to
 * a copy&paste monster, however it is tested quite well at the moment.
 */
public class Configurable extends Configuration {
    private String[] versionIdentifiers;

    private File outputDir;
    private File repoDir;

    private boolean overwrite;
    private boolean coverRenamed;
    private boolean deleteRepo;

    private String[] relativePathToSources;
    private String[] relativePathToCompiledSources;
    private String[] relativePathToAdditionalCompiledSources;
    private String[] buildCommand;
    private String[][] ignoreBySuffix;
    private String[][] ignoreByPrefix;
    private String[][] excludedMethods;
    private String[][] excludedDirectories;
    private String[] relativePathToBuildFile;

    private String[] relativePathToTests;
    private String[][] testsToRun;
    private String[][] testsToSkip;
    private String[][] executeMainArguments;
    private String[][] executeMainClassnames;
    private String[][] runCommands;

    private final Behavior behavior;

    private Miner miner;
    private VersionList allVersions;
    private boolean deleteTraces;
    private boolean coverageBasedTraces;
    private boolean distanceBasedTraces;
    private boolean sourceExportAllowed;
    private boolean deleteCompiledSourceBeforeCompilation;
    private boolean exportTracesFlat;
    private boolean generateTargetsForDivergences;
    private boolean generateTargetsForSyntax;
    private boolean mapOnlyToFirstSyntaxChange;
    private boolean useManualMatchingFile;
    private boolean injectCoverageObserverTwice;
    private boolean noRepoMode;
    private boolean enableInstrumentationCompatibilityMode;
    private boolean filterObjectCharacteristics;


    /**
     * Creates a new, undefined configuration object. Use Setters to fill it with data and the
     * finalize()-method to set the repository.
     *
     * @param b : desired behavior of the tool
     */
    public Configurable(Behavior b){
        assert(b!=null);
        behavior = b;

        //fill in default paths
        this.outputDir = new File("./output");

        //fill in default values
        buildCommand = new String[0];
        ignoreBySuffix = new String[0][];
        ignoreByPrefix = new String[0][];
        excludedMethods = new String[0][];
        excludedDirectories = new String[0][];
        relativePathToSources = new String[0] ;
        relativePathToCompiledSources = new String[0];
        relativePathToAdditionalCompiledSources = new String[0];
        relativePathToBuildFile = new String[0];
        versionIdentifiers = new String[0];

        relativePathToTests = new String[0] ;
        testsToRun = new String[0][];
        testsToSkip = new String[0][];
        executeMainArguments = new String[0][];
        executeMainClassnames = new String[0][];
        runCommands = new String[0][];

        deleteTraces = true;
        sourceExportAllowed = true;
    }

    @Override
    public Behavior getBehavior() {
        return behavior;
    }

    public void setVersionsToMine(String[] versions) {
        versionIdentifiers = versions;

        if (relativePathToSources.length == 1){
            //implements value duplication in case user only gave a single entry
            String val = relativePathToSources[0];
            relativePathToSources = new String[versions.length];
            for (int i = 0; i<versions.length; ++i)
                relativePathToSources[i] = val;
        }
        else if (relativePathToSources.length == 0 /*= not set yet! */){
            relativePathToSources = new String[versions.length]; //we expect the user to provide this data later
        }
        else if (relativePathToSources.length > 1 && relativePathToSources.length != versions.length){
            throw new IllegalArgumentException("Invalid list of source folders given, expected list of size "+versions.length+" but got "+relativePathToSources.length);
        }

        if (relativePathToCompiledSources.length == 1){
            //implements value duplication in case user only gave a single entry
            String val = relativePathToCompiledSources[0];
            relativePathToCompiledSources = new String[versions.length];
            for (int i = 0; i<versions.length; ++i)
                relativePathToCompiledSources[i] = val;
        }
        else if (relativePathToCompiledSources.length == 0  /*= not set yet! */){
            relativePathToCompiledSources = new String[versions.length]; //we expect the user to provide this data later
        }
        else if (relativePathToCompiledSources.length  > 1 && relativePathToCompiledSources.length != versions.length){
            throw new IllegalArgumentException("Invalid list of *compiled* source folders given, expected list of size "+versions.length+" but got "+relativePathToCompiledSources.length);
        }

        if (relativePathToAdditionalCompiledSources.length == 1){
            //implements value duplication in case user only gave a single entry
            String val = relativePathToAdditionalCompiledSources[0];
            relativePathToAdditionalCompiledSources = new String[versions.length];
            for (int i = 0; i<versions.length; ++i)
                relativePathToAdditionalCompiledSources[i] = val;
        }
        else if (relativePathToAdditionalCompiledSources.length == 0  /*= not set yet! */){
            relativePathToAdditionalCompiledSources = new String[versions.length]; //we expect the user to provide this data later
        }
        else if (relativePathToAdditionalCompiledSources.length  > 1 && relativePathToAdditionalCompiledSources.length != versions.length){
            throw new IllegalArgumentException("Invalid list of *additional compiled* source folders given, expected list of size "+versions.length+" but got "+relativePathToAdditionalCompiledSources.length);
        }

        if (buildCommand.length == 1){
            //implements value duplication in case user only gave a single entry
            String val = buildCommand[0];
            buildCommand = new String[versions.length];
            for (int i = 0; i<versions.length; ++i)
                buildCommand[i] = val;
        }
        else if (buildCommand.length == 0  /*= not set yet! */){
            buildCommand = new String[versions.length]; //we expect the user to provide this data later
        }
        else if (buildCommand.length  > 1 && buildCommand.length != versions.length){
            throw new IllegalArgumentException("Invalid list of build commands given, expected list of size "+versions.length+" but got "+buildCommand.length);
        }

        if (ignoreBySuffix.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = ignoreBySuffix[0];
            ignoreBySuffix = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                ignoreBySuffix[i] = val;
        }
        else if (ignoreBySuffix.length == 0  /*= not set yet! */){
            ignoreBySuffix = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (ignoreBySuffix.length  > 1 && ignoreBySuffix.length != versions.length){
            throw new IllegalArgumentException("Invalid list of suffix values given, expected list of size "+versions.length+" but got "+ignoreBySuffix.length);
        }

        if (ignoreByPrefix.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = ignoreByPrefix[0];
            ignoreByPrefix = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                ignoreByPrefix[i] = val;
        }
        else if (ignoreByPrefix.length == 0  /*= not set yet! */){
            ignoreByPrefix = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (ignoreByPrefix.length  > 1 && ignoreByPrefix.length != versions.length){
            throw new IllegalArgumentException("Invalid list of prefix values given, expected list of size "+versions.length+" but got "+ignoreByPrefix.length);
        }

        if (excludedMethods.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = excludedMethods[0];
            excludedMethods = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                excludedMethods[i] = val;
        }
        else if (excludedMethods.length == 0  /*= not set yet! */){
            excludedMethods = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (excludedMethods.length  > 1 && excludedMethods.length != versions.length){
            throw new IllegalArgumentException("Invalid list of methods to exclude from instrumentation given, expected list of size "+versions.length+" but got "+ excludedMethods.length);
        }

        if (testsToRun.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = testsToRun[0];
            testsToRun = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                testsToRun[i] = val;
        }
        else if (testsToRun.length == 0  /*= not set yet! */){
            testsToRun = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (testsToRun.length  > 1 && testsToRun.length != versions.length){
            throw new IllegalArgumentException("Invalid list of tests to run for trace generation given, expected list of size "+versions.length+" but got "+ testsToRun.length);
        }

        if (testsToSkip.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = testsToSkip[0];
            testsToSkip = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                testsToSkip[i] = val;
        }
        else if (testsToSkip.length == 0  /*= not set yet! */){
            testsToSkip = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (testsToSkip.length  > 1 && testsToSkip.length != versions.length){
            throw new IllegalArgumentException("Invalid list of tests to skip for trace generation given, expected list of size "+versions.length+" but got "+ testsToSkip.length);
        }

        if (executeMainArguments.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = executeMainArguments[0];
            executeMainArguments = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                executeMainArguments[i] = val;
        }
        else if (executeMainArguments.length == 0  /*= not set yet! */){
            executeMainArguments = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (executeMainArguments.length  > 1 && executeMainArguments.length != versions.length){
            throw new IllegalArgumentException("Invalid list of arguments for main method calls given, expected list of size "+versions.length+" but got "+ executeMainArguments.length);
        }

        if (executeMainClassnames.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = executeMainClassnames[0];
            executeMainClassnames = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                executeMainClassnames[i] = val;
        }
        else if (executeMainClassnames.length == 0  /*= not set yet! */){
            executeMainClassnames = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (executeMainClassnames.length  > 1 && executeMainClassnames.length != versions.length){
            throw new IllegalArgumentException("Invalid list of fully qualified classnames to use for trace generation given, expected list of size "+versions.length+" but got "+ executeMainClassnames.length);
        }

        if (runCommands.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = runCommands[0];
            runCommands = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                runCommands[i] = val;
        }
        else if (runCommands.length == 0  /*= not set yet! */){
            runCommands = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (runCommands.length  > 1 && runCommands.length != versions.length){
            throw new IllegalArgumentException("Invalid list of run commands for trace generation given, expected list of size "+versions.length+" but got "+ runCommands.length);
        }

        if (relativePathToTests.length == 1){
            //implements value duplication in case user only gave a single entry
            String val = relativePathToTests[0];
            relativePathToTests = new String[versions.length];
            for (int i = 0; i<versions.length; ++i)
                relativePathToTests[i] = val;
        }
        else if (relativePathToTests.length == 0  /*= not set yet! */){
            relativePathToTests = new String[versions.length]; //we expect the user to provide this data later
        }
        else if (relativePathToTests.length  > 1 && relativePathToTests.length != versions.length){
            throw new IllegalArgumentException("Invalid list of test folders given, expected list of size "+versions.length+" but got "+relativePathToTests.length);
        }

        if (excludedDirectories.length == 1){
            //implements value duplication in case user only gave a single entry
            String[] val = excludedDirectories[0];
            excludedDirectories = new String[versions.length][];
            for (int i = 0; i<versions.length; ++i)
                excludedDirectories[i] = val;
        }
        else if (excludedDirectories.length == 0  /*= not set yet! */){
            excludedDirectories = new String[versions.length][]; //we expect the user to provide this data later
        }
        else if (excludedDirectories.length  > 1 && excludedDirectories.length != versions.length){
            throw new IllegalArgumentException("Invalid list of directories to exclude from instrumentation given, expected list of size "+versions.length+" but got "+ excludedDirectories.length);
        }

        if (relativePathToBuildFile.length == 1){
            //implements value duplication in case user only gave a single entry
            String val = relativePathToBuildFile[0];
            relativePathToBuildFile = new String[versions.length];
            for (int i = 0; i<versions.length; ++i)
                relativePathToBuildFile[i] = val;
        }
        else if (relativePathToBuildFile.length == 0  /*= not set yet! */){
            relativePathToBuildFile = new String[versions.length]; //we expect the user to provide this data later
        }
        else if (relativePathToBuildFile.length  > 1 && relativePathToBuildFile.length != versions.length){
            throw new IllegalArgumentException("Invalid list of build files given, expected list of size "+versions.length+" but got "+relativePathToBuildFile.length);
        }
        //else we expect the user to provide this data later
    }

    @Override
    public String[] getVersionIdentifiers() {
        return versionIdentifiers;
    }

    @Override
    public File getOutputDirectory() {
        //no assert here
        return outputDir;
    }

    public void setOutputDirectory(File outputDir){
        this.outputDir = outputDir;
    }

    public void setRelativeLocationOfSources(String[] sources) {
        assert(sources.length >=1 );

        if (relativePathToSources.length == 0  /*= not set yet! */){
            relativePathToSources = new String[sources.length];
            System.arraycopy(sources, 0, relativePathToSources, 0, sources.length);
        }
        else if (relativePathToSources.length != 0 && relativePathToSources[0]==null){
            boolean duplication = (sources.length==1); //value duplication in case versions are already set
            if (!duplication && (relativePathToSources.length != sources.length))
                throw new IllegalArgumentException("Invalid list of paths to source folders given, expected list of size "+
                        relativePathToSources.length+" but got "+sources.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                relativePathToSources[i] = sources[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Source Location given twice!");
        }

        //standardize relative paths (basically, make sure the user input is sanitized)
        for (int i = 0; i<relativePathToSources.length; ++i){
            relativePathToSources[i] = FileUtils.standardizeRelativePaths(relativePathToSources[i]);
        }
    }

    @Override
    public String getRelativePathToSources(int identIndex) {
        if (identIndex<relativePathToSources.length)
            return relativePathToSources[identIndex];
        else if (versionIdentifiers.length == 1 && relativePathToSources.length > 0 && identIndex-1<relativePathToSources.length){
            //this will happen if the versions were expanded by auto-detection
            Tool.printDebug("returning [0] entry for sources for invalid entry "+identIndex);
            return relativePathToSources[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few paths to sources, no path for "+identIndex+s+" version! Continuing with version's main dir...");
            return "";
        }
    }

    public void setRelativeLocationOfCompiledSources(String[] compiled) {
        assert(compiled.length >=1 );

        if (relativePathToCompiledSources.length == 0  /*= not set yet! */){
            relativePathToCompiledSources = new String[compiled.length];
            System.arraycopy(compiled, 0, relativePathToCompiledSources, 0, compiled.length);
        }
        else if (relativePathToCompiledSources.length != 0 && relativePathToCompiledSources[0]==null){
            boolean duplication = (compiled.length==1); //value duplication in case versions are already set
            if (!duplication && (relativePathToCompiledSources.length != compiled.length))
                throw new IllegalArgumentException("Invalid list of paths to compiled source folders given, expected list of size "+
                        relativePathToCompiledSources.length+" but got "+compiled.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                relativePathToCompiledSources[i] = compiled[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Compiled Source Location given twice!");
        }

        //standardize relative paths (basically, make sure the user input is sanitized)
        for (int i = 0; i<relativePathToCompiledSources.length; ++i){
            relativePathToCompiledSources[i] = FileUtils.standardizeRelativePaths(relativePathToCompiledSources[i]);
        }
    }

    public void setRelativeLocationOfAdditionalCompiledSources(String[] compiled) {
        assert(compiled.length >=1 );

        if (relativePathToAdditionalCompiledSources.length == 0  /*= not set yet! */){
            relativePathToAdditionalCompiledSources = new String[compiled.length];
            System.arraycopy(compiled, 0, relativePathToAdditionalCompiledSources, 0, compiled.length);
        }
        else if (relativePathToAdditionalCompiledSources.length != 0 && relativePathToAdditionalCompiledSources[0]==null){
            boolean duplication = (compiled.length==1); //value duplication in case versions are already set
            if (!duplication && (relativePathToAdditionalCompiledSources.length != compiled.length))
                throw new IllegalArgumentException("Invalid list of paths to additional compiled source folders given, expected list of size "+
                        relativePathToAdditionalCompiledSources.length+" but got "+compiled.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                relativePathToAdditionalCompiledSources[i] = compiled[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Additional Compiled Source Location given twice!");
        }

        //standardize relative paths (basically, make sure the user input is sanitized)
        for (int i = 0; i<relativePathToAdditionalCompiledSources.length; ++i){
            relativePathToAdditionalCompiledSources[i] = FileUtils.standardizeRelativePaths(relativePathToAdditionalCompiledSources[i]);
        }
    }

    @Override
    public String getRelativePathToAdditionalCompiledSources(int identIndex) {
        if (identIndex<relativePathToAdditionalCompiledSources.length)
            return relativePathToAdditionalCompiledSources[identIndex];
        else if (versionIdentifiers.length == 1 && relativePathToAdditionalCompiledSources.length > 0 && identIndex-1<relativePathToAdditionalCompiledSources.length){
            //this will happen if the versions were expanded by auto-detection
            Tool.printDebug("returning [0] entry for additional compiled sources for invalid entry "+identIndex);
            return relativePathToAdditionalCompiledSources[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few paths to additional compiled sources, no path for "+identIndex+s+" version! Continuing with version's main dir...");
            return "";
        }
    }

    @Override
    public String getRelativePathToCompiledSources(int identIndex) {
        if (identIndex<relativePathToCompiledSources.length)
            return relativePathToCompiledSources[identIndex];
        else if (versionIdentifiers.length == 1 && relativePathToCompiledSources.length > 0 && identIndex-1<relativePathToCompiledSources.length){
            //this will happen if the versions were expanded by auto-detection
            Tool.printDebug("returning [0] entry for compiled sources for invalid entry "+identIndex);
            return relativePathToCompiledSources[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few paths to compiled sources, no path for "+identIndex+s+" version! Continuing with version's main dir...");
            return "";
        }
    }

    @Override
    public String getBuildCommand(int identIndex) {
        if (identIndex<buildCommand.length)
            return buildCommand[identIndex];
        else if (versionIdentifiers.length == 1 && buildCommand.length > 0 && identIndex-1<buildCommand.length){
            //this will happen if the versions were expanded by auto-detection
            Tool.printDebug("returning [0] entry for build commands for invalid entry "+identIndex);
            return buildCommand[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few build commands, no command for "+identIndex+s+" version! Continuing with empty command...");
            return "";
        }
    }

    public void setBuildString(String[] build) {
        assert(build.length >=1 );

        if (buildCommand.length == 0  /*= not set yet! */){
            buildCommand = new String[build.length];
            System.arraycopy(build, 0, buildCommand, 0, build.length);
        }
        else if (buildCommand.length != 0 && buildCommand[0]==null){
            boolean duplication = (build.length==1); //value duplication in case versions are already set
            if (!duplication && (buildCommand.length != build.length))
                throw new IllegalArgumentException("Invalid list of build commands given, expected list of size "+
                        buildCommand.length+" but got "+build.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                buildCommand[i] = build[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Build Command given twice!");
        }
    }

    @Override
    public String getRelativePathToBuildFile(int identIndex) {
        if (identIndex<relativePathToBuildFile.length)
            return relativePathToBuildFile[identIndex];
        else if (versionIdentifiers.length == 1 && relativePathToBuildFile.length > 0 && identIndex-1<relativePathToBuildFile.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for build file for invalid entry "+identIndex);
            return relativePathToBuildFile[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few paths to compiled sources, no path for "+identIndex+s+" version! Continuing with version's main dir...");
            return "";
        }
    }

    public void setRelativePathToBuildFile(String[] buildLocation) {
        assert(buildLocation.length >=1 );

        if (relativePathToBuildFile.length == 0  /*= not set yet! */){
            relativePathToBuildFile = new String[buildLocation.length];
            System.arraycopy(buildLocation, 0, relativePathToBuildFile, 0, buildLocation.length);
        }
        else if (relativePathToBuildFile.length != 0 && relativePathToBuildFile[0]==null){
            boolean duplication = (buildLocation.length==1); //value duplication in case versions are already set
            if (!duplication && (relativePathToBuildFile.length != buildLocation.length))
                throw new IllegalArgumentException("Invalid list of build files given, expected list of size "+
                        relativePathToBuildFile.length+" but got "+buildLocation.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                relativePathToBuildFile[i] = buildLocation[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Build File given twice!");
        }

        //standardize relative paths (basically, make sure the user input is sanitized)
        for (int i = 0; i<relativePathToBuildFile.length; ++i){
            relativePathToBuildFile[i] = FileUtils.standardizeRelativePaths(relativePathToBuildFile[i]);
        }
    }


    @Override
    public String[] getIgnoreBySuffix(int identIndex) {
        if (identIndex<ignoreBySuffix.length)
            return ignoreBySuffix[identIndex];
        else if (versionIdentifiers.length == 1 && ignoreBySuffix.length > 0 && identIndex-1<ignoreBySuffix.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for suffix of tests for invalid entry "+identIndex);
            return ignoreBySuffix[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few suffix values for tests, no suffix given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setIgnoreBySuffix(String[][] suffixList){
        assert(suffixList.length >=1 ); //there is at least an empty entry in the list!

        if (ignoreBySuffix.length == 0){
            /* this means versions have not yet been set as well! */
            ignoreBySuffix = suffixList;
        }
        else if (ignoreBySuffix.length != 0 && ignoreBySuffix[0]==null){
            boolean duplication = (suffixList.length==1); //value duplication in case versions are already set
            if (!duplication && (ignoreBySuffix.length != suffixList.length))
                throw new IllegalArgumentException("Invalid list of suffix values given, expected list of size "+
                        ignoreBySuffix.length+" but got "+suffixList.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                ignoreBySuffix[i] = suffixList[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Suffixes given twice!");
        }
    }

    @Override
    public String[] getIgnoreByPrefix(int identIndex) {
        if (identIndex<ignoreByPrefix.length)
            return ignoreByPrefix[identIndex];
        else if (versionIdentifiers.length == 1 && ignoreByPrefix.length > 0 && identIndex-1<ignoreByPrefix.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for prefix of tests for invalid entry "+identIndex);
            return ignoreByPrefix[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few prefix values for tests, no prefix given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setIgnoreByPrefix(String[][] prefixList){
        assert(prefixList.length >=1 );  //there is at least an empty entry in the list!

        if (ignoreByPrefix.length == 0){
            /* this means versions have not yet been set as well! */
            ignoreByPrefix = prefixList;
        }
        else if (ignoreByPrefix.length != 0 && ignoreByPrefix[0]==null){
            boolean duplication = (prefixList.length==1); //value duplication in case versions are already set
            if (!duplication && (ignoreByPrefix.length != prefixList.length))
                throw new IllegalArgumentException("Invalid list of prefix values given, expected list of size "+
                        ignoreByPrefix.length+" but got "+prefixList.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                ignoreByPrefix[i] = prefixList[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Prefixes given twice!");
        }
    }

    public void setExcludeMethodsFromInstrumentationBySignature(String[][] methodList) {
        assert(methodList.length >=1 );  //there is at least an empty entry in the list!

        if (excludedMethods.length == 0){
            /* this means versions have not yet been set as well! */
            excludedMethods = methodList;
        }
        else if (excludedMethods.length != 0 && excludedMethods[0]==null){
            boolean duplication = (methodList.length==1); //value duplication in case versions are already set
            if (!duplication && (excludedMethods.length != methodList.length))
                throw new IllegalArgumentException("Invalid list of methods to exclude from instrumentation given"+
                        ", expected list of size "+ excludedMethods.length+" but got "+methodList.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                excludedMethods[i] = methodList[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Excluded Method Signatures given twice!");
        }
    }

    @Override
    public String[] getExcludeMethodsFromInstrumentationBySignature(int identIndex) {
        if (identIndex< excludedMethods.length)
            return excludedMethods[identIndex];
        else if (versionIdentifiers.length == 1 && excludedMethods.length > 0 && identIndex-1< excludedMethods.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for methods excluded for invalid entry "+identIndex);
            return excludedMethods[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few excluded methods provided, no methods given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    @Override
    public String[] getRunCommands(int identIndex) {
        if (identIndex<runCommands.length)
            return runCommands[identIndex];
        else if (versionIdentifiers.length == 1 && runCommands.length > 0 && identIndex-1<runCommands.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for run command for invalid entry "+identIndex);
            return runCommands[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few run commands, no command given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setRunCommands(String[][] runCommands) {
        assert(runCommands.length >=1 );  //there is at least an empty entry in the list!

        if (this.runCommands.length == 0){
            /* this means versions have not yet been set as well! */
            this.runCommands = runCommands;
        }
        else if (this.runCommands.length != 0 && this.runCommands[0]==null){
            boolean duplication = (runCommands.length==1); //value duplication in case versions are already set
            if (!duplication && (this.runCommands.length != runCommands.length))
                throw new IllegalArgumentException("Invalid list of run commands for trace generation given"+
                        ", expected list of size "+ this.runCommands.length+" but got "+runCommands.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                this.runCommands[i] = runCommands[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Run commands for trace generation given twice!");
        }
    }

    @Override
    public String[] getExecuteMainArguments(int identIndex) {
        if (identIndex<executeMainArguments.length)
            return executeMainArguments[identIndex];
        else if (versionIdentifiers.length == 1 && executeMainArguments.length > 0 && identIndex-1<executeMainArguments.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for main method arguments for invalid entry "+identIndex);
            return executeMainArguments[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few main method arguments, no argument given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setExecuteMainArguments(String[][] executeMainArguments) {
        assert(executeMainArguments.length >=1 );  //there is at least an empty entry in the list!

        if (this.executeMainArguments.length == 0){
            /* this means versions have not yet been set as well! */
            this.executeMainArguments = executeMainArguments;
        }
        else if (this.executeMainArguments.length != 0 && this.executeMainArguments[0]==null){
            boolean duplication = (executeMainArguments.length==1); //value duplication in case versions are already set
            if (!duplication && (this.executeMainArguments.length != executeMainArguments.length))
                throw new IllegalArgumentException("Invalid list of main method arguments given"+
                        ", expected list of size "+ this.executeMainArguments.length+" but got "+executeMainArguments.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                this.executeMainArguments[i] = executeMainArguments[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Main method arguments given twice!");
        }
    }

    @Override
    public String[] getExecuteMainClassnames(int identIndex) {
        if (identIndex<executeMainClassnames.length)
            return executeMainClassnames[identIndex];
        else if (versionIdentifiers.length == 1 && executeMainClassnames.length > 0 && identIndex-1<executeMainClassnames.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for main methods for invalid entry "+identIndex);
            return executeMainClassnames[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few classpahts for main methods, no classpath given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setExecuteMainClassnames(String[][] executeMainClassnames) {
        assert(executeMainClassnames.length >=1 );  //there is at least an empty entry in the list!

        if (this.executeMainClassnames.length == 0){
            /* this means versions have not yet been set as well! */
            this.executeMainClassnames = executeMainClassnames;
        }
        else if (this.executeMainClassnames.length != 0 && this.executeMainClassnames[0]==null){
            boolean duplication = (executeMainClassnames.length==1); //value duplication in case versions are already set
            if (!duplication && (this.executeMainClassnames.length != executeMainClassnames.length))
                throw new IllegalArgumentException("Invalid list of main methods to execute given"+
                        ", expected list of size "+ this.executeMainClassnames.length+" but got "+executeMainClassnames.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                this.executeMainClassnames[i] = executeMainClassnames[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Main methods for trace generation given twice!");
        }
    }

    @Override
    public String[] getTestsToRun(int identIndex) {
        if (identIndex< testsToRun.length)
            return testsToRun[identIndex];
        else if (versionIdentifiers.length == 1 && testsToRun.length > 0 && identIndex-1< testsToRun.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for tests to run for invalid entry index "+identIndex);
            return testsToRun[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few test to run, no JUnit tests given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setTestsToRun(String[][] testsToRun) {
        assert(testsToRun.length >=1 );  //there is at least an empty entry in the list!

        if (this.testsToRun.length == 0){
            /* this means versions have not yet been set as well! */
            this.testsToRun = testsToRun;
        }
        else if (this.testsToRun.length != 0 && this.testsToRun[0]==null){
            boolean duplication = (testsToRun.length==1); //value duplication in case versions are already set
            if (!duplication && (this.testsToRun.length != testsToRun.length))
                throw new IllegalArgumentException("Invalid list of JUnit tests to run given"+
                        ", expected list of size "+ this.testsToRun.length+" but got "+ testsToRun.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                this.testsToRun[i] = testsToRun[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("JUnit tests to run given twice!");
        }

        //arguments are either relative paths or fully qualified classnames, sanatizing has no effect on the later!
        for (int i = 0; i<testsToRun.length; ++i){
            for (int j = 0; j<testsToRun[i].length; ++j) {
                testsToRun[i][j] = FileUtils.standardizeRelativePaths(testsToRun[i][j]);
            }
        }
    }


    @Override
    public String[] getTestsToSkip(int identIndex) {
        if (identIndex< testsToSkip.length)
            return testsToSkip[identIndex];
        else if (versionIdentifiers.length == 1 && testsToSkip.length > 0 && identIndex-1< testsToSkip.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for tests to skip for invalid entry index "+identIndex);
            return testsToSkip[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few test to skip, no JUnit tests given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setTestsToSkip(String[][] testsToSkip) {
        assert(testsToSkip.length >=1 );  //there is at least an empty entry in the list!

        if (this.testsToSkip.length == 0){
            /* this means versions have not yet been set as well! */
            this.testsToSkip = testsToSkip;
        }
        else if (this.testsToSkip.length != 0 && this.testsToSkip[0]==null){
            boolean duplication = (testsToSkip.length==1); //value duplication in case versions are already set
            if (!duplication && (this.testsToSkip.length != testsToSkip.length))
                throw new IllegalArgumentException("Invalid list of JUnit tests to skip given"+
                        ", expected list of size "+ this.testsToSkip.length+" but got "+ testsToSkip.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                this.testsToSkip[i] = testsToSkip[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("JUnit tests to skip given twice!");
        }

        //arguments are either relative paths or fully qualified classnames, sanatizing has no effect on the later!
        for (int i = 0; i<testsToSkip.length; ++i){
            for (int j = 0; j<testsToSkip[i].length; ++j) {
                testsToSkip[i][j] = FileUtils.standardizeRelativePaths(testsToSkip[i][j]);
            }
        }
    }


    
    public void setRelativePathToTests(String[] testClasspaths) {
        assert(testClasspaths.length >=1 );

        if (relativePathToTests.length == 0  /*= not set yet! */){
            relativePathToTests = new String[testClasspaths.length];
            System.arraycopy(testClasspaths, 0, relativePathToTests, 0, testClasspaths.length);
        }
        else if (relativePathToTests.length != 0 && relativePathToTests[0]==null){
            boolean duplication = (testClasspaths.length==1); //value duplication in case versions are already set
            if (!duplication && (relativePathToTests.length != testClasspaths.length))
                throw new IllegalArgumentException("Invalid list of paths to test folders given, expected list of size "+
                        relativePathToTests.length+" but got "+testClasspaths.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                relativePathToTests[i] = testClasspaths[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Test Location given twice!");
        }

        //standardize relative paths (basically, make sure the user input is sanitized)
        for (int i = 0; i<relativePathToTests.length; ++i){
            relativePathToTests[i] = FileUtils.standardizeRelativePaths(relativePathToTests[i]);
        }
    }

    @Override
    public String getRelativePathToTests(int identIndex) {
        if (identIndex<relativePathToTests.length)
            return relativePathToTests[identIndex];
        else if (versionIdentifiers.length == 1 && relativePathToTests.length > 0 && identIndex-1<relativePathToTests.length){
            //this will happen if the versions were expanded by auto-detection
            Tool.printDebug("returning [0] entry for test path for invalid entry "+identIndex);
            return relativePathToTests[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few paths to tests, no path for "+identIndex+s+" version! Continuing with version's main dir...");
            return "";
        }
    }
    

    public void setDirectoriesExcludedFromInstrumentation(String[][] directoryList) {
        assert(directoryList.length >=1 );  //there is at least an empty entry in the list!

        if (excludedDirectories.length == 0){
            /* this means versions have not yet been set as well! */
            excludedDirectories = directoryList;
        }
        else if (excludedDirectories.length != 0 && excludedDirectories[0]==null){
            boolean duplication = (directoryList.length==1); //value duplication in case versions are already set
            if (!duplication && (excludedDirectories.length != directoryList.length))
                throw new IllegalArgumentException("Invalid list of directories to exclude from instrumentation given"+
                        ", expected list of size "+ excludedDirectories.length+" but got "+directoryList.length);

            for (int i = 0; i<versionIdentifiers.length; ++i)
                excludedDirectories[i] = directoryList[duplication?0:i];
        }
        else{
            throw new IllegalArgumentException("Excluded Directories given twice!");
        }

        //standardize relative paths (basically, make sure the user input is sanitized)
        for (int i = 0; i<excludedDirectories.length; ++i){
            for (int j = 0; j<excludedDirectories[i].length; ++j) {
                excludedDirectories[i][j] = FileUtils.standardizeRelativePaths(excludedDirectories[i][j]);
            }
        }
    }

    @Override
    public String[] getDirectoriesExcludedFromInstrumentation(int identIndex) {
        if (identIndex< excludedDirectories.length)
            return excludedDirectories[identIndex];
        else if (versionIdentifiers.length == 1 && excludedDirectories.length > 0 && identIndex-1< excludedDirectories.length){
            //this will happen if the versions were expanded by auto-detection, its not a bug but a feature
            Tool.printDebug("returning [0] entry for methods excluded for invalid entry "+identIndex);
            return excludedDirectories[0];
        }
        else{
            String s = "";
            switch (identIndex){ case 1 : s = "st"; break; case 2 : s = "nd"; break; default : s = "th";}
            Tool.printDebug("Too few excluded directories provided, no methods given for "+identIndex+s+" version! Ignoring provided data...");
            return new String[]{""};
        }
    }

    public void setRepoDir(File repoDir) {
        this.repoDir = repoDir;
    }

    @Override
    public File getRepoDir() {
        return repoDir;
    }

    @Override
    public boolean isPresentDataToBeOverwritten() {
        return overwrite;
    }

    public void enableOverwrite() {
        overwrite = true;
        deleteCompiledSourceBeforeCompilation = true;
    }

    @Override
    public boolean areRenamedElementsToBeTreatedAsSyntaxChanges() {
        return coverRenamed;
    }

    public void enableRenamedCodeCoverage() {
        coverRenamed = true;
    }
    
    public void enableNoRepoMode(){ noRepoMode = true;}


    /**
     * Verifies every must-have that can be checked prior to actual execution,
     * then returns a read-only object of this instance.
     *
     * @return a read-only version of this instance
     * @throws IllegalArgumentException : if necessary data has not yet been provided by the user
     */
    public Configuration finalizeConfig() throws IllegalArgumentException{
        if (!noRepoMode) {
            if (!repoDir.exists() || !repoDir.isDirectory()) {
                String s = "Repository directory '" + repoDir.getAbsolutePath() + "' does not exist!";
                Tool.printError(s);
                throw new IllegalArgumentException(s);
            }
            if (miner == null) {
                String s = "Cannot create a Miner for the given repository!";
                Tool.printError(s);
                throw new IllegalArgumentException(s);
            }
        } else {
            if (! (repoDir == null && miner == null)) {
                String s = "If '"+ Commands.noRepo+"' is used, do not provide a repository!";
                Tool.printError(s);
                throw new IllegalArgumentException(s);
            }
        }

        if (!outputDir.exists() || !outputDir.isDirectory()  || !outputDir.canWrite()){
            String s = "Output directory '"+outputDir.getAbsolutePath()+"' does not exist or cannot be written to!";
            Tool.printError(s);
            throw new IllegalArgumentException(s);
        }
        if (versionIdentifiers.length==0){
            String s = "No versions to extract and analyze have been chosen!";
            Tool.printError(s);
            throw new IllegalArgumentException(s);
        }
        if (allVersions == null || allVersions.size()==0){
            String s = "Could not determine any versions inside the given repository!";
            Tool.printError(s);
            throw new IllegalArgumentException(s);
        }

        //at this point, versions have been set. If no e.g. sources have been provided, the array has the right length but contains only null!
        int i = 0;
        if (relativePathToSources[0] == null) while (i<versionIdentifiers.length){relativePathToSources[i]=""; ++i;} i=0;
        if (relativePathToCompiledSources[0] == null) while (i<versionIdentifiers.length){relativePathToCompiledSources[i]=""; ++i;} i=0;
        if (relativePathToAdditionalCompiledSources[0] == null) while (i<versionIdentifiers.length){relativePathToAdditionalCompiledSources[i]=""; ++i;} i=0;
        if (buildCommand[0] == null) while (i<versionIdentifiers.length){buildCommand[i]=""; ++i;} i=0;
        if (ignoreByPrefix[0] == null) while (i<versionIdentifiers.length){ignoreByPrefix[i]=new String[0]; ++i;} i=0;
        if (ignoreBySuffix[0] == null) while (i<versionIdentifiers.length){ignoreBySuffix[i]=new String[0]; ++i;} i=0;
        if (excludedDirectories[0] == null) while (i<versionIdentifiers.length){excludedDirectories[i]=new String[0]; ++i;} i=0;
        if (relativePathToBuildFile[0] == null) while (i<versionIdentifiers.length){relativePathToBuildFile[i]=""; ++i;} i=0;

        if (executeMainClassnames[0] == null) while (i<versionIdentifiers.length){executeMainClassnames[i]=new String[0]; ++i;} i=0;
        if (executeMainArguments[0] == null) while (i<versionIdentifiers.length){executeMainArguments[i]=new String[0]; ++i;} i=0;
        if (runCommands[0] == null) while (i<versionIdentifiers.length){runCommands[i]=new String[0]; ++i;} i=0;
        if (relativePathToTests[0] == null) while (i<versionIdentifiers.length){relativePathToTests[i]=relativePathToCompiledSources[i]; ++i;} i=0;
        if (testsToRun[0] == null) while (i<versionIdentifiers.length){testsToRun[i]=new String[0]; ++i;} i=0;
        if (testsToSkip[0] == null) while (i<versionIdentifiers.length){testsToSkip[i]=new String[0]; ++i;} i=0;

        //finally, if exclude has not been set, we set it ourselves with few default values!
        if (excludedMethods[0] == null)
            while (i< excludedMethods.length){
                excludedMethods[i]=new String[]{
                        "toString()Ljava/lang/String;", //we ignore toString()
                        "hashCode()I", //hashCode() is often used internally (lists, sets, ...)
                        "equals(Ljava/lang/Object;)Z",// as is equals()
                };
                ++i;
            }


        return this; //finalized config
    }

    public void setRepositoryMiner(Miner miner) {
        this.miner = miner;
    }

    @Override
    public Miner getRepositoryMiner() {
        return miner;
    }

    public void deleteRepoAtTheEnd() {
        this.deleteRepo = true;
    }

    public boolean isRepoToBeDeletedAtTheEnd(){
        return deleteRepo;
    }

    public void setAllVersionsOfRepo(VersionList list) {
        allVersions = list;
    }

    @Override
    public VersionList getAllVersionsOfRepo() {
        return allVersions;
    }
    

    public void disableTraceDeletion(){
        this.deleteTraces = false;
    }

    @Override
    public boolean areTracesToBeDeleted(){
        return deleteTraces;
    }

    @Override
    public boolean compareTraceCoverage() {
        return coverageBasedTraces;
    }

    @Override
    public boolean compareTraceDistance() {
        return distanceBasedTraces;
    }

    public void enableDistanceBasedTraces(){
        this.distanceBasedTraces = true;
    }

    public void enableCoverageBasedTraces(){
        this.coverageBasedTraces = true;
    }

    public void disableSourceExport() {
        this.sourceExportAllowed = false;
    }

    @Override
    public boolean exportSourceCodeToDirectory() {
        return sourceExportAllowed;
    }

    public void deleteCompiledSourceBeforeCompilation() {
        deleteCompiledSourceBeforeCompilation = true;
    }

    @Override
    public boolean areCompiledSourcesToBeDeletedBeforeCompilation() {
        return deleteCompiledSourceBeforeCompilation;
    }

    public void exportTraceDivergencesInFlatFormat() {
        this.exportTracesFlat = true;
    }

    @Override
    public boolean areTracesToBeExportedAsList() {
        return exportTracesFlat;
    }

    public void enableDivergenceTargetGeneration() {
        generateTargetsForDivergences = true;
    }

    public void enableSyntaxTargetGeneration() {
        generateTargetsForSyntax = true;
    }

    public boolean generateTargetsForSyntax(){
        return generateTargetsForSyntax;
    }

    public boolean generateTargetsForDivergences(){return generateTargetsForDivergences;}

    @Override
    public boolean needToUseManualTraceMatchingFile() {return useManualMatchingFile;}

    public void useManualTraceMatchingFile() {
        useManualMatchingFile = true;
    }

    @Override
    public boolean mapOnlyToFirstSyntaxChange() {return mapOnlyToFirstSyntaxChange;}

    public void restrictMappingOfDivergencesToFirstSyntaxChange() {
        mapOnlyToFirstSyntaxChange = true;
    }

    @Override
    public boolean needToInjectCoverageObserverTwice() {return injectCoverageObserverTwice;}

    @Override
    public boolean useMoreCompatibleInstrumentation() {
        return enableInstrumentationCompatibilityMode;
    }

    public void enableInstrumentationCompatibiltyMode() {
        enableInstrumentationCompatibilityMode = true;
    }

    public void injectCoverageObserverTwice() {
        injectCoverageObserverTwice = true;
    }

    @Override
    public boolean filterObjectCharacteristics() {
        return filterObjectCharacteristics;
    }

    public void enableFilteringOfObjectCharacteristics() {
        filterObjectCharacteristics = true;
    }
}
