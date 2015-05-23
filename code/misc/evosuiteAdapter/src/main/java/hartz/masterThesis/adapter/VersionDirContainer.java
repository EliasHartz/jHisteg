package hartz.masterThesis.adapter;

import hartz.masterThesis.adapter.json.JSONArray;
import hartz.masterThesis.adapter.json.JSONObject;

import java.io.*;
import java.util.*;


public class VersionDirContainer implements Comparable<VersionDirContainer> {

    public final int index;
    public final String identifier;
    public final String author;
    public final String msg;
    public final String date;
    public final String comparingAgainst;

    public final File syntaxChanges;
    public final File testingTargets;
    public final File testingTargetsSyntax;
    public final File testingTargetsDivergences;
    public final File fileLevelChanges;
    public final File callGraph;

    public final File mainDir;
    public final File actualRepo;
    public final File additionalNonInstrumented;
    public final File additionalExportedSource;
    public final File nonInstrumented;
    public final File exportedSource;
    public final File sources;
    public final File compiledSources;
    public final File additionalCompiledSources;


    public VersionDirContainer(File mainDir) throws IOException {
        File versionInfo = new File(mainDir.getAbsolutePath()+"/versionInfo");
        if (!versionInfo.exists() || !versionInfo.isFile())
            throw new IOException("'"+mainDir.getAbsolutePath()+"' is not a version directory, 'versionInfo' file is missing!");

        this.mainDir = mainDir;
        this.actualRepo = new File(this.mainDir,"version");
        this.nonInstrumented = new File(this.mainDir.getAbsolutePath()+"/code_before_instrumentation");
        this.exportedSource = new File(this.mainDir.getAbsolutePath()+"/code_under_observation");
        this.additionalNonInstrumented = new File(this.mainDir.getAbsolutePath()+"/code_before_instrumentation_additional");
        this.additionalExportedSource = new File(this.mainDir.getAbsolutePath()+"/code_under_observation_additional");

        fileLevelChanges = new File(this.mainDir,"allFileLevelChanges");
        syntaxChanges = new File(this.mainDir,"syntacticCodeChanges");
        testingTargets = new File(this.mainDir,"testingTargets");
        testingTargetsSyntax = new File(this.mainDir,"testingTargets_syntacticalChanges");
        testingTargetsDivergences = new File(this.mainDir,"testingTargets_traceDivergences");
        callGraph = new File(this.mainDir,"callGraph");

        JSONObject object = new JSONObject(readData(versionInfo));
        this.identifier = object.getString("identifier");
        this.author = object.getString("author");
        this.msg = object.getString("msg");
        this.date = object.getString("date");
        this.index = object.getInt("index");
        this.comparingAgainst = object.optString("comparingAgainst");

        this.sources = object.has("sourcesIn")?new File(object.getString("sourcesIn")): actualRepo;
        this.compiledSources = object.has("compiledSourcesIn")?new File(object.getString("compiledSourcesIn")): actualRepo;
        this.additionalCompiledSources = object.has("additionalCompiledSourcesIn")?new File(object.getString("additionalCompiledSourcesIn")): actualRepo;
    }

    /**
     * @return returns data contained in file as string
     */

    public static String readData(File f) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        char[] buffer = new char[750];

        StringBuilder builder = new StringBuilder();
        int i = reader.read(buffer);
        while(i>=0){
            if (i<buffer.length)
                builder.append(Arrays.copyOfRange(buffer, 0, i));
            else builder.append(buffer);
            i = reader.read(buffer);
        }
        reader.close();

