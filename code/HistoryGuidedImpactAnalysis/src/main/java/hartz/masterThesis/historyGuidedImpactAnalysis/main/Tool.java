package hartz.masterThesis.historyGuidedImpactAnalysis.main;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configurable;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.Configuration;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Behavior;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.targets.TestingTarget;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.VersionList;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.repoMining.Miner;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains the main method and encapsulates output. Also constructs configuration-objects from the command line.
 */

public class Tool {

    private static boolean moreOutput = false;
    private static boolean toolOutput = false;
    private static boolean allowOutput = true;
    private static boolean debugOutput = false;

    //user-friendly features
    private static boolean gaveTipAboutListNotation = false;

    /**
     * Start of the program.
     *
     * @param args : see output of Commands.showHelp()
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0){
            Commands.showHelp(System.out);
            return;
        }
        for (String s : args){
            if (s.equals("--help") || s.equals("-help") || s.equals("--usage") || s.equals("-usage")){
                Commands.showHelp(System.out);
                return;
            }
        }


        Configuration configuration = null;
        try{
            configuration = makeConfigAndPrepareData(args);
        }
        catch(Exception e){
            Tool.printError(e.getMessage());
            return; //get out!
        }

        main(configuration);
        print(" * exiting program...");
    }

    public static void main (Configuration config) throws IOException {
        if (config.getVersionIdentifiers().length==0){
            print("\n\nTo fully run the tool on versions from that repository, use the '"+Commands.versions+"' argument!");
            print("For this execution, no versions to mine and analyze have been defined. Exiting...\n\n");
        }
        else new MainFunctionality(config).execute(); //versions are defined, we can start extracting!

        if (config.isRepoToBeDeletedAtTheEnd()){
            FileUtils.removeDirectory(config.getRepoDir());
            print(" * deleted temporary directory of repository");
        }
    }

    public static Configuration makeConfigAndPrepareData(String[] args) throws IOException, IllegalArgumentException{
        String nameOfProject = "unnamedRepo___use_"+Commands.nameOfRepo.substring(1)+
                "_command_to_configure_name";

        assert(args != null && args.length > 0);

        //repository variables
        File repoDir = null;
        String repoURL = null;
        String branchName = "";
        boolean keep = false;
        boolean allVersions = false;
        boolean overwrite = false;

        //execution variables
        Configurable config = null;
        boolean versionsDefined = false;
        boolean noRepo = false;

        //##################### PROCESS COMMAND LINE FLAGS  ######################################
        try{
            for (int i = 0; i<args.length; ++i){
                if (i==0) switch(args[0]){
                    case Commands.extractOnly :{
                        config = new Configurable(Behavior.EXTRACT);
                        break;
                    }
                    case Commands.compile :{
                        config = new Configurable(Behavior.COMPILE);
                        break;
                    }
                    case Commands.instrument :{
                        config = new Configurable(Behavior.INSTRUMENT);
                        break;
                    }
                    case Commands.analyze:{
                        config = new Configurable(Behavior.DATA_COMPUTATION);
                        break;
                    }
                    case Commands.compute:{
                        config = new Configurable(Behavior.TARGET_GENERATION);
                        break;
                    }
                    default:{
                        throw new IllegalArgumentException("Unrecognized tool behavior command '"+args[0]+"', use '--help' to access the documentation!\n");
                    }
                }
                else{ //argument number 1 and onwards!
                    if (config == null) throw new IllegalArgumentException("You must specify the desired tool behavior!");

                    switch(args[i]){
                        case Commands.cloneShort :
                        case Commands.clone :{
                            if (repoDir!=null)
                                throw new IllegalArgumentException("'"+Commands.clone+"' command cannot be together with '"+Commands.pathToLocalRepo+"'");
                            repoURL = args[++i]; //store repo url

                            break;
                        }
                        case Commands.pathToLocalRepoShort :
                        case Commands.pathToLocalRepo :{
                            if (repoURL!=null)
                                throw new IllegalArgumentException("'"+Commands.pathToLocalRepo+"' command cannot be together with '"+Commands.clone+"'");
                            repoDir = new File(args[++i]); //overwrite output directory

                            //verify that directory exists
                            if (!repoDir.exists() || !repoDir.isDirectory())
                                throw new IllegalArgumentException("Repository directory '"+repoDir.getAbsolutePath()+"' does not exist or cannot be accessed");
                            break;
                        }
                        case Commands.outputFolderShort :
                        case Commands.outputFolder :{
                            File outputDir = new File(args[++i]);
                            if (outputDir.exists() && !outputDir.isDirectory())
                                throw new IllegalArgumentException("Designated output location '"+outputDir.getAbsolutePath()+"' is a file and not a directory");
                            config.setOutputDirectory(outputDir);
                            break;
                        }
                        case Commands.versionsShort :
                        case Commands.versions :{
                            if (versionsDefined)
                                throw new IllegalArgumentException("You cannot use '"+Commands.allVersions+"' and '"+Commands.versions+"' at the same time!");

                            ArrayList<String> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAList(args,i,v);
                            if (v.isEmpty())
                                throw new IllegalArgumentException("Invalid value(s) given for '"+Commands.versions+"', please correct this!");
                            config.setVersionsToMine(v.toArray(new String[v.size()]));
                            versionsDefined = true;
                            break;
                        }
                        case Commands.allVersions:{
                            if (versionsDefined)
                                throw new IllegalArgumentException("You cannot use '"+Commands.versions+"' and '"+Commands.allVersions+"' at the same time!");
                            allVersions = true;
                            versionsDefined = true;
                            break;
                        }
                        case Commands.nameOfRepoShort :
                        case Commands.nameOfRepo :{
                            nameOfProject = args[++i].replace(" ","_");
                            break;
                        }
                        case Commands.branchToWorkOn :{
                            branchName = args[++i];
                            break;
                        }
                        case Commands.relativePathToSourcesShort :
                        case Commands.relativePathToSources :{
                            ArrayList<String> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAList(args, i, v);
                            if (v.isEmpty())
                                throw new IllegalArgumentException("Invalid value given for '"+Commands.relativePathToSources+"'");
                            config.setRelativeLocationOfSources(v.toArray(new String[v.size()]));
                            break;
                        }
                        case Commands.relativePathToCompiledSourcesShort :
                        case Commands.relativePathToCompiledSources :{
                            ArrayList<String> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAList(args, i, v);
                            if (v.isEmpty())
                                throw new IllegalArgumentException("Invalid value given for '"+Commands.relativePathToCompiledSources+"'");
                            config.setRelativeLocationOfCompiledSources(v.toArray(new String[v.size()]));
                            break;
                        }
                        case Commands.relativePathToAdditionalCompiledSources :{
                            ArrayList<String> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAList(args,i,v);
                            if (v.isEmpty())
                                throw new IllegalArgumentException("Invalid value given for '"+Commands.relativePathToAdditionalCompiledSources+"'");
                            config.setRelativeLocationOfAdditionalCompiledSources(v.toArray(new String[v.size()]));
                            break;
                        }
                        case Commands.keepRepoShort :
                        case Commands.keepRepo :{
                            keep = true;
                            break;
                        }
                        case Commands.buildCommandShort :
                        case Commands.buildCommand :{
                            ArrayList<String> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAList(args,i,v);
                            if (v.isEmpty())
                                throw new IllegalArgumentException("Invalid value given for '"+Commands.buildCommand+"'");
                            config.setBuildString(v.toArray(new String[v.size()]));
                            break;
                        }
                        case Commands.buildFileShort :
                        case Commands.buildSpot:{
                            ArrayList<String> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAList(args,i,v);
                            if (v.isEmpty())
                                throw new IllegalArgumentException("Invalid value for '"+Commands.buildSpot +"'");
                            config.setRelativePathToBuildFile(v.toArray(new String[v.size()]));
                            break;
                        }
                        case Commands.forceOverwriteShort2 :
                        case Commands.forceOverwrite2 :
                        case Commands.forceOverwriteShort :
                        case Commands.forceOverwrite :{
                            overwrite = true;
                            config.enableOverwrite();
                            break;
                        }
                        case Commands.coverRenamingShort :
                        case Commands.coverRenaming :{
                            config.enableRenamedCodeCoverage();
                            break;
                        }
                        case Commands.ignorePrefixShort :
                        case Commands.ignorePrefix :{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setIgnoreByPrefix(s);
                            break;
                        }
                        case Commands.ignoreSuffixShort :
                        case Commands.ignoreSuffix :{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setIgnoreBySuffix(s);
                            break;
                        }
                        case Commands.excludeMethodFromInstrumentationShort:
                        case Commands.excludeMethodFromInstrumentation:{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setExcludeMethodsFromInstrumentationBySignature(s);
                            break;
                        }
                        case Commands.excludeDirFromInstrumentationShort:
                        case Commands.excludeDirFromInstrumentation:{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setDirectoriesExcludedFromInstrumentation(s);
                            break;
                        }
                        case Commands.testsDir:
                        case Commands.testsDirShort:{
                            ArrayList<String> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAList(args,i,v);
                            if (v.isEmpty())
                                throw new IllegalArgumentException("Invalid value given for '"+Commands.testsDir +"'");
                            config.setRelativePathToTests(v.toArray(new String[v.size()]));
                            break;
                        }
                        case Commands.executeTestsShort:
                        case Commands.executeTests:{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setTestsToRun(s);
                            break;
                        }
                        case Commands.skipTests:{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setTestsToSkip(s);
                            break;
                        }
                        case Commands.executeMain:
                        case Commands.executeMainShort:{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setExecuteMainClassnames(s);
                            break;
                        }

                        case Commands.executeMainArgs:
                        case Commands.executeMainArgsShort:{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setExecuteMainArguments(s);
                            break;
                        }

                        case Commands.runCommand:
                        case Commands.runCommandShort:{
                            ArrayList<List<String>> v = new ArrayList<>();
                            i = readArgumentThatIsPotentiallyAListOfLists(args,i,v);
                            String[][] s = convertToArrayOfArrays(v);
                            config.setRunCommands(s);
                            break;
                        }
                        case Commands.keepTracesShort :
                        case Commands.keepTraces :{
                            config.disableTraceDeletion();
                            break;
                        }
                        case Commands.coverageBasedTraces :{
                            config.enableCoverageBasedTraces();
                            break;
                        }
                        case Commands.distanceBasedTraces :{
                            config.enableDistanceBasedTraces();
                            break;
                        }
                        case Commands.loud :{
                            moreOutput = true;
                            TestingTarget.enableNotes();
                            break;
                        }
                        case Commands.debug :{
                            System.out.println(" --- DEBUG MODE ACTIVE ---");
                            debugOutput = true;
                            break;
                        }
                        case Commands.quiet :{
                            allowOutput = false;
                            break;
                        }
                        case Commands.toolOut :{
                            toolOutput = true;
                            break;
                        }
                        case Commands.filterObjectCharacteristics :{
                            config.enableFilteringOfObjectCharacteristics();
                            break;
                        }
                        case Commands.compatibleInstr:{
                            config.enableInstrumentationCompatibiltyMode();
                            break;
                        }
                        case Commands.doNotExportSource :{
                            config.disableSourceExport();
                            break;
                        }
                        case Commands.flatDivergences :{
                            config.exportTraceDivergencesInFlatFormat();
                            break;
                        }
                        case Commands.cleanBeforeCompile :{
                            config.deleteCompiledSourceBeforeCompilation();
                            break;
                        }
                        case Commands.generateTargetsForDivergences :{
                            config.enableDivergenceTargetGeneration();
                            break;
                        }
                        case Commands.useManualTraceMatchFile : {
                            config.useManualTraceMatchingFile();
                            break;
                        }
                        case Commands.generateTargetsForSyntax :{
                            config.enableSyntaxTargetGeneration();
                            break;
                        }
                        case Commands.injectTwice :{
                            config.injectCoverageObserverTwice();
                            break;
                        }
                        case Commands.noTargetDetails:{
                            TestingTarget.disableDetailedOutput();
                            break;
                        }
                        case Commands.restrictSyntaxMapping :{
                            config.restrictMappingOfDivergencesToFirstSyntaxChange();
                            break;
                        }
                        case Commands.skipVersionCheck:{
                            Globals.performMachineDependentVersionChecks = false;
                            break;
                        }
                        case Commands.noRepo :{
                            noRepo = true;
                            config.enableNoRepoMode();
                            break;
                        }
                        default :
                            throw new IllegalArgumentException("Unrecognized parameter/flag '"+args[i]+"'\n\n");
                    }
                } //end of ELSE
            }//end of FOR
        }
        catch (ArrayIndexOutOfBoundsException e){
            throw new IllegalArgumentException("Invalid arguments, please consult the documentation via the 'help'-command!\n\n");
        }

        //##################### CHECK THAT WE HAVE A REPO  #################################################
        if( !noRepo && ((repoDir ==null || !repoDir.exists()) && (repoURL == null || repoURL.isEmpty())))
            throw new IllegalArgumentException("Repository must be provided, either '"+Commands.pathToLocalRepo+"' or '"+Commands.clone+"' must be used!");


        //##################### CREATE OUTPUT DIRECTORY IF NECESSARY  ######################################
        File outputDir = config.getOutputDirectory();
        if (!outputDir.exists()){
            print(" * creating a new output directory");
            if (!outputDir.mkdirs())
                throw new IllegalArgumentException("Creation of output directory '"+outputDir.getAbsolutePath()+"' failed due to unknown reasons, aborting!");
        } else {
            for (File f : outputDir.listFiles())
                if (f.isFile() && f.getName().startsWith("history_of_") && !f.getName().startsWith("history_of_"+nameOfProject))
                    throw new IllegalArgumentException("Selected output directory '"+outputDir.getAbsolutePath()+"' was already used while working on a different project, aborting!");
        }


        //##################### CREATE THE REPOSITORY MINER  ###############################################
        Miner miner = null;
        VersionList list = null;
        if (!noRepo) {
            if (repoURL != null) {
                //this means the repository has to be cloned!
                if (keep)
                    //NOTE: we made sure above that the output directory exists!
                    repoDir = new File(outputDir.getAbsoluteFile() + "/" + nameOfProject);
                else {
                    repoDir = Files.createTempDirectory("tmpRepo" + System.currentTimeMillis()).toFile();
                    print(" * NOTE: Using temporary directory for repository!");
                    config.deleteRepoAtTheEnd();
                }
                miner = Miner.createMinerForOnlineRepo(repoDir, repoURL, branchName);
            } else {//this means the repository has been checked-out/cloned and is somewhere on the disc
                if (!(repoDir.exists() && repoDir.isDirectory()))
                    throw new IllegalArgumentException("Local repository directory '" + repoDir.getAbsolutePath() + "' does not exist!");
                miner = Miner.createMinerForExistingRepo(repoDir, branchName);
            }
            config.setRepoDir(repoDir);
            config.setRepositoryMiner(miner);

            //##################### EXTRACT VERSIONS AND CREATE HISTORY FILE  ####################################

            File commitHistoryFile = new File(outputDir.getAbsolutePath()+"/"+"history_of_"+nameOfProject);
            if (commitHistoryFile.exists() && commitHistoryFile.isFile() && !overwrite){
                print(" * reading existing version history file");
                list = new VersionList(new JSONArray(FileUtils.readData(commitHistoryFile)));//read data from file
            }
            else{
                if (moreOutput) printExtraInfo(" * creating machine readable Version History File at '"+outputDir.getAbsolutePath()+"'");
                else print(" * creating machine readable Version History File");
                list = miner.getCommitList();//generate commit log file
                list.writeIntoFile(new File(outputDir.getAbsolutePath()+"/"+"history_of_"+nameOfProject));
            }
        } else {
            //##################### IN NO-REPO-MODE, DEAL WITH VERIFICATION OF STUFF  #############################

            print(" * NOTE: '"+Commands.noRepo+"' is used, contents of current output folder are the foundation of computation!)");
            if (!(outputDir.exists() && outputDir.isDirectory()))
                throw new IllegalArgumentException("Output directory exist if '"+ Commands.noRepo+"' is used, aborting!");
            File commitHistoryFile = new File(outputDir.getAbsolutePath()+"/"+"history_of_"+nameOfProject);
            if (commitHistoryFile.exists() && commitHistoryFile.isFile()) {
                print(" * reading existing version history file");
                list = new VersionList(new JSONArray(FileUtils.readData(commitHistoryFile)));//read data from file
            }
            else
                throw new IllegalArgumentException("Output directory must contain history file belonging to project if '"+
                        Commands.noRepo+"' is used, aborting!");
        }

        assert(list != null);
        //Miner may be null...
        config.setAllVersionsOfRepo(list);

        //#####################  HANDLE THE MAGIC 'ALL-VERSIONS'-CMD   ######################################

        if (allVersions){
            String[] arr = new String[list.getAmountOfCommits()];
            int i = 0;
            for (Version c : list){
                arr[i] = c.identifier;
                ++i;
            }
            config.setVersionsToMine(arr);
        }

        //finalizes the config, makes certain default values match!
        return config.finalizeConfig();
    }

    private static String[][] convertToArrayOfArrays(ArrayList<List<String>> v) {
        String[][] s = new String[v.size()][];
        int tmp = 0;
        for (List<String> l : v){
            s[tmp] = l.toArray(new String[l.size()]);
            ++tmp;
        }
        return s;
    }


    private static int readArgumentThatIsPotentiallyAList(String[] args, int indexCounter, List<String> resultBuffer) {
        indexCounter = indexCounter+1; //skip the instruction that caused the call to this, e.g '-versions'

      /* Parse list given as arguments, I do not want to enforce some rigid structure or one argument rule,
       * hence I need this ugly code to support lists in multiple arguments with no assumptions on their
       * structure (like ']' being its own argument)...   Furthermore, I support the convenience feature
       * of wrapping a single argument into a list automatically. */
        boolean listStarted = false;
        boolean gaveWarning = false;
        while(indexCounter<args.length){
            String s = args[indexCounter].trim();
            boolean stop = false;

            String[] entriesByComma = s.split(",");

            for (String byComma : entriesByComma){
                //opening brackets
                if (byComma.startsWith("[")){
                    listStarted = true;
                    byComma = byComma.substring(1).trim(); //cut off first
                }

                //closing brackets
                if (byComma.endsWith("]")){
                    if (!gaveWarning && !listStarted){
                        printError("Detecting suspicious list argument structure, your arguments are likely to be invalid! Continuing anyway...");
                        gaveWarning = true; //we only warn once!
                    }
                    if (listStarted){
                        byComma = byComma.substring(0,byComma.length()-1).trim();//cut off last
                        stop = true;
                    }
                }

                //entries!
                byComma = byComma.trim();
                if (!byComma.isEmpty()) resultBuffer.add(byComma);
            }

            //finally, test if we need to get out of the loop
            if (stop || !listStarted /* was a single argument only */)
                //do not increase the counter, since this will be handled by the outer while loop in the caller!
                break;
            else ++indexCounter; //we have dealt with an argument, increase the counter
        }

