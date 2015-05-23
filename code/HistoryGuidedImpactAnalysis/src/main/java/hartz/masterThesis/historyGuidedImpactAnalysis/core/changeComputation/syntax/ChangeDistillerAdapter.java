package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileDiffType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileLevelChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.syntaxChange.ClassLevelSyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.syntaxChange.MethodLevelSyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.MemorizingClassLoader;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import org.objectweb.asm.Type;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


/**
 * Basically, this is a front to call ChangeDistiller from the rest of the program.
 */
public class ChangeDistillerAdapter {

    private final static LinkedList<String> loadingFailedFor = new LinkedList<>();

    /**
     * Computes syntactical differences between modified files in two versions.
     *
     * @param fileLevelChanges : all file-level changes between the given versions
     * @param compareTo : the base we compare against, the "older" version in the default use case
     * @param comparing : the version we compute differences for, the "newer" version in the default use case
     * @param treatRenamingAsChange : set to 'false' to filter out renaming changes!
     * @return all changes detected in modified files
     */
    public static List<SyntaxChange> computeSyntaxChanges(List<FileLevelChange> fileLevelChanges,  Version compareTo,
                                                          Version comparing, boolean treatRenamingAsChange) {
        LinkedList<SyntaxChange> allSyntaxChanges = new LinkedList<>();
        loadingFailedFor.clear(); //clear error memory before starting with the next run!

        for (FileLevelChange fileLevelChange : fileLevelChanges) {
            if ( (treatRenamingAsChange && fileLevelChange.getTypeOfChange() == FileDiffType.RENAMED) ||
                    (fileLevelChange.getTypeOfChange() == FileDiffType.MODIFIED )) {

                 /* Convert the relative paths to actual ones, in theory one of the paths could not exist if the file-level change
                  * analysis screwed up. At the moment, changes modelling the movement of a file are not supported but the VCSs
                  * we currently use do not support them as well (which influenced this decision) so everything is okay :-)  */
                File pathInRepo1 = fileLevelChange.getFile(compareTo);
                File pathInRepo2 = fileLevelChange.getFile(comparing);
                String fileName = (pathInRepo2!=null ? pathInRepo2 : pathInRepo1).getName();

                if (pathInRepo1 == null || pathInRepo2 == null){
                    Tool.printExtraInfo("   - skipping detailed syntax analysis for file '" + fileName + "' because it is not present in both repositories for some reason");
                    continue;
                } else Tool.printExtraInfo("   - starting to processing syntax changes for file '" +fileName+"':");

                assert (fileLevelChange.isPartOfSourceCode());
                assert (pathInRepo1.getAbsolutePath().toLowerCase().endsWith(".java"));
                assert (pathInRepo2.getAbsolutePath().toLowerCase().endsWith(".java"));


                //if the file is modified, use change distiller to compare the two versions:
                List<SourceCodeChange> changes = ChangeDistillerAdapter.compareJavaClasses(pathInRepo1, pathInRepo2);
                HashSet<SyntaxChange> syntaxChangesForThisFile = new HashSet<>();

                //filter the detected changes:
                for (SourceCodeChange change : changes) {

                    try {
                        boolean renamed = false;

                    /* Source Code Changes come in 'Insert', 'Update', 'Delete' and 'Move' variants. For our
                     * purposes, we want to control what we export. Thanks to ChangeDistiller's stupid API,
                     * we have to do it by extracting all the relevant stuff now unless we want code duplication.*/


                        switch (change.getChangeType() /* we encode this as 'CodeChangeKind' */) {

                            //------------------ CLASSES AND INNER CLASSES -----------------------------------------
                            case CLASS_RENAMING:
                                if (!treatRenamingAsChange) break;
                                //else FALL-THROUGH and execute the code below
                                renamed = true;
                            case ADDITIONAL_CLASS:
                            case REMOVED_CLASS: {
                                /** Triggered for inner classes or multiple classes in the file, which is usually a mess anyway! */
                                handleClassLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                break;
                            }


                            //-------------------- INHERITANCE -----------------------------------------------------
                            case ADDING_CLASS_DERIVABILITY:
                            case REMOVING_CLASS_DERIVABILITY:
                            case PARENT_CLASS_CHANGE:
                            case PARENT_CLASS_DELETE:
                            case PARENT_CLASS_INSERT:
                            case PARENT_INTERFACE_CHANGE:
                            case PARENT_INTERFACE_DELETE:
                            case PARENT_INTERFACE_INSERT: {
                                /** This should pretty much be self-explanatory... */
                                handleClassLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                break;
                            }

                            //------------------ CHANGES TO ACCESSIBILITY --------------------------------------

                            case DECREASING_ACCESSIBILITY_CHANGE: //can be found for attributes, methods and classes
                            case INCREASING_ACCESSIBILITY_CHANGE: {
                                /** e.g. a field or a class is now protected instead of private. This is always class-level for us, in contrast
                                 *  the ChangeDistiller world (which I consider to be stupid...) */
                                handleClassLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                break;
                            }

                            //----------------- FIELDS INSIDE CLASSES -----------------------------------------
                            case ATTRIBUTE_RENAMING:
                                if (!treatRenamingAsChange) break;
                                //else FALL-THROUGH and execute the code below
                                renamed = true;
                            case ADDITIONAL_OBJECT_STATE:
                            case REMOVED_OBJECT_STATE: {
                                /** in change distiller's brilliant logic, adding a field is on class-level
                                 *  while making it final is its own field-level which would be okay IF IT
                                 *  HAD ANY CONNECTION OR ASSOCIATED WITH THE CLASS! Which is has not :-( */
                                handleClassLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                break;
                            }

                            case ADDING_ATTRIBUTE_MODIFIABILITY:
                            case REMOVING_ATTRIBUTE_MODIFIABILITY:
                            case ATTRIBUTE_TYPE_CHANGE: {
                                handleClassLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                break;
                            }


                            //----------------- CLASS-LEVEL CHANGES TO METHODS ---------------------------------
                            case METHOD_RENAMING:
                                if (!treatRenamingAsChange) break;
                                //else FALL-THROUGH and execute the code below
                                renamed = true;
                            case ADDITIONAL_FUNCTIONALITY:
                            case REMOVED_FUNCTIONALITY: {
                                /** new methods, still a class-level change for us in contrast to change distiller! */
                                handleClassLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                break;
                            }


                            //---- METHOD-HEADER CHANGES, STILL CLASS LEVEL -----------------------------------
                            case PARAMETER_RENAMING:
                                if (!treatRenamingAsChange) break;
                                //else FALL-THROUGH and execute the code below
                                renamed = true;
                            case PARAMETER_DELETE:
                            case PARAMETER_INSERT:
                            case PARAMETER_ORDERING_CHANGE:
                            case ADDING_METHOD_OVERRIDABILITY: //final-methods
                            case REMOVING_METHOD_OVERRIDABILITY:
                            case RETURN_TYPE_CHANGE:
                            case RETURN_TYPE_DELETE:
                            case RETURN_TYPE_INSERT:
                            case PARAMETER_TYPE_CHANGE: {
                                /** again, self-explanatory. Personal side node, one can again only ask oneself what the Change Distiller
                                 *  creator thought to himself here, e.g. a parameter entity has no idea about its type while it does
                                 *  print the type for fields of a class. One could argue that a parameter does not need the type as long
                                 *  as you know the method and the parameter's index BUT GUESS WHAT, no index given. I hate this API...
                                 */
                                handleClassLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                break;
                            }


                            //--- STATEMENTS AND BRANCHES, THE ONLY THING I CONSIDER METHOD-LEVEL ----------------
                            case STATEMENT_INSERT:
                            case STATEMENT_DELETE:
                            case STATEMENT_ORDERING_CHANGE:
                            case ALTERNATIVE_PART_DELETE:
                            case ALTERNATIVE_PART_INSERT://this and the above are not 'statements' in the true sense...
                            case CONDITION_EXPRESSION_CHANGE:
                            case STATEMENT_PARENT_CHANGE:
                            case STATEMENT_UPDATE: {
                                /** Stuff happening inside a method. */
                                handleMethodLevelChange(change, syntaxChangesForThisFile, compareTo.getClassLoader(), comparing.getClassLoader(), renamed);
                                //TODO: is this also triggered for complex expressions in fields?
                                break;
                            }

                            //------------------ DOCUMENTATION AND COMMENTS ------------------------
                            case DOC_DELETE:
                            case DOC_INSERT:
                            case DOC_UPDATE:
                            case COMMENT_DELETE:
                            case COMMENT_INSERT:
                            case COMMENT_MOVE:
                            case COMMENT_UPDATE: {
                            /* do nothing */
                                break;
                            }

                            //change distiller has no clue what we are dealing with...
                            case UNCLASSIFIED_CHANGE:
                                Tool.print("WARNING: Could not classify source code change for '" +
                                        change.getChangedEntity().getUniqueName() + "' in '" + fileName + "'");


                        }//end of switch
                    } catch (Exception e){
                        Tool.printDebug(e);
                        Tool.printError("Unexpected problem occurred when processing '"+change.toString()+"' in '" + fileName + "', skipping change!");
                    }

                } //end of loop over all syntax changes

                if (!syntaxChangesForThisFile.isEmpty()) {
                    allSyntaxChanges.addAll(syntaxChangesForThisFile);
                }

            } else Tool.printExtraInfo("   - skipping detailed syntax analysis for file '" + fileLevelChange.getRelativePathForDebug() +
                    "' because its status is " + fileLevelChange.getTypeOfChange());
        }

        return allSyntaxChanges;
    }

