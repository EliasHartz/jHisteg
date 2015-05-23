package hartz.masterThesis.historyGuidedImpactAnalysis.repoMining;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileDiffType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileLevelChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.VersionList;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.VersionSettingDifference;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.repoMining.miners.GitMiner;
import hartz.masterThesis.historyGuidedImpactAnalysis.repoMining.miners.SVNMiner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The common super-class of all repository miners, defines their
 * structure and required capabilities. Also doubles as the basic
 * factory class to construct Miner's smartly.
 */
public abstract class Miner {

    /**
     * Checks out all commits defined in the configuration object and copies them into
     * the output folder. Will return the list of changes on file-level between the commits.
     * It is not guaranteed that this method leaves the repository unchanged or functional.
     *

     * @param versionsToCheckout : an array of (not necessarily consecutive or ordered) versions
     * @param forceOverwrite : set to 'true' to enforce re-generation of data, otherwise data might
     *                         be imported from previous runs
     * @return the list of changes on a file-level between the versions, KEEP IN MIND THAT
     *         THIS LIST IS ONE ELEMENT SHOTER THAN THE AMOUNTS OF VERSIONS since the first
     *         entry of that list describes the changes between version 0 and 1.
     * @throws IOException : if any of the involved directories cannot be accessed properly
     * @throws IllegalArgumentException : if an previous version is present and incompatible
     */

    public HashMap<Version, List<FileLevelChange>> checkoutCommits(Version[] versionsToCheckout, boolean forceOverwrite)
            throws IOException, IllegalArgumentException {
        assert(versionsToCheckout.length>0); //otherwise this should not be called

        HashMap<Version, List<FileLevelChange>> result = new HashMap<Version, List<FileLevelChange>>();
        try{

            //create folders and scan what is already present
            boolean[] needToExtract = new boolean[versionsToCheckout.length];
            for (int i = 0; i<versionsToCheckout.length; ++i){
                Version version = versionsToCheckout[i];
                needToExtract[i] = createMainAndExtractedDirs(version, forceOverwrite);
            }

            //store the commit data:
            for (int i = 0; i<versionsToCheckout.length; ++i){
                Version version = versionsToCheckout[i];
                assert(version.dataIsComplete());
                if (!needToExtract[i]){
                        VersionSettingDifference d = version.checkAgainstPrevious();
                        if (d != null) {
                            Tool.print(" * previous data exists for version '" + version.identifier + "' but it was extracted with different settings:");
                            Tool.print("   " + d + " This difference will lead to corrupted results!");
                            Tool.print(" * cannot continue until one of the following actions is taken");
                            Tool.print("    - you configure your call to match what was extracted to avoid any corrupted results");
                            Tool.print("    - you delete the directory containing the previous version manually and extract anew");
                            Tool.print("    - you use the '" + Commands.forceOverwrite + "' flag which has the same effect but works automatically");
                            throw new IllegalArgumentException("previous data exists for version '" + version.identifier + "' but it was extracted with different settings: " + d);
                        }
                }
                else version.persistIntoMainDirectory(); //store the commit data
            }

            //checkout all versions requested in the configuration
            Tool.print(" * starting extraction of versions, BE PATIENT THIS MIGHT TAKE A WHILE!");
            boolean didWeCheckoutSomething = false;
            for (int i = 0; i<versionsToCheckout.length; ++i){
                if (!needToExtract[i]){
                    Tool.print("   - skipped '"+versionsToCheckout[i].identifier+"' as its already present ["+(i+1)+"/"+versionsToCheckout.length+"]");
                    continue; //skip these entries
                }
                checkoutVersion(versionsToCheckout[i]);
                Tool.print("   - completed extraction of version '"+versionsToCheckout[i].identifier+"' ["+(i+1)+"/"+versionsToCheckout.length+"]");
                didWeCheckoutSomething = true;
            }
            if (didWeCheckoutSomething)
                cleanUpRepo();
            Tool.print(" * extraction completed, individual versions can now be at '"+
                    versionsToCheckout[0].getMainDirectory().getParentFile().getAbsolutePath()+"'");


            //generate the file-level differences
            Tool.print(" * computing file-level differences for all versions");
            for (int i = 0; i<versionsToCheckout.length; ++i){
                if (i==0){
                    //for the first version, we need some special treatment because we HAVE NOT EXTRACTED its predecessor!
                    VersionList fullHistory = getCommitList();
                    Version firstVersionToMine = versionsToCheckout[0];
                    int indexInHistory = fullHistory.indexOf(firstVersionToMine);

                    if (indexInHistory!=0){
                        result.put(firstVersionToMine, computeFileLevelDifferences(
                                        fullHistory.getVersionByIndex(indexInHistory-1), //automatic predecessor computation
                                        firstVersionToMine //first version the user specified
                                )
                        );
                    }
                    //otherwise it is the initial version of the repo (= the very first commit), for which we need an even more special treatment!
                    else result.put(firstVersionToMine, handleInitialVersion(firstVersionToMine));
                }
                else {
                    Version v1 = versionsToCheckout[i-1];
                    Version v2 = versionsToCheckout[i];
                    result.put(v2, computeFileLevelDifferences(v1,v2));
                }
            }
        }
        catch (IOException e){
            Tool.print("An unexpected IO error occurred during the extraction of the desired versions!\n");
            Tool.printDebug(e);
            throw new IOException("An unexpected IO error occurred during the extraction of the desired versions!");
        }

        return result;
    }