        if (!gaveTipAboutListNotation && listStarted && resultBuffer.size() == 1){
            gaveTipAboutListNotation = true;
            print("Did you know: Instead of using the list notation [...] if you are only providing a single element, you can simply provide the element. No brackets are needed.");
            print("              Even if you are working on multiple versions, you can use a single parameter which is then simply used for all versions. Convenience for-the-win!");
        }

        return indexCounter;
    }

    private static int readArgumentThatIsPotentiallyAListOfLists(String[] args, int indexCounter, List<List<String>> resultBuffer) {
        indexCounter = indexCounter+1; //skip the instruction that caused the call to this, e.g '-excludeFromInstr'

      /* Parse list of lists given as arguments, I do not want to enforce some rigid structure or one,
       * argument rule hence I need this ugly code to support lists in multiple arguments with no
       * assumptions on their structure (like ']' being its own argument)...
       * Furthermore, I support the convenience feature of wrapping a single argument or list into a
       * list of lists automatically. */

        ArrayList<String> innerList = null;
        boolean innerListStarted = false;
        boolean outerListStarted = false;
        boolean gaveWarning = false;
        boolean wrapped = false;
        while(indexCounter<args.length){
            String s = args[indexCounter].trim();
            boolean stop = false;

            String[] entriesByComma = s.split(",");

            for (String byComma : entriesByComma) {
                byComma = byComma.trim();

                while (byComma.startsWith("[")){
                    if (!outerListStarted) outerListStarted = true;
                    else if (outerListStarted && !innerListStarted && !wrapped) {
                        assert (innerList == null);
                        innerList = new ArrayList<>(); //create next list
                        innerListStarted = true;
                    } else if (!gaveWarning) {
                        printError("Detecting suspicious list argument structure, your arguments are likely to be invalid! Continuing anyway...");
                        gaveWarning = true; //we only warn once!
                    }
                    byComma = byComma.substring(1).trim(); //cut off first
                }

                //closing brackets
                boolean needToStoreInner = false;
                while (byComma.endsWith("]")) {
                    if (innerListStarted) {
                        innerListStarted = false;
                        needToStoreInner = true;
                    } else if (outerListStarted) stop = true; //we are done!
                    else if (!gaveWarning) {
                        printError("Detecting suspicious list argument structure, your arguments are likely to be invalid! Continuing anyway...");
                        gaveWarning = true; //we only warn once!
                    }

                    byComma = byComma.substring(0, byComma.length() - 1).trim();//cut off last
                }

                byComma = byComma.trim();
                /** Note that we do not check for '[' characters inside the entry now, which could result from a user missing a comma.
                 *  The reason for this is that we might actually encounter valid occurrences of these characters, for example in
                 *  the method descriptor 'main([Ljava/lang/String;)V';
                 */
                if (!byComma.isEmpty()){
                    if (innerList == null /* we are wrapping a list or a single argument into a list of lists! */){
                        assert(!innerListStarted);
                        wrapped = true; //there should obviously not be another list then
                        innerList = new ArrayList<>();
                    }
                    innerList.add(byComma);
                    if (!outerListStarted) stop = true; //its a single argument then!
                }

                if (needToStoreInner){
                    resultBuffer.add(innerList);
                    innerList = null;
                }

            }//end-of-loop

            //finally, test if we need to get out of the loop
            if (stop) {
                if (wrapped /* was a single argument only */)
                    resultBuffer.add(innerList); //wrap single argument in list and store it!

                //do not increase the counter, since this will be handled by the outer while loop in the caller!
                break; //exit loop!

            } else ++indexCounter; //we have dealt with an argument, increase the counter!
        }

        //handle UI tip and '[ ]' case
        if (!gaveTipAboutListNotation && outerListStarted && resultBuffer.size() == 1){
            gaveTipAboutListNotation = true;
            print("Did you know: Instead of using the list notation [...] if you are only providing a single element, you can simply provide the element. No brackets are needed.");
            print("              Even if you are working on multiple versions, you can use a single parameter which is then simply used for all versions. Convenience for-the-win!");
        }
        if (resultBuffer.isEmpty()) //occurs if user supplies an empty outer list '[ ]' only
            resultBuffer.add(new LinkedList<String>());

        return indexCounter;
    }


    /**
     * @param s : what is to be printed
     */

    public static void print(String s){
        if (allowOutput) print(s, OutputType.OUTPUT);
    }

    /**
     * @param s : what is to be printed but only when the program is
     *            supposed to give a lot of feedback
     */

    public static void printExtraInfo(String s){
        if (allowOutput && moreOutput) print(s, OutputType.OUTPUT);
    }

    /**
     * @param s : what is to be printed but only when the program is
     *            supposed to include the output of external tools
     */
    public static void printToolOutput(String s, OutputType t){
        if (allowOutput && toolOutput) print(s,t);
    }

    public static void printError(String s) {
        print("ERROR:> "+s,OutputType.ERROR); //errors are always printed!
    }

    /**
     * @param s : what is to be printed but only when the program is
     *            supposed to include the output of external tools
     */
    private static void print(String s, OutputType t){
//      if (t==OutputType.ERROR) //this is disabled for the moment to obtain a cleaner output!
//         System.err.println(s);
//      else
        System.out.println(s);
    }


    //------------------ SPECIAL DEBUG PRINT METHODS ------------------------------

    public static void printDebug(String s) {
        if (allowOutput && isDebugMode()){
            System.out.println("DEBUG:> "+s);
        }
    }

    public static void printDebug(Throwable t) {
        if (isDebugMode()){
            System.out.println("DEBUG:> ");
            t.printStackTrace();
        }
    }

    public static void activateDebugMode() {
        debugOutput = true;
        toolOutput = true;
        allowOutput = true;
    }

    public static void deactivateDebugMode() {
        debugOutput = true;
        toolOutput = true;
        allowOutput = true;
    }

    public static boolean isDebugMode() {
        return debugOutput;
    }

    public static boolean isExtraInfoEnabled() {
        return moreOutput && allowOutput;
    }

    public static PrintStream redirectSout(PrintStream newStream) {
        if (toolOutput) return null; //ignore request
            PrintStream original = System.out;
            System.setOut(newStream);
            return original;
    }

    public static PrintStream redirectSerr(PrintStream newStream) {
        if (toolOutput) return null; //ignore request
        PrintStream original = System.err;
        System.setErr(newStream);
        return original;
    }
}