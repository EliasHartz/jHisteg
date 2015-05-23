package hartz.masterThesis.adapter;

import hartz.masterThesis.adapter.generators.evosuite.EvosuiteAdapter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

public class Main {

    /** Can be used to obtain Evosuite calls to generate tests */
    public static void main(String[] args) throws IOException {
        LinkedList<VersionDirContainer> versions = new LinkedList<>();
        if (args.length == 0){
            System.err.println("Please provide paths to version directories (with testing targets) for which you wish to obtain arguments to call Evosuite!");
            return;
        }

        File outputDir = null;

        boolean stats = false;
        boolean flat = false;
        if (args.length <= 4){
            for (String s : args){
                if (!s.trim().isEmpty()){
                    if (s.equals("--stats")) stats = true;
                    else if (s.equals("--flat")) flat = true;
                    else{
                        if (outputDir != null)
                            throw new IllegalArgumentException("Cannot work in two output directores at the same time, use distinct calls!");
                        outputDir = new File(s);
                        if (outputDir.exists()){
                            File[] arr = outputDir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File file) {
                                    return file.getName().startsWith("history_of_");
                                }
                            });
                            if (arr.length == 1){
                                for (File versionDir : outputDir.listFiles(new FileFilter() {
                                    @Override
                                    public boolean accept(File file) {
                                        try{
                                            return Integer.parseInt(file.getName())>=0;
                                        } catch (NumberFormatException e) { return false; }
                                    }
                                })){
                                    versions.add(new VersionDirContainer(versionDir));
                                }
                            }
                        } else throw new IllegalArgumentException(outputDir.getAbsolutePath()+" is not a jHisteg output directory!");
                    }
                }
            }
        }
        if (versions.isEmpty()){
            for (String s : args){
                if (!s.trim().isEmpty()){
                    if (s.equals("--stats")) stats = true;
                    else if (s.equals("--flat")) flat = true;
                    else versions.add(new VersionDirContainer(new File(s)));
                }
            }
        }

        Collections.sort(versions);


        if (stats){
            String fileLevelM = "";
            String syntaxM = "";
            String jHistegM = "";
            String version = "";
            for (VersionDirContainer v : versions){
                //get file level information
                Set<String>[] filelevel = v.getFileLevelChanges();
                Set<String> addedClasses = filelevel[0];
                Set<String> modifiedClasses = filelevel[1];
                addedClasses = convertToFullyQualifiedNames(addedClasses,v);
                modifiedClasses = convertToFullyQualifiedNames(modifiedClasses,v);

                Set<String> methodsToTest_added = new HashSet<>();
                Set<String> methodsToTest_modified = new HashSet<>();
                for (String classname : addedClasses)
                    methodsToTest_added.addAll(v.getMethodsOfClass(classname));
                for (String classname : modifiedClasses)
                    methodsToTest_modified.addAll(v.getMethodsOfClass(classname));

                Set<String> syntacticallyChangedMethods = v.getSyntacticallyChangedMethods();
                Set<String> methodTargets = v.getTargets()[1];

                if (!flat) {
                    System.out.println("\n--------  Version " + v.index + " - " + v.identifier + "    --------");
                    System.out.println("Methods to test (file-level): " + methodsToTest_added.size() + methodsToTest_modified.size());
                    System.out.println("Methods to test (syntax analysis): " + methodsToTest_added.size() + syntacticallyChangedMethods.size());
                    System.out.println("Methods to test (jHisteg targets): " + methodsToTest_added.size() + methodTargets.size());
                } else{
                    fileLevelM+= methodsToTest_added.size() + methodsToTest_modified.size()+"\n";
                    syntaxM+= methodsToTest_added.size() + syntacticallyChangedMethods.size()+"\n";
                    jHistegM+= methodsToTest_added.size() + methodTargets.size()+"\n";
                    version+= v.index+"\n";
                }
            }

            if (flat){
                System.out.println("Methods to test (file-level):");
                System.out.println(fileLevelM);
                System.out.println("Methods to test (syntax analysis):");
                System.out.println(syntaxM);
                System.out.println("Methods to test (jHisteg targets):");
                System.out.println(jHistegM);
                System.out.println("Version:");
                System.out.println(version);
            }

        }
        else for (VersionDirContainer v : versions){
            /** EVOSUITE CALL ADAPTER FUNCTIONALITY */
            Set<String>[] targets = v.getTargets();
            List<String> results = null;

            if (!targets[1].isEmpty()){
                results = EvosuiteAdapter.getEvosuiteCallStringsForMethods(v, targets[1]);
            }

            if(results != null){
                System.out.println("\n--------  Version "+v.index+" - "+v.identifier+"    --------");
                for (String s : results)
                    System.out.println(s);
                System.out.println(" ");
            }
        }

        System.out.println("\n\nExiting...");
    }

    public static String standardizeRelativePaths(String path){
        while (path.startsWith("/"))
            path = path.substring(1);

        while (path.endsWith("/"))
            path = path.substring(0,path.length()-1);

        return path;
    }

    public static String convertToRelativePath(String absolutePath, String baseDir) throws IOException {
        File parent = new File(baseDir);
        File child = new File(absolutePath);

        String s = standardizeRelativePaths(parent.getCanonicalPath());
        String s2 = standardizeRelativePaths(child.getCanonicalPath());

        return standardizeRelativePaths(s2.substring(s.length()+1));
    }


    public static Set<String> convertToFullyQualifiedNames(Set<String> classes, VersionDirContainer v) throws IOException {
        Set<String> result = new HashSet<>();
        for (String c : classes){
            if (c.toLowerCase().endsWith(".class"))
                c = c.substring(0,c.length()-6);
            if (c.toLowerCase().endsWith(".java"))
                c = c.substring(0,c.length()-5);

            File f = new File(v.actualRepo.getAbsoluteFile()+"/"+standardizeRelativePaths(c).replace(".","/"));

            String relativePath = convertToRelativePath(f.getAbsolutePath(), v.sources.getAbsolutePath());
            result.add(relativePath.replace("/","."));
        }
        return result;
    }
}