    /**
     * Deals with a change outside a method.
     *
     * @param change : the change reported by change distiller (e.g. a changed field)
     * @param resultList : list to store any found results, may thus be modified by this method
     * @param thisIsARename : 'true' if this change is flagged as a rename operation
     */
    private static void handleClassLevelChange(SourceCodeChange change, HashSet<SyntaxChange> resultList,
                                               MemorizingClassLoader loadFromOld, MemorizingClassLoader loadFromNew,
                                               boolean thisIsARename) {
        SourceCodeEntity[] entities = getSourceCodeEntities(change);
        String oldClassName;
        String newClassName;
        String oldCode;
        String newCode;

        String fullyQualifiedMethodName = null;
        SyntaxChangeType type = getTypeOfChange(change);
        MemorizingClassLoader classLoaderToUse = getCorrectClassLoader(loadFromOld, loadFromNew, type);
        boolean isNewClassLoader = isNewClassLoader(type);

        if (change.getRootEntity().getType().isMethod() || change.getChangedEntity().getType().isMethod()){
            /** This handles all changes reported by change distiller to be on method-level but are to be mapped onto class-level,
             *  because they that take place on a method's header, e.g. a method becomes 'public' after being 'private' before */

            String fullyQualifiedClassName;
            String methodHeader;
            if (change.getChangedEntity().getType().isMethod() && change.getRootEntity().getType().isClass()){
                /** addition or removal of a whole method */
                if (type == SyntaxChangeType.CODE_REMOVAL){
                    //read from the old entities
                    fullyQualifiedClassName = entities[2].getUniqueName();
                    methodHeader = getMethodHeader(entities[0]); //parent will be the class!
                } else {
                    //read from the new ones
                    fullyQualifiedClassName = entities[3].getUniqueName();
                    methodHeader = getMethodHeader(entities[1]); //dito
                }
                oldCode = entities[0] != null ? getMethodHeader(entities[0]) : null;
                newCode = entities[1] != null ? getMethodHeader(entities[1]) : null;
            }
            else{
                assert (change.getRootEntity().getType().isMethod());
                /** any modification of a method, e.g. a new parameter */
                fullyQualifiedClassName = getClassName(change.getRootEntity());
                methodHeader = getMethodHeader(change.getRootEntity());
                oldCode = entities[0] != null ? entities[0].getUniqueName() : null;
                newCode = entities[1] != null ? entities[1].getUniqueName() : null;

                /* For this special case, we will always have to load from the new class, as this one contains
                 * the now modified method with the different parameters! Since the class was not changed, that
                 * is okay since we only load said class to make sure we fix ChangeDistiller's class name for
                 * inner classes. A lot of work just because they do not give proper class names...    */
                classLoaderToUse = loadFromNew;
                isNewClassLoader = true;
            }

            //try to match the method that was added/removed/modified
            assert(methodHeader != null);
            Class actualClass = loadClassThatIsPotentiallyAnInnerClassByTrialAndError(fullyQualifiedClassName, classLoaderToUse);
            if (actualClass == null){
                /* since we disable the Memorizing Class Loader's error reporting functionality here, we need to mimic it! */
                if (!loadingFailedFor.contains(fullyQualifiedClassName)) {
                    Tool.printError("Failed to find class '" + fullyQualifiedClassName + (isNewClassLoader?"'":"' in predecessor version") +
                            ", was either not compiled into compiled classpath directory or is anonymous inner class. Associated syntax changes will be skipped!");
                    loadingFailedFor.add(fullyQualifiedClassName);
                }
                return;
            } else fullyQualifiedClassName = actualClass.getName();

            fullyQualifiedMethodName = findMethodOrConstructor(
                    methodHeader, //method we need to find
                    actualClass
            );

            //if we found something and could match it uniquely, export it!
            if (fullyQualifiedMethodName != null){
                oldClassName = null; //class is never changed in this case
                newClassName = fullyQualifiedClassName;
            }
            else return; //failed to match method!


        } else if (change.getRootEntity().getType().isField()){
            /** If a field is modified (that means not added or removed from a class), change distiller reports something
             *  that is not associated with the class whatsoever but lives on its own level, the so called 'attribute-level'.
             *  We do not want that and have to re-map this to the class-level.
             *
             *  In case of adding or removing a field however, get some something with a class as root entity. I really hate
             *  this API...   :-(   */
            oldCode = entities[0] != null ? getFieldDesc(entities[0], entities[2], change.getRootEntity()) : null;
            newCode = entities[1] != null ? getFieldDesc(entities[1], entities[3], change.getRootEntity()) : null;

            oldClassName = null; //old class is never changed in this case, after all the class is still there!
            //since we need proper class names, we also load the class in this case even if we do not really need it...
            Class actualClass = loadClassThatIsPotentiallyAnInnerClassByTrialAndError(getClassName(change.getRootEntity()), classLoaderToUse);
            if (actualClass == null){
                /* since we disable the Memorizing Class Loader's error reporting functionality here, we need to mimic it! */
                if (!loadingFailedFor.contains(getClassName(change.getRootEntity()))) {
                    Tool.printError("Failed to find class '" + getClassName(change.getRootEntity()) + (isNewClassLoader?"'":
                            " in predecessor version") + ", was either not compiled into compiled classpath directory or is anonymous inner class. Associated syntax changes will be skipped!");
                    loadingFailedFor.add(getClassName(change.getRootEntity()));
                }
                return;
            } else newClassName =  actualClass.getName();


        } else {
            /** This covers everything not covered above that is to be considered on the class-level, which means that
             *  the root entity is a class in change distiller's way of putting things. */
            assert(change.getRootEntity().getType().isClass() || change.getChangedEntity().getType().isClass());

            if (change.getChangedEntity().getType().isField()){
                /** Addition or removal of a class' field. */
                oldClassName = entities[0] != null ? getClassName(entities[0]) : null;
                newClassName = entities[1] != null ? getClassName(entities[1]) : null;
                oldCode = entities[0] != null ? reformatFieldDesc(entities[0].getUniqueName()) : null;
                newCode = entities[1] != null ? reformatFieldDesc(entities[1].getUniqueName()) : null;
            }
            else{
                /** everything else */
                oldClassName = entities[0] != null ? entities[0].getUniqueName() : null;
                newClassName = entities[1] != null ? entities[1].getUniqueName() : null;
                oldCode = oldClassName;
                newCode = newClassName;
            }

            //in this case, we actually need to load TWO CLASSES, at least potentially...
            if (newClassName != null){
                Class actualClass = loadClassThatIsPotentiallyAnInnerClassByTrialAndError(newClassName, loadFromNew);
                if (actualClass == null){
                    if (!loadingFailedFor.contains(newClassName)) {
                        Tool.printError("Failed to find class '" + newClassName +
                                ", was either not compiled into compiled classpath directory or is anonymous inner class. Associated syntax changes will be skipped!");
                        loadingFailedFor.add(newClassName);
                    }
                    return;
                } else newClassName = actualClass.getName(); //overwrite with potential inner class name
            }
            if (oldClassName != null){
                Class actualClass = loadClassThatIsPotentiallyAnInnerClassByTrialAndError(oldClassName, loadFromOld);
                if (actualClass == null){
                    if (!loadingFailedFor.contains(oldClassName)) {
                        Tool.printError("Failed to find class '" + oldClassName  + "' in predecessor version" +
                                ", was either not compiled into compiled classpath directory or is anonymous inner class. Associated syntax changes will be skipped!");
                        loadingFailedFor.add(oldClassName);
                    }
                    return;
                } else oldClassName = actualClass.getName(); //overwrite with potential inner class name
            }

        }

        String classOfCode = change.getChangedEntity().getType().name().toUpperCase();


        /** workaround for new or removed inner classes */
        if (classOfCode.equals("CLASS") && (type == SyntaxChangeType.CODE_ADDITION || type == SyntaxChangeType.CODE_REMOVAL)){
            String noDollarOld = (oldClassName!=null)?oldClassName.replace("$","."):null;
            String noDollarNew = (newClassName!=null)?newClassName.replace("$","."):null;
            if ( (oldCode != null && oldCode.equals(noDollarOld)) || (newCode != null && newCode.equals(noDollarNew)) ){
                type = (oldCode!=null) ? SyntaxChangeType.REMOVED_CLASS : SyntaxChangeType.NEW_CLASS;
                oldCode = null;
                newCode = null;
            }
        }


        ClassLevelSyntaxChange classLevelChange = new ClassLevelSyntaxChange(
                classOfCode,
                thisIsARename?SyntaxChangeType.RENAMING:type,
                oldClassName,
                newClassName,
                fullyQualifiedMethodName, //may be null
                oldCode,
                newCode
        );
        resultList.add(classLevelChange); //store
        Tool.printExtraInfo("     ~> found class-level change in "+classLevelChange); //report
    }




