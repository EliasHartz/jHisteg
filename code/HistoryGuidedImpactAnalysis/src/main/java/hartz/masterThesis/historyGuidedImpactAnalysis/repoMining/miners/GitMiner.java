package hartz.masterThesis.historyGuidedImpactAnalysis.repoMining.miners;

import hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution.ExecutionResult;
import hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution.Executor;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileDiffType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileLevelChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.VersionList;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.OutputType;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.repoMining.Miner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Implementation of Miner, class containing methods to mine a given GIT repository.
 */
public class GitMiner extends Miner{

    private final File gitRepoDirectory;
    private final String branch;

    /**
     * Creates a GitMiner for an existing and functional repository on
     * the local hard drive.
     *
     * @param repoDir : the GIT repository's main directory
     * @param branch : the branch to checkout
     */
    public GitMiner(File repoDir, String branch) throws IOException {
        this.gitRepoDirectory = repoDir;
        this.branch = branch;

        //verify that directory is a GIT repo
        File dotGitFolder = new File(repoDir.getAbsolutePath()+"/.git");
        if ((!dotGitFolder.exists() || !dotGitFolder.isDirectory())){
            Tool.printError("Directory '"+repoDir.getAbsolutePath()+"' is not a GIT repository (.git folder is missing)");
        }
        Tool.print(" * found GIT repository at '"+repoDir.getAbsolutePath()+"'");

        Executor.execute(new String[]{"git", "checkout", branch},repoDir); //make sure we are at the right branch
    }

    /**
     * Creates a GitMiner that will clone a repository's 'master' branch.
     *
     * @param gitURL : the URL to use for the cloning operation
     * @param repoDir : the directory into which the repository should be cloned
     */

    public GitMiner(String gitURL, File repoDir) throws IOException {
        this(repoDir,gitURL,"master");
    }

    /**
     * Creates a GitMiner that will clone a repository's given branch.
     *
     * @param dirToCloneTo : the directory into which the repository should be cloned
     * @param gitURL : the URL to use for the cloning operation
     * @param branch : the branch to checkout
     *
     * @throws IllegalArgumentException : in case the Git-Clone fails
     */
    public GitMiner(File dirToCloneTo, String gitURL, String branch) throws IllegalArgumentException, IOException {
        this.gitRepoDirectory = dirToCloneTo;
        this.branch = "master";

        Tool.print(" * cloning GIT repository into '"+dirToCloneTo.getAbsolutePath()+"', THIS MAY TAKE A WHILE!");
        String[] clone = new String[]{"git", "clone","-b",branch,"-q",gitURL,dirToCloneTo.getAbsolutePath()};
        ExecutionResult result = Executor.execute(clone);
        if (result.hadErrors()){
            String s = "Git-Clone operation reported errors!";
            Tool.printError(s);
            Tool.printError(result.errors.getContent());
            Tool.printError("Command '"+Commands.toolOut+"' may be useful...");
            throw new IllegalArgumentException(result.errors.getContent());
        }
    }