    /**
     * Exports both all and source-code only file-level changes into the version's main directory.
     * NOTE: The given list will be filtered and will only contain source code changes afterwards!
     *
     * @param v : the version for which the changes were computed
     * @param fileLevelChanges : all file-level changes, THIS LIST WILL BE MODIFIED IN THIS METHOD!
     * @throws IOException
     */
    protected void persistAndFilterFileLevelChanges(Version v, List<FileLevelChange> fileLevelChanges) throws IOException{
        File allChangesFile = new File (v.getMainDirectory().getAbsolutePath()+"/allFileLevelChanges");
        File codeChangesFile = new File (v.getMainDirectory().getAbsolutePath()+"/codeFileLevelChanges");
        assert(!allChangesFile.exists()); //must not exist yet, otherwise this call is useless
        assert(!codeChangesFile.exists());

        codeChangesFile.createNewFile();
        allChangesFile.createNewFile();

        //export all the file level differences:
        JSONArray a = new JSONArray(); //convert to JSON
        for (FileLevelChange d : fileLevelChanges)
            a.put(d.toJSONObject());

        BufferedWriter writer = new BufferedWriter(new FileWriter(allChangesFile));
        writer.write(a.toString(Globals.jsonIndentFactor));//store as JSON
        writer.close();
        Tool.printExtraInfo("   - exported *all* file-level changes to file '"+allChangesFile.getAbsolutePath()+"'");


        //after exporting all changes, filter out those that have nothing to do with the source code to save time later!
        HashSet<FileLevelChange> codeOnly = new HashSet<>();
        for (FileLevelChange d : fileLevelChanges)
            if (d.isPartOfSourceCode()) codeOnly.add(d);
        fileLevelChanges.retainAll(codeOnly);

        //NOTE: we will always import these filtered later changes, we do not care about others!
        writer = new BufferedWriter(new FileWriter(codeChangesFile));
        a = new JSONArray();
        for (FileLevelChange d : fileLevelChanges)
            a.put(d.toJSONObjectWithoutPartOfSource());
        writer.write(a.toString(Globals.jsonIndentFactor));//store as JSON
        writer.close();
        Tool.printExtraInfo("   - exported source code file-level changes to file '"+codeChangesFile.getAbsolutePath()+"'");
    }