    /**
     * Deals with a change inside a method. Attempts to match the change against the actual method present!
     *
     * @param change      : the change reported by change distiller
     * @param resultList  : list to store any found results, may thus be modified by this method
     * @param loadFromOld : a class loader to load the unmodified class and verify this reported change
     * @param loadFromNew : a class loader to load the modified class and verify this reported change
     * @param thisIsARename : 'true' if this change is flagged as a rename operation
     */
    private static void handleMethodLevelChange(SourceCodeChange change, HashSet<SyntaxChange> resultList,
                                                MemorizingClassLoader loadFromOld, MemorizingClassLoader loadFromNew,
                                                boolean thisIsARename) {
        StructureEntityVersion methodRootEntity = change.getRootEntity();
        assert (methodRootEntity.getType().isMethod()); //everything else would be OUTSIDE of a method!

        String fullyQualifiedClassName = getClassName(methodRootEntity);
        SyntaxChangeType type = getTypeOfChange(change);
        MemorizingClassLoader classLoaderToUse = getCorrectClassLoader(loadFromOld, loadFromNew, type);
        boolean isNewClassLoader = isNewClassLoader(type);

        //try to match the method that was added/removed/modified
        String methodHeader = getMethodHeader(methodRootEntity);
        assert(methodHeader != null);
        Class actualClass = loadClassThatIsPotentiallyAnInnerClassByTrialAndError(fullyQualifiedClassName, classLoaderToUse);
        if (actualClass == null){
            if (!loadingFailedFor.contains(fullyQualifiedClassName)) {
                Tool.printError("Failed to find class '" + fullyQualifiedClassName + (isNewClassLoader?"'":"' in predecessor version") +
                        ", was either not compiled into compiled classpath directory or is anonymous inner class. Associated syntax changes will be skipped!");
                loadingFailedFor.add(fullyQualifiedClassName);
            }
            return;
        } else fullyQualifiedClassName = actualClass.getName();

        String fullyQualifiedMethodName = findMethodOrConstructor(
                methodHeader,
                actualClass
        );

        //if we found something, export it...
        if (fullyQualifiedMethodName != null) {
            SourceCodeEntity[] entities = getSourceCodeEntities(change);

            MethodLevelSyntaxChange methodLevelChange = new MethodLevelSyntaxChange(
                    change.getChangedEntity().getType().name().toUpperCase(),
                    thisIsARename?SyntaxChangeType.RENAMING:type,
                    entities[0] != null ? entities[0].getUniqueName() : null,
                    entities[1] != null ? entities[1].getUniqueName() : null,
                    fullyQualifiedClassName,
                    fullyQualifiedMethodName
            );
            resultList.add(methodLevelChange); //store
            Tool.printExtraInfo("     ~> found method-level change in "+methodLevelChange); //report
        }
    }

