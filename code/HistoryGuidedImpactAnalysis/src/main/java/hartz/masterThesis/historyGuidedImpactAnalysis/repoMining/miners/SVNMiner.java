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

/*
 * EXPERIMENTAL!!! And of course it is slower, since SVN always relies on some online server...
 */

/**
 * Implementation of Miner, class containing methods to mine a given SVN repository.
 */
public class SVNMiner extends Miner{

    private final File svnDirectory;

    /**
     * Creates a SVNMiner for an existing and functional repository on
     * the local hard driver.
     *
     * @param repoDir : the SVN repository's main directory
     */
    public SVNMiner(File repoDir){
        this.svnDirectory = repoDir;

        //verify that directory is a SVN repo
        File dotGitFilder = new File(repoDir.getAbsolutePath()+"/.svn");
        if ((!dotGitFilder.exists() || !dotGitFilder.isDirectory())){
            Tool.printError("Directory '"+repoDir.getAbsolutePath()+"' is not an SVN repository (.svn folder is missing)");
        }
        Tool.print(" * found SVN repository at '"+repoDir.getAbsolutePath()+"'");
    }

    /**
     * Creates a SVNMiner that will checkout a repository's currently active branch.
     *
     * @param toCheckoutTo : the directory into which the repository should be checked out
     * @param svnURL : the URL to use for the checkout operation
     *
     * @throws java.lang.IllegalArgumentException : in case SVN-checkout fails
     */
    public SVNMiner(File toCheckoutTo, String svnURL) throws IllegalArgumentException, IOException {
        this.svnDirectory = toCheckoutTo;

        Tool.print(" * checking out SVN repository into '"+toCheckoutTo.getAbsolutePath()+"', THIS MAY TAKE A WHILE!");
        String[] checkout = new String[]{"svn", "checkout",svnURL,toCheckoutTo.getAbsolutePath()};

        ExecutionResult result = Executor.execute(checkout);
        if (result.hadErrors()){
            String s = "SVN-checkout operation reported errors!";
            Tool.printError(s);
            Tool.printError(result.errors.getContent());
            Tool.printError("Command '"+Commands.toolOut+"' may be useful...");
            throw new IllegalArgumentException(result.errors.getContent());
        }
    }

    @Override
    protected List<FileLevelChange> computeFileLevelChanges(Version version1, Version version2) throws IllegalArgumentException, IOException {
        try{
            File changesFile = new File (version2.getMainDirectory().getAbsolutePath()+"/allFileLevelChanges");
            assert(!changesFile.exists()); //must not exist yet, otherwise this call is useless

            //compute the changes:
            List<FileLevelChange> fileLevelChanges;
            Tool.printToolOutput("   -------------- output by 'svn log' -------------------------------", OutputType.OUTPUT);
            ExecutionResult resultDiff = Executor.execute(new String[]{"svn", "log", "-v", "-r",version2.identifier},svnDirectory);
            Tool.printToolOutput("   -------------- end of output by 'svn log' ------------------------", OutputType.OUTPUT);


            if (resultDiff.hadErrors()){
                //verify that operation succeeded, if diff-command failed then the arguments of the entire program are invalid!
                String s = "Revision extraction failed, please verify program arguments!";
                Tool.printError(s+"Command '"+Commands.toolOut+"' may be useful...");
                throw new IllegalArgumentException(s);
            }

            String[] lines = resultDiff.output.getContent().split("\n");
            fileLevelChanges = new ArrayList<>(lines.length);

            //parse output of the above SVN LOG statement:
            boolean reachedChangesPaths = false;
            for (int i = 0; i<lines.length; ++i){
                String lineInQuestion = lines[i].substring(lines[i].indexOf(">")+1); //cut off the 'EXTERNAL_OUTPUT:> '
                if (!reachedChangesPaths && !lineInQuestion.toLowerCase().startsWith("changed paths:"))
                    continue;

                if (reachedChangesPaths){
                    if (lineInQuestion.trim().isEmpty())
                        break; //last change was processed, exit loop

                    String[] data = lineInQuestion.split(" ");
                    int j = 0;
                    while(data[j].trim().isEmpty()) ++j; //advance to type of change

                    /**
                     * SVN status encodings:
                     * A = Added
                     * D = Deleted
                     * U = Updated
                     * C = Conflict
                     * G = Merged
                     * E = Existed
                     * R = Replaced
                     */
                    String status = data[j].trim();

                    while(data[j].trim().isEmpty()) ++j; //advance to changed path

                    String path = data[j].trim();
                    if(path.startsWith("/")){
                        //TODO: is this really a good way to deal with branches?
                        path = path.substring(path.substring(1).indexOf("/")+1); //cut off the branch, e.g. '/trunk/
                    }
                    //transform to relative path inside repository
                    path = FileUtils.standardizeRelativePaths(path);

                    switch (status){
                        case "A" : fileLevelChanges.add(new FileLevelChange(FileDiffType.ADDED, isPartOfSourceCode(path, version2), path, null)); break;
                        case "D" : fileLevelChanges.add(


                        new FileLevelChange(FileDiffType.DELETED,
                                isPartOfSourceCode(path,version2) /* use of version2 might seem counter-intuitive but it is the only way
                                    * of obtaining a reliable source/not-source metric since the version from which this was deleted MAY
                                    * NOT HAVE BEEN EXTRACTED! Therefore, part-of-source models if the file would have been part of the
                                    * source in the current version, even if the file might not even be there. */, null, path)); break;

                        case "M" : //fallthrough
                        case "U" : //fallthrough
                        case "R" : fileLevelChanges.add(new FileLevelChange(FileDiffType.MODIFIED, isPartOfSourceCode(path, version2), path, null)); break;

                        default : fileLevelChanges.add(new FileLevelChange(FileDiffType.UNKNOWN, false, path, null)); break;
                    }
                }
                else reachedChangesPaths = true;

            }

            //export changes:
            persistAndFilterFileLevelChanges(version2, fileLevelChanges);
            return fileLevelChanges;
        }
        catch (IOException e){
            Tool.printError("Failed to store file level changes to file, an IOException occurred!");
            Tool.printDebug(e);
            throw e;
        }
    }


