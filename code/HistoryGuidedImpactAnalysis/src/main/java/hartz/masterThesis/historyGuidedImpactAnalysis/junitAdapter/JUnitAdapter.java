package hartz.masterThesis.historyGuidedImpactAnalysis.junitAdapter;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.MemorizingClassLoader;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.OutputType;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

/** Contains functionality to run JUnit tests from inside our tool, in case the user does not want to generate traces him- or herself */
public class JUnitAdapter {

    public static Result runJUnitTests(File directoryContainingTests, Version v, Set<String> skip){
        return runJUnitTests(v.getClassLoader(), directoryContainingTests, skip);
    }

    public static Result runJUnitTests(Set<Class> classes){
        return runJUnitTests(classes.toArray(new Class[classes.size()]));
    }

    public static Result[] runJUnitTests(Version v, Collection<File> directories, Set<String> skip){
        return runJUnitTests(v.getClassLoader(), skip, directories.toArray(new File[directories.size()]));
    }

    public static Result[] runJUnitTests(MemorizingClassLoader loader, Set<String> skip, File... directories){
        Result[] results = new Result[directories.length];
        int counter = 0;
        for (File d : directories){
            results[counter++] = runJUnitTests(loader, d, skip);
        }
        return results;
    }

    public static Result runJUnitTests(MemorizingClassLoader loader, File directoryContainingTestClasses, Set<String> skip){
        assert(directoryContainingTestClasses.isDirectory() && directoryContainingTestClasses.exists());

        List<Class<?>> testClasses = new ArrayList<>();
        List<File> list = FileUtils.listAllFilesInDirectoryAsFile(directoryContainingTestClasses);
        Iterator<File> i = list.iterator();
        while(i.hasNext()){
            File f = i.next();
            String name = f.getName().toLowerCase().trim();
            if (!name.endsWith(".class") || name.contains("$")) {
              /* inner classes should not contain tests, in fact the JLS says that the dollar
               * character should ONLY be used in automatically generated code, thus this line
               * s okay */
                i.remove();
                continue;
            }

            //check if this class is to be skipped
            String fullyQualifiedName1 = "";
            String fullyQualifiedName2 = "";
            try {
                fullyQualifiedName1 = FileUtils.obtainFullyQualifiedNameFromDirectoryStructure(loader.getClasspath(), f);
            }catch(IOException e){
                Tool.printDebug(e);
            }

            if (loader.getAdditionalClasspath() != null) {
                try {
                    fullyQualifiedName2 = (loader.getAdditionalClasspath() != null) ?
                            FileUtils.obtainFullyQualifiedNameFromDirectoryStructure(loader.getAdditionalClasspath(), f) :
                            "";
                } catch (IOException e) {
                    Tool.printDebug(e);
                }
            }

            if (fullyQualifiedName1.isEmpty() && fullyQualifiedName2.isEmpty()) {
                Tool.printError("Failed to obtain fully qualified class name for test class '" + f.getName() + "'");
                i.remove(); //if we cannot obtain a classname, we do not run this test!
            }
            else if (skip.contains(fullyQualifiedName1) || skip.contains(fullyQualifiedName2))
                i.remove();
        }

        //load classes that supposedly are JUnit tests...
        for (File clazzFile : list){
            File mainClassp = loader.getClasspath();
            File additionalClassp = loader.getAdditionalClasspath();
            Class<?> clazz = null;
            try {
                String fullyQualifiedNameOfClass;
                if (additionalClassp == null || !FileUtils.isSubDirectoryOrFile(additionalClassp, clazzFile)){
                    if (!FileUtils.isSubDirectoryOrFile(mainClassp, clazzFile)){
                        Tool.printError("Test Class '"+clazzFile.getAbsolutePath()+"' is not under the given compiled classpath (and not under the additional classpath, if any)!");
                        continue;
                    } else fullyQualifiedNameOfClass = FileUtils.obtainFullyQualifiedNameFromDirectoryStructure(mainClassp, clazzFile);
                }
                else fullyQualifiedNameOfClass = FileUtils.obtainFullyQualifiedNameFromDirectoryStructure(additionalClassp, clazzFile);

                clazz = loader.loadClass(fullyQualifiedNameOfClass);
            } catch (IOException e){
                Tool.printError(e.getMessage());
            }

            if (clazz != null) testClasses.add(clazz);
        }


        //call JUnit
        return runJUnitTests(testClasses.toArray(new Class<?>[testClasses.size()]));
    }


    public static Result runJUnitTests(Class<?>... tests){
        if (tests.length==0){
            Tool.print("     - detecting no tests to run, skipping JUnit execution");
            return new Result();
        }

        if (tests.length==1){
            Tool.printExtraInfo("     - trying to invoke JUnit for class '" + tests[0].getName() + "'");
        }
        else{
            Tool.printExtraInfo("     - trying to invoke JUnit for the following classes:");
            for (Class<?> c : tests){
                Tool.printExtraInfo("         ~> '"+c.getName()+"'");
            }
        }

        JUnitCore junit = new JUnitCore();
        Tool.printExtraInfo("     - beginning JUnit execution on "+tests.length+" classes");

        Tool.printToolOutput("     - ------------- output by executed test ------------------------", OutputType.OUTPUT);
        PrintStream originalSout = Tool.redirectSout(new PrintStream(new OutputStream() {public void write(int b) {/*DO NOTHING*/}}));
        PrintStream originalSerr = Tool.redirectSerr(new PrintStream(new OutputStream() {public void write(int b) {/*DO NOTHING*/}}));
        Result r = junit.run(tests); //tests might contain output, so let's build something around the logging!
        boolean successful = r.wasSuccessful();
        Tool.redirectSout(originalSout);
        Tool.redirectSerr(originalSerr);
        Tool.printToolOutput("     - ------------- end of output by executed test -----------------", OutputType.OUTPUT);
        Tool.printExtraInfo("     - JUnit execution has ended " + (successful ? "without" : "with") + " errors after " + (r.getRunTime() / 1000) + " seconds");
        if (!successful){
            Tool.printExtraInfo("   - " + r.getFailureCount() + " of " + r.getRunCount() + " test failed (though some of these classes might not have been JUnit Tests)");
            Tool.printExtraInfo("   - " + r.getIgnoreCount() + " of " + r.getRunCount() + " were ignored");
        }
        return r;
    }


    /**
     * DEBUG ONLY
     *
     * @param args : expects exactly one argument, the testing directory
     */
    public static void main (String[] args){
        if (args.length!=2)
            throw new IllegalArgumentException("Expecting exactly two argument, the classpath for the tests and path to a testing directory!");
        Tool.activateDebugMode();
        Tool.print(" * started JUnitAdapter, THIS IS NOT THE TOOL BUT SERVES ONLY DEBUG PURPOSES!");

        File f1 = new File(args[0]);
        File f2 = new File(args[1]);
        if (!f1.exists() || !f1.isDirectory())
            throw new IllegalArgumentException("Invalid argument!");
        if (!f2.exists() || !f2.isDirectory())
            throw new IllegalArgumentException("Invalid argument!");

        runJUnitTests(MemorizingClassLoader.getClassLoader(f1),f2, new HashSet<String>());
    }
}