    private ArrayList<FileLevelChange> handleInitialVersion(Version firstVersion) throws IOException {
        File folderOfInitalCommit = firstVersion.getActualExtractedRepo();
        File changesFile = new File(firstVersion.getMainDirectory().getAbsolutePath()+"/codeFileLevelChanges");

        if (changesFile.exists()){
            //read the data from previous run:
            String data = FileUtils.readData(changesFile);
            JSONArray a = new JSONArray(data);
            ArrayList<FileLevelChange> fileLevelChangesList = new ArrayList<FileLevelChange>(a.length());
            for (int j=0; j<a.length();++j){
                fileLevelChangesList.add(new FileLevelChange(a.getJSONObject(j)));
            }
            Tool.printExtraInfo("   - imported file-level changes of initial version from present data");
            return fileLevelChangesList;
        }

        List<String> allFiles = FileUtils.listAllFilesInDirectoryAsString(folderOfInitalCommit);
        ArrayList<FileLevelChange> initialVersionFileChanges = new ArrayList<>();

        for (String s : allFiles){
            String changedFile = FileUtils.convertToRelativePath(s, folderOfInitalCommit.getAbsolutePath());
            boolean partOfProjectSourceCode = changedFile.toLowerCase().endsWith(".java") &&
                    FileUtils.isSubDirectoryOrFile(firstVersion.getSourcesDir(),
                            new File(firstVersion.getActualExtractedRepo()+"/"+changedFile));

            initialVersionFileChanges.add(new FileLevelChange(FileDiffType.ADDED, partOfProjectSourceCode, changedFile, null));
        }
        persistAndFilterFileLevelChanges(firstVersion, initialVersionFileChanges);
        return initialVersionFileChanges;
    }


    private List<FileLevelChange> computeFileLevelDifferences(Version version1, Version version2) throws IOException {
        File changes = new File(version2.getMainDirectory().getAbsoluteFile()+"/codeFileLevelChanges");

        if (changes.exists()){
            //read the data from previous run:
            assert(changes.exists()); //file must exist from previous run
            String data = FileUtils.readData(changes);
            JSONArray a = new JSONArray(data);
            List<FileLevelChange> fileLevelChangesList = new ArrayList<FileLevelChange>(a.length());
            for (int j=0; j<a.length();++j){
                fileLevelChangesList.add(new FileLevelChange(a.getJSONObject(j)));
            }
            Tool.printExtraInfo("   - imported file-level changes of '"+version2.identifier+"' from present data");
            return fileLevelChangesList;
        }
        else{
            Tool.printExtraInfo("   - comparing '"+version2.identifier+"' with predecessor '"+version1.identifier+"'");
            //compute the data
            List<FileLevelChange> l = computeFileLevelChanges(version1, version2);
            return l;
        }
    }

    /**
     * Creates the directory to house all extracted data of a commit. If folder is already present,
     * it can be assumed that this data has been extracted before in later steps!
     *
     * @param version : the version to checkout
     * @param forceOverwrite : if resources shall be overwritten
     * @return 'true' if a folder was created, 'false' if a folder is already present!
     * @throws IOException in case some directories cannot be accessed
     */
    protected boolean createMainAndExtractedDirs(Version version, boolean forceOverwrite) throws IOException{
        File f = version.getMainDirectory();
        File e = version.getActualExtractedRepo();

        if (f.exists()){
            if (e.exists()){
                if (forceOverwrite){
                    Tool.printExtraInfo("   - version "+version.identifier+" was extracted before, will now overwrite!");
                    FileUtils.removeDirectory(f); //delete everything!
                    e.mkdirs();
                }
                else
                    //extracted folder already exits, we assume this means that we have extracted this version before!
                    return false;
            }
            else e.mkdir();
        }
        else{
            //completely fresh checkout, create everything we need anew
            f.mkdirs();
            e.mkdir();
        }

        return true;
    }

    /**
     * Creates a CommitList object for this repository.
     *
     * @return a CommitList, containing all the commits of the repository
     */
    public abstract VersionList getCommitList() throws IOException;

    /**
     * Finalizer-method, is called after extraction. Implementing classes can attempt to
     * undo any changes they made to the repository during the mining operation.
     */

    protected abstract void cleanUpRepo() throws IOException;

    /**
     * Extracts a version from the repository this Miner wraps around and copies
     * the entire content of the repository to the extracted-folder of the version.
     *
     * @param commitByIdentifier : version to extract
     */
    protected abstract void checkoutVersion(Version commitByIdentifier) throws IllegalArgumentException, IOException;

    /**
     * Computes the changes between two given versions on file level. As a side effect,
     * this method persists all file-level changes on the hard-drive!
     *
     * @param version1 : the first commit
     * @param version2 : the second commit
     * @return a list of file-level changes that occurred on source code files
     */
    protected abstract List<FileLevelChange> computeFileLevelChanges(Version version1, Version version2) throws IOException;