    @Override
    protected void checkoutVersion(Version revision) throws IllegalArgumentException, IOException{
        File folder = revision.getActualExtractedRepo();
        assert(folder.exists() && folder.list().length==0); //must exist from previous operation but be empty!

        ExecutionResult result = Executor.execute(new String[]{"svn", "up", "--depth=infinity","-r",revision.identifier},svnDirectory);
        if (result.hadErrors()){
            String s = result.errors.getContent();
            Tool.printError("SVN checkout failed, errors reported were: "+s);
            throw new IllegalArgumentException(s);
        }
        try {
            FileUtils.copyAllButSomeDirectoresFromAtoB(svnDirectory, folder, ".svn");//copy into version folder
        } catch (IOException e) {
            Tool.printError("Failed to extract revision "+revision+" to '"+folder.getAbsolutePath()+"'");
            Tool.printDebug(e);
        }
    }


    @Override
    public VersionList getCommitList() throws IOException {
        String[] getLog = new String[]{"svn", "log"};

        Tool.printToolOutput("   -------------- output by 'svn log' -------------------------------", OutputType.OUTPUT);
        ExecutionResult result = Executor.execute(getLog,svnDirectory);
        Tool.printToolOutput("   -------------- end of output by 'svn log' ------------------------", OutputType.OUTPUT);

        //transform into more easily parse-able version
        String[] tmp = result.output.getContent().split("\n");
        ArrayList<String> l = new ArrayList<>();
        for (String s : tmp){
            l.add(s.substring(s.indexOf(">")+1));//cut off "EXTERNAL_OUTPUT:>"
        }

        String author = null;
        String date = null;
        String msg = null;
        int revision = -1;

        ArrayList<Version> commits = new ArrayList<>();
        HashMap<String,Version> commitsMap = new HashMap<>();

        try {
            /**
             * Example SVN log data (since they do not support setting a format, I am screwed if they change it):
             *
             * ------------------------------------------------------------------------
             *  r4 | eliash | 2011-06-01 22:10:47 +0200 (Mi, 01 Jun 2011) | 6 lines
             *
             *  - getText() -> getPassword().toString() - security reasons and getText() is now deprecated
             *  - rezising of interface blabla...
             */

            for (int i = 0; i < l.size(); ++i) {
                String line = l.get(i).trim();

                if (line.startsWith("-------------------------------------------------------")) {
                    if (author != null) {
                        //store last commit:
                        Version c = new Version(revision - 1 /*index starts with 0 */, Integer.toString(revision), author, date, msg);
                        commits.add(c);
                        commitsMap.put(c.identifier, c);//identifier is revision number, aka index+1
                    }
                    //reset stored fields
                    author = null;
                    date = null;
                    msg = "";
                    continue;
                } else if (author == null) {
                    if (line.trim().isEmpty()) continue;

                    //read in data fields
                    revision = Integer.parseInt(line.substring(0, line.indexOf("|")).trim().substring(1));
                    line = line.substring(line.indexOf("|") + 1);

                    author = line.substring(0, line.indexOf("|")).trim();
                    line = line.substring(line.indexOf("|") + 1);

                    date = line.substring(0, line.indexOf("|")).trim();
                } else {
                    if (line.trim().isEmpty()) continue;
                    msg += line;
                }
            }
            Collections.reverse(commits); //first commit, first element!
        } catch (IndexOutOfBoundsException | NumberFormatException e){
            throw new IOException("Command 'svn log' returned its results in an unexpected format. Cannot automatically mine this repository!");
        }
        return new VersionList(commits,commitsMap);
    }

    @Override
    protected void cleanUpRepo() throws IOException {
        Executor.execute(new String[]{"svn", "up"},svnDirectory); //return to most recent version
    }

    @Override
    public String getBranchName(){
        return "trunk";
    }
}