    /**
     * Activates ChangeDistiller on two given Java files.
     *
     * @param f1 : the first file which serves as the base
     * @param f2 : the second file which represents the "changed one"
     * @return a list of source code changes
     */
    public static List<SourceCodeChange> compareJavaClasses(File f1, File f2) {
        assert (f1.exists() && f1.isFile());
        assert (f2.exists() && f2.isFile());
        assert (f1.getAbsolutePath().toLowerCase().endsWith(".java"));
        assert (f2.getAbsolutePath().toLowerCase().endsWith(".java"));

        FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
        List<SourceCodeChange> changes = null;
        try {
            distiller.extractClassifiedSourceCodeChanges(f1, f2);
            changes = distiller.getSourceCodeChanges();
        } catch (Exception e) {
            Tool.printError("Encountered the following error while distilling changes:\n\n" + e.getMessage() + "\n\n");
            Tool.printError("This is most likely a bug in Change Distiller. Please file a bug report at" +
                    " https://bitbucket.org/sealuzh/tools-changedistiller/issues and attach the" +
                    " full stack trace along with the two files that you tried to distill. Attempting to continue with execution...");
        }
        return (changes != null) ? changes : new LinkedList<SourceCodeChange>();
    }

    /** Terrible hack. Change Distiller has no idea about inner classes :-( This means we basically need to
     *  try out if a class-not-found occurs because of a inner class problem. It is quite ugly...
     *
     * @param fullyQualifiedClassName : class name of the method's class, e.g. 'thisPackageOfMine.TestClass'
     * @param classLoader             : a class loader ready to load above class by its fully qualified name
     * @return the loaded class or 'null' if no class exists
     */
    public static Class loadClassThatIsPotentiallyAnInnerClassByTrialAndError(String fullyQualifiedClassName, MemorizingClassLoader classLoader){
        String classNameForLoading = fullyQualifiedClassName;
        Class<?> clazz;
        while(true){
            //attempt to load the class, potentially with a modified class name when compared to the given parameter...
            clazz = classLoader.loadClass(classNameForLoading, null /* to not report errors*/);
            if (clazz == null && classNameForLoading.indexOf(".") != classNameForLoading.lastIndexOf(".")){
                /** if the loading would have been successful, we would exit the loop. In this case we attempt the load
                 *  operation again but with the innermost entry considered to be an inner class. It is terrible but I
                 *  see no other way as change distiller simply has no idea about these constructions... */
                classNameForLoading = classNameForLoading.substring(0,classNameForLoading.lastIndexOf(".")) + //part before the last '.'
                        "$" + //replace the last '.' with '$' in case we are trying to load a inner class
                        classNameForLoading.substring(classNameForLoading.lastIndexOf(".")+1, classNameForLoading.length());//part after the last '.'
                Tool.printDebug("Attempting inner class detection, new class name is '"+classNameForLoading+"'");
            }
            else break;
        }

        return clazz;
    }

