package hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution;

import hartz.masterThesis.historyGuidedImpactAnalysis.main.OutputType;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Helper-class to access and execute commands on the outside environment, e.g. to invoke Git.
 */
public class Executor{

    /**
     * DEBUG only!
     *
     * @param args : see output of method showHelp()
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String args[]) throws IOException, InterruptedException{
        //check that arguments are valid
        if (args.length < 1 || args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help")
                || args[0].equalsIgnoreCase("--help") || args[0].equalsIgnoreCase("-usage") || args[0].equalsIgnoreCase("--usage")){
            printHelp();
        }

        Tool.print("NOTE: This is NOT the actual tool but just a way to debug a specific subpart!");
        File executionEnvironment = new File(".");
        ArrayList<String> commands = new ArrayList<>();

        try{
            boolean stillFlagsToParse = true;
            boolean nextIsExDir = false;
            for (String s : args){
                //---------------  handle flags from last iteration  ---------------
                if (nextIsExDir){
                    executionEnvironment = new File(s);
                    if (!executionEnvironment.exists() || !executionEnvironment.isDirectory()){
                        Tool.printError("Given execution directory is not valid");
                        return;
                    }
                    nextIsExDir = false;
                    continue;
                }


                //---------------  parse the arguments for this program  ---------------
                if (stillFlagsToParse && s.startsWith("-")){
                    s = s.substring(s.lastIndexOf("-")+1, s.length());
                    if (s.toLowerCase().startsWith("h")||s.toLowerCase().startsWith("u")){
                        printHelp();
                        return;
                    }
                    else if (s.equals("exDir")) nextIsExDir = true;

                    continue;
                }

                //---------------  handle commands to execute  ---------------
                commands.add(s);
                stillFlagsToParse = false;

            }
        }
        catch (ArrayIndexOutOfBoundsException a){
            printHelp();
            return;
        }
        Tool.activateDebugMode(); //will cause tool output to be printed!
        execute(commands.toArray(new String[commands.size()]), executionEnvironment);
    }

    /**
     * Explains the usage of the command line and exits the executor program afterwards.
     */
    private static void printHelp() {
        Tool.print(
                "'This method executes a given command on your OS command line. Available flags (put before desired command):\n"+
                        "    '-h' (and other associated commands) = Displays this helpful text.'\n"+
                        "    '-exDir {?}' = will execute the given command in the directory denoted by {?}'\n"+
                        "'Example usage: '-exDir ../blub git pull' which attempts to execute git's pull operation a directory 'blub' in the parent directory of the current one.\n");
    }

    /**
     * Executes a command on the OS runtime in the programs current directory (=>".")
     *
     * @param tokens : the individual tokens of the command (e.g. ["git", "checkout", "master"])
     * @return an ExecutionResult object containing the output of the command
     */
    public static ExecutionResult execute(String[] tokens) throws IOException {
        return execute(tokens, new File("."));
    }

    /**
     * Executes a command on the OS runtime in the programs current directory (=>".")
     *
     * @param string : the command to be executed, should not contain any special
     *                 characters like (")
     * @return an ExecutionResult object containing the output of the command
     */
    public static ExecutionResult execute(String string) throws IOException {
        return execute(string, new File("."));
    }

    /**
     * Executes a command on the OS runtime in a given directory.
     *
     * @param string : the command to be executed, should not contain any special
     *                 characters like (")
     * @param executionEnvironment : directory where to execute the command
     * @return an ExecutionResult object containing the output of the command
     */
    public static ExecutionResult execute(String string, File executionEnvironment) throws IOException {
        String[] arr = string.split(" ");
        ArrayList<String> list = new ArrayList<>(arr.length);
        for (String s : arr){
            if (!s.trim().isEmpty()) list.add(s);
        }
        return execute(list.toArray(new String[list.size()]),executionEnvironment);
    }

    /**
     * Executes a command on the OS runtime in a given directory.
     *
     * @param tokens : the individual tokens of the command (e.g. ["git", "checkout", "master"])
     * @param executionEnvironment : directory where to execute the command
     * @return an ExecutionResult object containing the output of the command
     *
     * @throws IOException : if external factors interfere with the execution
     * @throws IllegalArgumentException : if the given execution environment is invalid
     */
    public static ExecutionResult execute(String[] tokens, File executionEnvironment) throws IOException, IllegalArgumentException {
        if (! (executionEnvironment.isDirectory() && executionEnvironment.exists())){
            Tool.printError("'"+executionEnvironment.getAbsolutePath()+"' is not a directory!");
            throw new IllegalArgumentException("'"+executionEnvironment.getAbsolutePath()+"' is not a directory!");
        }
        Runtime r = Runtime.getRuntime();
        Process proc = r.exec(tokens, null, executionEnvironment);

        //Error and Output handlers
        StreamWrapper errors = new StreamWrapper(proc.getErrorStream(), OutputType.ERROR);
        StreamWrapper output = new StreamWrapper(proc.getInputStream(), OutputType.OUTPUT);
        errors.start();
        output.start();

        int exitVal = 0;
        try {
            //wait until execution has ended
            exitVal = proc.waitFor();
            output.join();
            errors.join();
        }
        catch (InterruptedException e){
            Tool.printError("Execution of external program failed due to unknown reasons...");
        }
        return new ExecutionResult(exitVal, errors, output);
    }
}