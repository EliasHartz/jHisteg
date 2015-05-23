package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation;

import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;


public class CoverageObserver {

    private static CoverageObserver instance; //this is a Singleton class!

    public final static String coverageObserverClassDots = CoverageObserver.class.getName();
    public final static String coverageObserverClass = coverageObserverClassDots.replace(".", "/");
    public final static String coverageObserverPackageStructure = coverageObserverClass.substring(0,coverageObserverClass.lastIndexOf("/"))+"/data/json";

    public static final String ldcValueToReplace = "!!!REPLACE-ME-INSTRUMENTER!!!"; //at runtime, the resulting LDC loads something different!
    public static File exportToFile = new File("!!!REPLACE-ME-INSTRUMENTER!!!"); /* this construction looks stupid but makes instrumentation

   /* Stores all errors that occur inside this class and its dependencies during observation */
    protected final LinkedList<String> errors;

    /*
     * This is obviously not perfect because it will NOT deal correctly with applications that
     * spawn numerous threads (since thread IDs will be re-used at some point). It might fail on occasion...
     * Still, its a hell-of-a-lot better than some other solutions out there ;-)
     */
    protected final HashMap<Long, ObservedMethod> currentMethods;

    /* Might get long if the project-under-test uses a lot of Threads. Exceptions also lead to funny trees of course! */
    protected final LinkedList<ObservedMethod> callTree;

    protected static boolean stringifyInProgress = false;
   /* During object-stringification, functions like 'hashCode()' and 'toString()' are used and would thus modify the
    * callTree or currentMethods with nonsense data if implemented by the instrumented class (and that data would
    * only exist because of the observation, no relationship with the programmer's coding decisions exists) */


    protected CoverageObserver(){
        currentMethods = new HashMap<Long, ObservedMethod>();
        errors = new LinkedList<String>();
        callTree = new LinkedList<ObservedMethod> ();
    }

    public synchronized static CoverageObserver getCurrentInstance(){
        CoverageObserver c = null;
        if (CoverageObserverNoHook.noHookInstance != null)
            c = CoverageObserverNoHook.noHookInstance;
        else if (instance == null){
            instance = new CoverageObserver();
            //make sure that we report our findings AFTER execution of the program under test has ended...
            Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
            c = instance;
        }
        else c = instance;

        return c;
    }

    @Override
    public final Object clone() {
        return getCurrentInstance();
    }

    public synchronized void enterNewMethod(String fullMethodIdentifier, Object[] parameters, Thread t){
        if (stringifyInProgress) return;

        //replace 'null'-values with NullSingleton!
        Object[] parametersToStore = new Object[parameters.length];
        for (int i = 0; i<parameters.length; ++i) {
            if (parameters[i] == null) parametersToStore[i] = NullSingleton.getInstance();
            else parametersToStore[i] = parameters[i];
        }

        long id = t.getId();
        if(currentMethods.containsKey(id) /* = we observed the caller */){
            ObservedMethod enteredMethod = new ObservedMethod(fullMethodIdentifier, currentMethods.get(id), parametersToStore);
            ObservedMethod callingMethod = currentMethods.put(id, enteredMethod);
            callingMethod.storeCallToMethod(enteredMethod);
        }
        else{
            //there was no previous caller, this should be the main or some JUnit test method!
            ObservedMethod enteredMethod = new ObservedMethod(fullMethodIdentifier, null, parametersToStore);
            currentMethods.put(t.getId(), enteredMethod);
            callTree.add(enteredMethod); //its the start of a new execution
        }
    }

    public synchronized void storeExecutedInstruction(Thread t, String fullMethodIdentifier, int opcode, int bytecodeIndex){
        if (stringifyInProgress) return;

        ObservedMethod currentMethod = currentMethods.get(t.getId());
        if (currentMethod != null && currentMethod.matchesIdentifier(fullMethodIdentifier))
            currentMethod.updateCoverage(opcode, bytecodeIndex);
        else
            errors.add("UNKNOWN STATE: Failed to detect to which thread was executing instrumented method '"+fullMethodIdentifier+
                    "' (bytecode index "+ bytecodeIndex+", instruction '"+OpcodeTranslator.getInstructionStr(opcode)+"'!"+
                    " Callstack observation suggested that thread "+t+" is not in any observed method at the moment."+
                    " Coverage result are likely to be corrupted!");
    }


    public synchronized void returnFromMethodWithObject(Object returnValue, Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;

        Object returnValueToStore = returnValue == null ? NullSingleton.getInstance() : returnValue;

        ObservedMethod returningMethod = currentMethods.get(t.getId());
        if (returningMethod == null){
            errors.add("UNKNOWN RETURN: Failed to detect to which thread was executing instrumented method '"+fullMethodIdentifier+"' returning '"+stringifyObjectForExport(returnValueToStore).toString(1)+
                    "', thread "+t+" is now in unknown state and future results may be corrupted! Callstack observation suggested that thread is not in any observed method at the moment. "+
                    "Coverage result are likely to be corrupted!");
            return;
        }


        //thread was observed to be in a method, thats good!
        if (returningMethod.matchesIdentifier(fullMethodIdentifier)){
            returningMethod.storeReturnValue(returnValueToStore);

            if (returningMethod.wasCalledFromInstrumentedCode())
                //we have returned, replace the currently active method!
                currentMethods.put(t.getId(), returningMethod.getCaller());
            else{
                //we are at the top-of-observed-callstack, therefore the stack is now empty!
                currentMethods.remove(t.getId());
            }
        }
        else{
            errors.add("UNKNOWN STATE: Failed to detect to which thread was just retunred from instrumented method '"+fullMethodIdentifier+
                    "with '"+stringifyObjectForExport(returnValueToStore).toString(1)+"! Callstack suggested that "+t+" should be in "+returningMethod.getIdentifier()+"!");
        }

    }