    /**
     * Given a source code change, this will try to find the matching method or constructor
     * of the class to the one reported by change distiller's SourceCodeChange output.
     *
     * @param methodHeader : the changed method or constructor declaration, e.g. 'main(String[])'
     * @param clazz : class where the method is to be found
     * @return a fully qualified method name or 'null' if none could be matched uniquely
     */
    public static String findMethodOrConstructor(String methodHeader, Class clazz){
        if (clazz == null) return null; //loading of the class failed...

        /**
         * We need to load the class and use reflection to obtain the method's header and
         * then try to match change distiller's output to it. This is necessary since change
         * distiller does not provide bindings or fully qualified class names for parameters).
         */
        //get the name of the method
        String methodName = methodHeader.substring(0, methodHeader.indexOf("("));

        //get the parameter types of the method
        String tmp = methodHeader.substring(methodHeader.indexOf("(") + 1, methodHeader.lastIndexOf(")")).trim();
        String[] parameterClassNames;
        if (tmp.isEmpty()) parameterClassNames = new String[0];
        else {
            //filter and copy over
            String[] tmpArr = tmp.split(",");
            int j = 0;
            for (int i = 0; i < tmpArr.length; ++i) {
                String s = tmpArr[i].trim();

                while (s.contains(".") || s.contains("<")){
                    /** deal with monstrosities like 'java.lang.Collection<Connection.KeyVal>' */
                    int dot = s.contains(".") ? s.indexOf(".") : Integer.MAX_VALUE;
                    int less = s.contains("<") ? s.indexOf("<") : Integer.MAX_VALUE;
                    if (dot != less){
                        assert (dot != Integer.MAX_VALUE || less != Integer.MAX_VALUE);
                        if (less == Integer.MAX_VALUE){
                            /** java.lang.Collection -> lang.Collection*/
                            s = s.substring(s.indexOf(".")+1);
                        } else {
                            /** java.lang.Collection<Connection.KeyVal> -> java.lang.Collection*/
                            s = s.substring(0,s.indexOf("<"));
                        }
                    }
                }
                if (!s.isEmpty())
                    tmpArr[j++] = s;
            }
            parameterClassNames = new String[j];
            System.arraycopy(tmpArr, 0, parameterClassNames, 0, j);
        }

        Method m = findMethod(clazz, methodName, parameterClassNames);
        Constructor<?> c = findConstructor(clazz, methodName, parameterClassNames);

        if ((m == null && c == null)) {
            Tool.printError("Failed to find changed method or constructor '" + methodHeader +
                    "' for reported source code change in '" + clazz.getSimpleName() +
                    "'. NOTE: This probably results from an earlier 'Class not found'-Problem!");
            return null;
        } else if ((m != null && c != null)) {
            Tool.printError("Failed to find a unique matching for '" + methodHeader + "' in class '" + clazz.getSimpleName() +
                    "'. The associated syntax change will be ignored.");
            return null;
        }


        //this will then produce a proper descriptor
        if (m!=null)
            return m.getName()+Type.getMethodDescriptor(m);
        else return "<init>"+Type.getConstructorDescriptor(c);
    }


