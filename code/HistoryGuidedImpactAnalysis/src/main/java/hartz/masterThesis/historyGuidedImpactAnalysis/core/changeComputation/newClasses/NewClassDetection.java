package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.newClasses;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileDiffType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileLevelChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChangeType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.syntaxChange.ClassLevelSyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Basically contains some simple String magic to detect a fresh class and to create a Class Target for it.
 */
public class NewClassDetection {

    /**
     * Determines if there are new classes in a given version and returns those as syntax change objects.
     *
     * @param v : version to analyze
     * @param fileLevelChanges : all file-level changes of this version
     * @param areRenamedElementsToBeTreatedAsSyntaxChanges : set to 'false' to exclude renaming operations
     * @return new classes as syntax changes of the corresponding type!
     */
    public static Set<SyntaxChange> createTargetsForNewClasses(Version v, List<FileLevelChange> fileLevelChanges,
                                                               boolean areRenamedElementsToBeTreatedAsSyntaxChanges) {

        Set<SyntaxChange> newClasses = new HashSet<>();

        //filter out new files and extract a class from them!
        for (FileLevelChange c : fileLevelChanges){
            if ( (c.getTypeOfChange() == FileDiffType.DELETED) || (c.getTypeOfChange()  == FileDiffType.MODIFIED)
                    || (c.getTypeOfChange()  == FileDiffType.UNKNOWN) )  continue;
            if ((c.getTypeOfChange() == FileDiffType.RENAMED) && !areRenamedElementsToBeTreatedAsSyntaxChanges) continue;

            File newClassFileInSourceFolder = c.getFile(v);
            if (c.isPartOfSourceCode()){
                assert(newClassFileInSourceFolder.exists());
                assert(newClassFileInSourceFolder.isFile());
                assert(newClassFileInSourceFolder.getName().toLowerCase().endsWith(".java"));

                try {
                    //get the package name and construct the absolute path to the package
                    String packageName = FileUtils.obtainPackageNameFromJava(newClassFileInSourceFolder);
                    String tmp = packageName;
                    tmp = tmp.replace(".", "/");
                    String pathToCompiledPackage = v.getCompiledSourcesDir().getAbsolutePath()+"/"+tmp;

                    /* Now, go through all the files in that package and scan if there are new ones related to the new class,
                     * this is necessary to also obtain new inner classes, which will only be visible as '.class' files with '$'
                     * characters in their name. Alternative would be to scan the Java-Code of course, but sadly ChangeDistiller
                     * can only compare classes and now analyze new ones. I will not start using another framework just for
                     * this use case.*/
                    File packageFile =  new File(pathToCompiledPackage);
                    String publicClassName = newClassFileInSourceFolder.getName(); //simply use the file

                    if (packageFile.exists()) {
                        publicClassName = publicClassName.substring(0, publicClassName.indexOf(".")); //we want the name only
                        /* TODO: this does not support the HORRIBLE practice of having multiple classes in the same file (which is
                         * valid java for some reason). Since ChangeDistiller does not support multiple classes in one file either
                         * however and this class implements functionality to complement Change Distiller, it is at least a
                         * consistently missing feature! */

                        for (File f : packageFile.listFiles()) {
                            if (f.getName().startsWith(publicClassName) //inner classes will look like 'ClassName$InnerClassName.class'
                                    && f.getName().toLowerCase().endsWith(".class")) {
                                String fullyQualifiedName = packageName + "." + f.getName().substring(0, f.getName().indexOf("."));

                                //load the class and create the target
                                Class<?> clazz = v.loadClass(fullyQualifiedName);
                                if (clazz != null) {
                                    Tool.printExtraInfo("   - found new class '" + fullyQualifiedName + "'!");
                                    newClasses.add(new ClassLevelSyntaxChange("" +
                                            "CLASS", SyntaxChangeType.NEW_CLASS, null /*old class*/,
                                            fullyQualifiedName, null /*affected method*/, null /*old code*/,
                                            null /*new class (not necessary as the class has been provided above*/));
                                }
                            }
                        }
                    } else Tool.printError("Failed to find compiled package structure '"+packageName+"' for new class '"+publicClassName+
                            "', seems like this file was either not compiled or it is actually in the wrong location since the base compiled sources classpath is '"+
                            v.getCompiledSourcesDir().getAbsolutePath()+"' and it could not be found there!");
                } catch (IOException e) {
                    Tool.printDebug(e);
                    //should actually never happen!
                }
            }
        }

        return newClasses;
    }
}