    public synchronized void returnFromMethodWithVoid(Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(VoidSingleton.getInstance(),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithBoolean(boolean b ,Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedBoolean(b),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithInteger(int i ,Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedInteger(i),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithByte(byte b ,Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedByte(b),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithShort(short s ,Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedShort(s),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithChar(char c ,Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedChar(c),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithFloat(float f, Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedFloat(f),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithLong(long l ,Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedLong(l),t,fullMethodIdentifier);
    }

    public synchronized void returnFromMethodWithDouble(double d, Thread t, String fullMethodIdentifier){
        if (stringifyInProgress) return;
        returnFromMethodWithObject(new NonBoxedDouble(d),t,fullMethodIdentifier);
    }

    public synchronized void export(){
        if (!currentMethods.isEmpty()){
            errors.add("Execution Trace Data Export to file was requested even though not all methods have returned! Method(s) still on callstack:");
            for (ObservedMethod c : currentMethods.values()){
                errors.add("  - "+c.getIdentifier());
            }
        }

        JSONObject toExport = new JSONObject();
        JSONArray callTreeJSON = new JSONArray();
        for (ObservedMethod c : callTree){
            callTreeJSON.put(c.toJSON());
        }

        toExport.put("executionTraces", callTreeJSON);
        if (!errors.isEmpty())
            toExport.put("errors", new JSONArray(errors));

        try{
         /* Although the idea is that is method is only called once and only one file is produced, it may be different
          * in practice. E.g. ANT will execute all tests of a project in separate JVMs, hence there will be multiple
          * calls to export. Since a static counter will not work in this case, the following code is necessary: */
            File f = null;
            if (exportToFile.exists()){
                int i = 1;
                f = new File(exportToFile.getAbsolutePath()+"_"+i);
                while (f.exists())
                    f = new File(exportToFile.getAbsolutePath()+"_"+(++i));
            } else f = exportToFile;


            f.getParentFile().mkdirs(); //directory should in fact exist in most cases by construction already

            f.createNewFile(); //overwrite previous data without warning
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write(toExport.toString(Globals.jsonIndentFactor));//store as JSON
            writer.close();
        }
        catch(IOException e){
            e.printStackTrace();
            System.err.println("Export of execution traces failed! Please create the file yourself to continue!");
            System.err.println("Printing results as JSON to console for manual file-creation by user:");
            System.err.println("#########-----------------   BEGIN   --------------------------#########");
            System.err.print(toExport.toString(Globals.jsonIndentFactor));
            System.err.println("#########-----------------    END    --------------------------#########");
        }

        //exported all present data, clear for potential next call (if there is any)
        callTree.clear();
        errors.clear();
        currentMethods.clear();
        ObservedMethod.resetTraceIndexCounter(); //need to reset in case there is a next execution (see long comment above)
    }

    public synchronized static JSONObject stringifyObjectForExport(Object objectToStringify){
        assert(objectToStringify != null); //we want null-singletons in this case!
        stringifyInProgress = true;

        //store some values we use to identify instances if there is a proper implementation!
        Class<?> classOfToString;
        Class<?> classOfHashCode;
        try{
            classOfToString = objectToStringify.getClass().getMethod("toString").getDeclaringClass();
            classOfHashCode = objectToStringify.getClass().getMethod("hashCode").getDeclaringClass();
        } catch (NoSuchMethodException e){
            assert (false);  /* return incomplete object, should actually never ever happen! */
            stringifyInProgress = false;
            return new JSONObject();
        }

        //store class name
        JSONObject result = new JSONObject();
        try {
            if (objectToStringify instanceof NonBoxed) {
                String className = ((NonBoxed) objectToStringify).getTypeString();
                result.put("class", className);
                result.put("value",((NonBoxed) objectToStringify).getValueAsString());
            }
            else if (objectToStringify instanceof SpecialReturnValue){
                result.put("stringRepresentation", objectToStringify.toString());
                //no hash code or class!
            } else{
                result.put("class", objectToStringify.getClass().getCanonicalName());
                result.put("stringRepresentation", classOfToString.getCanonicalName().equals(Object.class.getCanonicalName()) ? "toString() of object" : objectToStringify.toString());
                result.put("hashCode", classOfHashCode.getCanonicalName().equals(Object.class.getCanonicalName()) ? "hashCode() of object" : objectToStringify.hashCode());
            }
        }  catch (Throwable t){
            /* programmer has implemented something that crashes or is concurrently modified, cannot determine stuff for this... */
            if (!result.has("class")) result.put("class","!-could not be determined -!");
            if (!result.has("stringRepresentation")) result.put("stringRepresentation","!-could not be determined -!");
            if (!result.has("hashCode")) result.put("hashCode","!-could not be determined -!");
        }

        stringifyInProgress = false;
        return result;
    }

}