    private static Method findMethod(Class<?> clazz, String methodName, String[] parameterClassNames) {
        LinkedList<Method> methodsOfClass = new LinkedList<>();

        if (parameterClassNames.length == 0){
            try {
                return clazz.getDeclaredMethod(methodName);
            } catch (Throwable t){
                /** no such method */
            }
        } else {
            try {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (    //check name and number of parameters first
                            m.getName().equals(methodName) &&
                                    m.getParameterTypes().length == parameterClassNames.length) {
                        //then, check the parameters of the method

                        boolean matches = true;
                        for (int i = 0; i < parameterClassNames.length; ++i) {
                            String expected = parameterClassNames[i];
                            String found = m.getParameterTypes()[i].getSimpleName();

                            if (!expected.equals(found)) {
                                matches = false;
                                break;
                            }
                        }

                        if (matches) {
                            methodsOfClass.add(m);
                        }
                    }
                }
            } catch (Throwable t) {
                Tool.printDebug(t);
                Tool.printExtraInfo("     ~> WARNING: Exception occurred during lookup of method '" + methodName + "' while scanning methods!");
                Tool.printExtraInfo("        Message was: " + t.getMessage());
            }

            //return the method if it could be uniquely determined
            if (methodsOfClass.size() == 1) return methodsOfClass.getFirst();
        }