    @Override
    protected List<FileLevelChange> computeFileLevelChanges(Version version1, Version version2) throws IOException {
        try{
            //compute the changes:
            List<FileLevelChange> fileLevelChanges;
            Tool.printToolOutput("   -------------- output by 'git diff' ------------------------------", OutputType.OUTPUT);
            ExecutionResult resultDiff = Executor.execute("git diff --name-status "+version1.identifier+
                    " "+version2.identifier,gitRepoDirectory);
            Tool.printToolOutput("   -------------- end of output by 'git diff' ----------------------", OutputType.OUTPUT);


            if (resultDiff.hadErrors()){
                //verify that operation succeeded, if diff-command failed then the arguments of the entire program are invalid!
                String s = "File level change extraction failed, please verify program arguments!";
                Tool.printError(s+" Command '"+Commands.toolOut+"' may be useful...");
                throw new IllegalArgumentException(s);
            }

            //parse output of the above GIT DIFF statement:
            String[] lines = resultDiff.output.getContent().split("\n");
            fileLevelChanges = new ArrayList<>(lines.length);
            for (int i = 0; i<lines.length; ++i){
                String lineInQuestion = lines[i];
                if (lineInQuestion.isEmpty()) continue;
                String pathInRepo = FileUtils.standardizeRelativePaths(lineInQuestion.substring(lineInQuestion.indexOf("\t")+1).trim());

                int type = lineInQuestion.indexOf(">")+2; //cut off the '...OUTPUT:> '
                switch (lineInQuestion.substring(type,type+1)){
                    case "A" /** added   */  : fileLevelChanges.add(
                            new FileLevelChange(FileDiffType.ADDED, isPartOfSourceCode(pathInRepo,version2), pathInRepo, null)); break;
                    case "D" /** deleted */  : fileLevelChanges.add(
                            new FileLevelChange(FileDiffType.DELETED,
                                    isPartOfSourceCode(pathInRepo,version2) /* use of version 2 might seem counter-intuitive but it is
                                    * the only way of obtaining a reliable source/not-source metric since the version from which this was
                                    * deleted MAY NOT HAVE BEEN EXTRACTED! Therefore, part-of-source models if the file would have been part
                                    *  of the source in the current version, even if the file might not even be there. */, null, pathInRepo));
                        break;
                    case "C" /** copied */  :  //fallthrough
                    case "T" /** typechnge */:  //fallthrough
                    case "M" /** merged  */  : fileLevelChanges.add(
                            new FileLevelChange(FileDiffType.MODIFIED, isPartOfSourceCode(pathInRepo,version2), pathInRepo, null)); break;
                    case "R" /** renamed */  :  fileLevelChanges.add(
                            new FileLevelChange(FileDiffType.RENAMED, isPartOfSourceCode(pathInRepo,version2), pathInRepo, null)); break;
                    default /** X:unknown, U:unmerged, B:brokenpairing */ : fileLevelChanges.add(new FileLevelChange(FileDiffType.UNKNOWN, false, pathInRepo, null)); break;
                }
            }

            //export changes:
            persistAndFilterFileLevelChanges(version2, fileLevelChanges);
            return fileLevelChanges;
        }
        catch (IOException e){
            Tool.printError("Failed to export file level changes, IOException occurred:");
            Tool.printDebug(e);
            throw e;
        }
    }

    @Override
    protected void checkoutVersion(Version version) throws IllegalArgumentException, IOException{
        File folder = version.getActualExtractedRepo();
        assert(folder.exists() && folder.list().length==0); //must exist from previous operation but be empty!

        ExecutionResult result = Executor.execute(
                new String[]{"git", "checkout","--detach", "-q", version.identifier},
                gitRepoDirectory);
        if (result.hadErrors()){
            String s = result.errors.getContent();
            Tool.printError(s);
            throw new IllegalArgumentException(s);
        }
        try {
            FileUtils.copyAllButSomeDirectoresFromAtoB(gitRepoDirectory, folder, ".git"); //copy into the corresponding folder
        } catch (IOException e) {
            Tool.printError("Failed to extract git commit "+version+" to '"+folder.getAbsolutePath()+"'");
            Tool.printDebug(e);
        }
    }

    @Override
    protected void cleanUpRepo() throws IOException {
        Executor.execute(new String[]{"git", "checkout", branch, "-q"},gitRepoDirectory); //return from detached head
    }

    @Override
    public VersionList getCommitList() throws IOException {
        //use formatted "git log" to get a nicely readable format
        String[] getLog = new String[]{"git", "log", "--pretty=format:{%H}\u0020%an\u0020[%ad]\u0020%s\u0020"};

        Tool.printToolOutput("   -------------- output by 'git log' -------------------------------", OutputType.OUTPUT);
        ExecutionResult result = Executor.execute(getLog,gitRepoDirectory);
        Tool.printToolOutput("   -------------- end of output by 'git log' ------------------------", OutputType.OUTPUT);

        ArrayList<Version> commits = new ArrayList<>(25);
        HashMap<String, Version> hashToCommit = new HashMap<>();

        try{
            //transform into more easily parse-able version
            String[] tmp = result.output.getContent().split("\n");
            ArrayList<String> l = new ArrayList<>();
            for (String s : tmp){
                l.add(s.substring(s.indexOf("{")));
            }
            Collections.reverse(l); //first commit, first element!
            String[] data = l.toArray(new String[l.size()]);

            int i = 0;
            for (String entry : data){
                //this simple parsing method works as long as the commit message is always the last entry
                String hash = entry.substring(entry.indexOf("{")+1,entry.indexOf("}") );
                String author = entry.substring(entry.indexOf("}")+1, entry.indexOf("[") ).trim();
                String date = entry.substring(entry.indexOf("[")+1,entry.indexOf("]"));
                String msg = entry.substring(entry.indexOf("]")+1).trim();
                Version c = new Version(i++, hash, author, date, msg);
                commits.add(c);
                hashToCommit.put(hash,c);
            }
        } catch (IndexOutOfBoundsException | NumberFormatException e){
            throw new IOException("Command 'git log' returned its results in an unexpected format. Cannot automatically mine this repository!");
        }
        return new VersionList(commits, hashToCommit);
    }

    @Override
    public String getBranchName() {
        return branch;
    }
}