        String s = builder.toString();
        return s;
    }

    /**
     * @return array containing two lists: First holds all class-level targets, second holds method level targets
     */
    public Set[] getTargets() throws IOException {
        if (!testingTargets.exists()) return new Set[]{new HashSet<String>(),new HashSet<String>()};
        JSONArray arr = new JSONArray(readData(testingTargets));
        Set<String> classTargets = new HashSet<>();
        Set<String> methodTargets = new HashSet<>();
        for (int i = 0; i<arr.length(); ++i){
            String targetIdentifier = arr.getJSONObject(i).getString("-> testingTarget");
            if (targetIdentifier.contains("("))
                methodTargets.add(targetIdentifier);
            else classTargets.add(targetIdentifier);
        }
        return new Set[]{classTargets, methodTargets};
    }

    /**
     * @return array containing two sets: First holds added classes, second holds modified classes
     */
    public Set[] getFileLevelChanges() throws IOException {
        if (!fileLevelChanges.exists()) return new Set[]{new HashSet<String>(),new HashSet<String>()};
        JSONArray arr = new JSONArray(readData(fileLevelChanges));
        Set<String> added = new HashSet<>();
        Set<String> modified = new HashSet<>();
        for (int i = 0; i<arr.length(); ++i){
            JSONObject o = arr.getJSONObject(i);
            if (o.optBoolean("partOfSource", false)){
                String classFile = o.getString("pathInRepo");
                if (classFile.startsWith("/"))
                    classFile = classFile.substring(1);
                if (o.getString("type").equals("MODIFIED"))
                    modified.add(classFile.replace("/","."));
                else added.add(classFile.replace("/", "."));
            }
        }
        return new Set[]{added, modified};
    }

    /**
     * @return set of methods syntactically modified in this version
     */
    public Set<String> getSyntacticallyChangedMethods() throws IOException {
        Set<String> methods = new HashSet<>();
        if (testingTargetsSyntax.exists()){
            JSONArray arr = new JSONArray(readData(testingTargetsSyntax));
            for (int i = 0; i<arr.length(); ++i){
                JSONObject o = arr.getJSONObject(i);
                String target = o.getString("-> testingTarget");
                if (target.contains("(")){
                    methods.add(target);
                } //else it is a class-level syntax change
            }
        } else {
            if (!syntaxChanges.exists()) return methods;
            JSONArray arr = new JSONArray(readData(syntaxChanges));
            for (int i = 0; i<arr.length(); ++i){
                JSONObject o = arr.getJSONObject(i);
                String target = o.optString("affectedMethod", null);
                if (target != null){
                    methods.add(target); //since it is a set, no worries about duplicates!
                }
            }
        }

        return methods;
    }

    /**
     * @return set of methods contained in the class
     */
    public Set<String> getMethodsOfClass(String fullyQualifiedClassName) throws IOException {
        if (exportedSource != actualRepo && exportedSource.exists()){
            Set<String> methods = getMethodsOfClassHelper(fullyQualifiedClassName, exportedSource);
            if (!methods.isEmpty()) return methods;
        }

        //check the additional path as well
        if (additionalExportedSource != actualRepo && additionalExportedSource.exists()){
            Set<String> methods = getMethodsOfClassHelper(fullyQualifiedClassName, additionalExportedSource);
            if (!methods.isEmpty()) return methods;
        }

        return new HashSet<>();
    }

    private Set<String> getMethodsOfClassHelper(String fullyQualifiedClassname, File f) throws IOException {

        if (fullyQualifiedClassname.toLowerCase().endsWith(".java"))
            fullyQualifiedClassname = fullyQualifiedClassname.substring(0,fullyQualifiedClassname.length()-5);
        if (fullyQualifiedClassname.toLowerCase().endsWith(".class"))
            fullyQualifiedClassname = fullyQualifiedClassname.substring(0,fullyQualifiedClassname.length()-6);

        final String classname = fullyQualifiedClassname;


        File[] files =  f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().equals(classname);
            }
        });
        if (files.length == 1){
            JSONArray arr = new JSONArray(readData(files[0]));
            Set<String> methods = new HashSet<>();
            for (int i = 0; i<arr.length(); ++i){
                methods.add(arr.getJSONObject(i).getString("method"));
            }
            return methods;
        }
        return new HashSet<>();
    }

    @Override
    public int compareTo(VersionDirContainer versionDirContainer) {
        return index - versionDirContainer.index;
    }
}