        return null;
    }

    private static Constructor<?> findConstructor(Class<?> clazz, String constructorName, String[] parameterClassNames) {
        LinkedList<Constructor<?>> constructorsOfClass = new LinkedList<>();
        try{
            /** This loop is executed only up to two times, we simply want to re-use the code for two different cases! */
            boolean insertOuterInstanceParameter = false;
            out: while(constructorsOfClass.isEmpty()){
                for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                    //getName()-output for constructors includes the package, so do some string magic:
                    String nameC = c.getName().substring(c.getName().lastIndexOf(".") + 1);
                    if (nameC.contains("$")) nameC = c.getName().substring(c.getName().lastIndexOf("$") + 1);

                    int actualNumberOfParameters = c.getParameterTypes().length;
                    int expectedNumberOfParameters = parameterClassNames.length;
                    if (insertOuterInstanceParameter) ++expectedNumberOfParameters;

                    if (nameC.equals(constructorName) && expectedNumberOfParameters   == actualNumberOfParameters) {
                        //check the individual parameters!

                        boolean matches = true;
                        parameters : for (int i = 0; i < actualNumberOfParameters; ++i) {

                            String expected;
                            if (i==0 && insertOuterInstanceParameter)
                                if (clazz.getDeclaringClass() != null)
                                    expected = clazz.getDeclaringClass().getSimpleName();
                                else break out; //no constructor to be found!
                            else
                                expected = parameterClassNames[insertOuterInstanceParameter?i-1:i];
                            String found = c.getParameterTypes()[i].getSimpleName();

                            if (!expected.equals(found)) {
                                matches = false;
                                break parameters; //no need to check the rest
                            }
                        }

                        if (matches) {
                            constructorsOfClass.add(c);
                        }
                    }
                }


                if (constructorsOfClass.isEmpty() && insertOuterInstanceParameter) break;
                else /** constructors of inner, non static classes always get the outer instance as automatic
                 first parameter. Since we have no idea exactly what this class is (inner? static nested?
                 local? anonymous?), we simply try to find a constructor using both methods */
                    insertOuterInstanceParameter = true;
            }
        } catch (Throwable t){
            Tool.printDebug(t);
            Tool.printExtraInfo("     ~> WARNING: Exception occurred during lookup of method '"+constructorName+"' while scanning constructors!");
            Tool.printExtraInfo("        Message was: "+t.getMessage());
        }

        //return the method if it could be uniquely determined
        if (constructorsOfClass.size() != 1) return null;
        else return constructorsOfClass.getFirst();
    }


    private static SyntaxChangeType getTypeOfChange(SourceCodeChange change) {
        if (change instanceof Insert) {
            return SyntaxChangeType.CODE_ADDITION;
        } else if (change instanceof Move) {
            return SyntaxChangeType.CODE_MOVE;
        } else if (change instanceof Delete) {
            return SyntaxChangeType.CODE_REMOVAL;
        } else if (change instanceof Update) {
            return SyntaxChangeType.CODE_MODIFICATION;
        } else throw new InternalError("Change Distiller is not supposed to have other " +
                "source code change classes than 'Insert', 'Update', 'Delete' and 'Move'!");
    }

    private static MemorizingClassLoader getCorrectClassLoader(MemorizingClassLoader loadFromOld, MemorizingClassLoader loadFromNew,
                                                               SyntaxChangeType type) {
        switch (type){
            case CODE_ADDITION: return loadFromNew;
            case CODE_MODIFICATION: return loadFromNew;
            case CODE_MOVE: return loadFromNew;
            case CODE_REMOVAL: return loadFromOld;
            case NEW_CLASS: return loadFromNew;
            default : throw new InternalError("Programmer has forgotten type '"+type+"' in a switch-case!");
        }
    }

    private static boolean isNewClassLoader(SyntaxChangeType type) {
        switch (type){
            case CODE_ADDITION: return true;
            case CODE_MODIFICATION: return true;
            case CODE_MOVE: return true;
            case CODE_REMOVAL: return false;
            case NEW_CLASS: return true;
            default : throw new InternalError("Programmer has forgotten type '"+type+"' in a switch-case!");
        }
    }

    /**
     * Extracts all associated entitys of a change BUT the root entity and returns
     * them as an array. Depending on the type of change, not all entries may be set.
     *
     * @param change : a change reported by ChangeDistiller
     * @return [oldEntity, newEntity, parentOfOldEntity, parentOfNewEntity]
     */
    private static SourceCodeEntity[] getSourceCodeEntities(SourceCodeChange change) {

        SourceCodeEntity[] result = new SourceCodeEntity[]{
                null, //old entity
                null, //new entity
                null, //old parent entity
                null //new parent entity
        };

        switch (getTypeOfChange(change)) {
            case CODE_ADDITION:
                Insert i = (Insert) change;
                result[1] = i.getChangedEntity();
                result[3] = i.getParentEntity();
                break;
            case CODE_REMOVAL:
                Delete d = (Delete) change;
                result[0] = d.getChangedEntity();
                result[2] = d.getParentEntity();
                break;
            case CODE_MODIFICATION:
                Update u = (Update) change;
                result[0] = u.getChangedEntity();
                result[1] = u.getNewEntity();
                result[2] = u.getParentEntity(); //parent is the same
                result[3] = u.getParentEntity();
                break;
            case CODE_MOVE:
                Move m = (Move) change;
                result[0] = m.getChangedEntity();
                result[1] = m.getNewEntity();
                result[2] = m.getParentEntity(); //parent has been changed
                result[3] = m.getNewParentEntity();
                break;

            default:
                throw new InternalError("Change Distiller is not supposed to have other " +
                        "source code change classes than 'Insert', 'Update', 'Delete' and 'Move'!");
        }
        return result;
    }



    /**
     * HELPER METHOD. ChangeDistiller is really stupid and instead of returning a nice
     * tree, it returns a senseless mess of objects called entities that lose all their
     * relationships which were encoded in the AST. This method attempts to extract a
     * method's header from the so-called 'unique name' that the various entity classes
     * representing a method return.
     *
     * @param entity : a structure or source code entity of change distiller, should be
     *                 one representing a method-related element!
     * @return the method's header string without the class' name in front
     */
    private static String getMethodHeader(Object entity) {
        assert (!(entity instanceof String)); //verify that I am not stupid...

        String uniqueNameReportedByChangeDistiller;
        if (entity instanceof StructureEntityVersion){
            StructureEntityVersion method = (StructureEntityVersion) entity;
            uniqueNameReportedByChangeDistiller = method.getUniqueName();
        }
        else if (entity instanceof SourceCodeEntity){
            SourceCodeEntity method = (SourceCodeEntity) entity;
            uniqueNameReportedByChangeDistiller = method.getUniqueName();
        }
        else return null;

        if (uniqueNameReportedByChangeDistiller.contains(".") && uniqueNameReportedByChangeDistiller.contains("(")
                && uniqueNameReportedByChangeDistiller.contains(")") /* then its a method related entity*/) {
            int indexOfBracket = uniqueNameReportedByChangeDistiller.indexOf("(");
            String parametersCutOff = uniqueNameReportedByChangeDistiller.substring(0,indexOfBracket);
            String parametersOnly = uniqueNameReportedByChangeDistiller.substring(indexOfBracket);
            //we need to do this step-wise to handle methods like package.class.method(Collection<java.lang.String>) properly!
            return parametersCutOff.substring(parametersCutOff.lastIndexOf(".") + 1)+parametersOnly;
        }
        else return null;
    }


    /**
     * Obtains which field changed.
     *
     * @param changedEntity : entity that changed
     * @param parentEntity : parent of that entity
     * @param rootEntity : root entity of the entity that changed, this must be a field!
     * @return nicely formatted data about the given attribute
     */
    private static String getFieldDesc(SourceCodeEntity changedEntity, SourceCodeEntity parentEntity, StructureEntityVersion rootEntity) {
        if (changedEntity.getType().isField()){
            return changedEntity.getUniqueName();
        } else {
            if (parentEntity != null && parentEntity.getType().isField()) {
                return parentEntity.getUniqueName();
            } else {
                assert(rootEntity.getType().isField());
                return rootEntity.getUniqueName();
            }
        }
    }

    /**
     * Helper-method to get ChangeDistiller's terrible output in a better format.
     *
     * @param fullyQualifiedFieldNameWithType unique name of a field entity, change distiller's format here is
     *                                        'Package.class.fieldName : Type'
     * @return nicely formatted data about the given attribute
     */
    private static String reformatFieldDesc(String fullyQualifiedFieldNameWithType) {
        if (!fullyQualifiedFieldNameWithType.contains(":")) return fullyQualifiedFieldNameWithType;

        String nameOfField = fullyQualifiedFieldNameWithType.substring(
                fullyQualifiedFieldNameWithType.lastIndexOf(".")+1,
                fullyQualifiedFieldNameWithType.indexOf(":")
        ).trim();
        String typeOfField = fullyQualifiedFieldNameWithType.substring(
                fullyQualifiedFieldNameWithType.lastIndexOf(":")+1,
                fullyQualifiedFieldNameWithType.length()
        ).trim();

        return typeOfField+" "+nameOfField;
    }

    /**
     * HELPER METHOD. ChangeDistiller is really stupid and instead of returning a nice
     * tree, it returns a senseless mess of objects called entities that lose all their
     * relationships which were encoded in the AST. This method attempts to extract a
     * fully qualified class name from the so-called 'unique name' from an entity's
     * unique name. Note that the format of this unique name varies greatly :-(
     *
     * @param entity : a structure or source code entity of change distiller
     * @return the fully qualified class name of the class housing this method
     */
    private static String getClassName(Object entity) {
        String uniqueNameReportedByChangeDistiller;
        if (entity instanceof StructureEntityVersion){
            StructureEntityVersion e = (StructureEntityVersion) entity;
            uniqueNameReportedByChangeDistiller = e.getUniqueName();
        }
        else if (entity instanceof SourceCodeEntity){
            SourceCodeEntity e = (SourceCodeEntity) entity;
            uniqueNameReportedByChangeDistiller = e.getUniqueName();
        }
        else return null;

        if (uniqueNameReportedByChangeDistiller.contains(".")){
            if (uniqueNameReportedByChangeDistiller.contains(":")) //'field of a class'-case
                return uniqueNameReportedByChangeDistiller.substring(0, uniqueNameReportedByChangeDistiller.lastIndexOf("."))
                        .substring(0, uniqueNameReportedByChangeDistiller.lastIndexOf(".")); //the second-to-last '.' is the one we want!
            else { //everything else, e.g. from a method
                if (uniqueNameReportedByChangeDistiller.contains(".") && uniqueNameReportedByChangeDistiller.contains("(")
                        && uniqueNameReportedByChangeDistiller.contains(")") /* then its a method related entity*/) {
                    String parametersCutOff = uniqueNameReportedByChangeDistiller.substring(0,uniqueNameReportedByChangeDistiller.indexOf("("));
                    return parametersCutOff.substring(0, parametersCutOff.lastIndexOf("."));
                } else {
                    //something else, just do the substring by point and pray...
                    return uniqueNameReportedByChangeDistiller.substring(0, uniqueNameReportedByChangeDistiller.lastIndexOf("."));
                }
            }

        }
        else return null;
    }
}
