package hartz.masterThesis.adapter.generators.evosuite;

import hartz.masterThesis.adapter.VersionDirContainer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EvosuiteAdapter {
    
    public static List<String> getEvosuiteCallStringsForClasses(VersionDirContainer v, List<String> classIdentifiers) {
        assert(!classIdentifiers.isEmpty());
        LinkedList<String> evosuiteCalls = new LinkedList<>();
        for (String s : classIdentifiers) {
            String evosuiteCall = //call to evosuite
                    "-generateSuite" +
                            " -base_dir " + v.mainDir +
                            " -target " + v.compiledSources +
                            " -class " + s;
            evosuiteCalls.add(evosuiteCall);
        }

        return evosuiteCalls;
    }

    public static List<String> getEvosuiteCallStringsForMethods(VersionDirContainer v, Set<String> methodIdentifiers) {
        assert(!methodIdentifiers.isEmpty());
        LinkedList<String> evosuiteCalls = new LinkedList<>();

        HashMap<String, List<String>> classToMethods = new HashMap<>();
        for (String s : methodIdentifiers) {
            String className = s.substring(0,s.indexOf("(")).substring(0,s.lastIndexOf("."));
            String methodName = s.substring(className.length()+1);

            if (classToMethods.containsKey(className)) {
                classToMethods.get(className).add(methodName);
            } else {
                List<String> l = new LinkedList<>();
                l.add(methodName);
                classToMethods.put(className,l);
            }
        }

        for (String clazz : classToMethods.keySet()){
            String methodStr = "";
            for (String s : classToMethods.get(clazz)) {
                methodStr=methodStr+s+":";
            }
            methodStr = "\""+methodStr.substring(0,methodStr.length()-1)+"\"";

            String evosuiteCall = //call to evosuite
                    "-generateSuite" +
                            " -base_dir " + v.mainDir.getAbsolutePath() +
                            " -target " + v.compiledSources.getAbsolutePath() +
                            " -class " + clazz +
                            " -Dtarget_method_list="+ methodStr;
            evosuiteCalls.add(evosuiteCall);
        }
        return evosuiteCalls;
    }
}