    /**
     * Factory-method, constructs a appropriate Miner for an existing repository on the local hard drive.
     *
     * @param repoDir : the repository directory
     * @param branchName : the branch to checkout, may be ignored depending on the type of repository
     * @return an appropriate miner to extract data from the repository
     * @throws IllegalArgumentException : if the repository type is not supported
     */
    public static Miner createMinerForExistingRepo(File repoDir, String branchName) throws IllegalArgumentException, IOException {
        File dotGitFolder = new File(repoDir.getAbsolutePath()+"/.git");
        if (dotGitFolder.exists() && dotGitFolder.isDirectory()){
            if (branchName.isEmpty()){
                Tool.printExtraInfo(" * NOTE: No branch specified for repository, using default value 'master'!");
                branchName = "master"; //default value for git!
            }
            return new GitMiner(repoDir, branchName);
        }
        File dotSvnFolder = new File(repoDir.getAbsolutePath()+"/.svn");
        if (dotSvnFolder.exists() && dotSvnFolder.isDirectory()){

            if (!branchName.isEmpty())
                Tool.printError("Different branches are currently unsupported for SVN, instruction to work on branch '"+branchName+"' will be ignored!");
            return new SVNMiner(repoDir);
        }

        String s = "Given directory '"+repoDir.getAbsolutePath()+"' is not a repository of any supported type!";
        Tool.printError(s);
        throw new IllegalArgumentException(s);
    }



    /**
     * Factory-method, constructs a appropriate Miner for an online repository not on the local hard drive.
     *
     * @param folterToStoreRepoIn : the directory where to store the downloaded data
     * @param linkToOnlineRepostiory : where to download the repo, can be a link/URL/address/...
     * @param branchName : the branch to checkout, may be ignored depending on the type of repository
     * @return an appropriate miner to extract data from
     *         the repository (repo is  downloaded first)
     * @throws IllegalArgumentException : if the repository type is not supported
     */
    public static Miner createMinerForOnlineRepo(File folterToStoreRepoIn, String linkToOnlineRepostiory, String branchName) throws IllegalArgumentException, IOException {
        boolean svn = false;
        boolean git = false;
        if (linkToOnlineRepostiory.startsWith("!git!")) {
            linkToOnlineRepostiory = linkToOnlineRepostiory.substring(5); //cut off "!git!"
            git = true;
        }
        else if (linkToOnlineRepostiory.startsWith("!svn!")) {
            linkToOnlineRepostiory = linkToOnlineRepostiory.substring(5); //cut off "!svn!"
            svn = true;
        }


        /** stupid but usually working auto-detection */
        if (!svn && !git){
            git = linkToOnlineRepostiory.contains("git");
            svn = linkToOnlineRepostiory.contains("svn");
            String s = " * NOTE: It seems like '"+linkToOnlineRepostiory+"' is ";
            if (svn && !git) s+= "an SVN repository";
            else if (git && !svn) s+= "an Git repository";
            else {
                //could not match this!
                s = "Type of repository at '"+linkToOnlineRepostiory+"' cannot be determined!" +
                    "Add prefix '!git!' or '!svn!' to your link to force a VCS or checkout/clone the repository " +
                    "manually and use the '"+Commands.pathToLocalRepo+"' command to provide the location!";
                Tool.printError(s);
                throw new IllegalArgumentException(s);
            }
            //could be matched
            Tool.print(s);
        }


        assert ((git || svn) && git != svn);

        /** create the miner */
        if (git){
            if (branchName.isEmpty()) branchName = "master"; //default value for git!
            return new GitMiner(folterToStoreRepoIn,linkToOnlineRepostiory,branchName);
        }
        else{
            if (!branchName.isEmpty())
                Tool.printError("Different branches are currently unsupported for SVN, instruction to work on branch '"+branchName+"' will be ignored!");
            return new SVNMiner(folterToStoreRepoIn, linkToOnlineRepostiory);
        }
    }

    protected static boolean isPartOfSourceCode(String relativePathToClass, Version v){
        return relativePathToClass.toLowerCase().endsWith(".java") &&
                FileUtils.isSubDirectoryOrFile(v.getSourcesDir(),
                        new File(v.getActualExtractedRepo()+"/"+relativePathToClass));
    }

    public abstract String getBranchName();
}
