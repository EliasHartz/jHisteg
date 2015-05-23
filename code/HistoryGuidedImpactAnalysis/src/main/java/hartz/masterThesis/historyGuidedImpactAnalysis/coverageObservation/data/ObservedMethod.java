package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserver;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.util.Arrays;

/**
 * Special data container for storing a method's execution during actual runtime of the instrumented project.
 */
public class ObservedMethod {

   private final String methodIdentifier;
   private final ObservedMethod caller; //may be null
   private final Object[] parameters; //may be null
   private Object returnValue;

   /* Used to store coverage data, sort of a manual array-list or buffer implementation to reduce memory footprint.
    * We store the index of the bytecode operation in the method. Note that we might also encounter entries with
    * Integer-Min as value, this encodes that an instrumented function call was registered here! */
   private int[] bytecodeExecuted;
   private int[] opcodesOfExecutedBytecode;
   private int writeBytecodeAtIndex;

   private ObservedMethod[] calledMethods;
   private int writeCallIndex;

   private static final int initialArraySize = 5; //initial size of the any of the arrays
   private static int entryPointCounter = 0;


   /**
    * Constructs a new container.
    * 
    * @param methodIdentifier : identifier of the method aka 'ClassName.FullMethodSignature'
    * @param caller : who called this method, may be 'null'
    * @param parameters : parameters of this method, may be empty!
    */
   public ObservedMethod(String methodIdentifier, ObservedMethod caller, Object[] parameters) {
      assert(methodIdentifier != null && !methodIdentifier.isEmpty());
      assert(parameters != null);

      this.methodIdentifier = methodIdentifier;
      this.caller = caller; 
      this.parameters = parameters;

      returnValue = NotReturnedYetSingleton.getInstance(); //when called, this method has not returned yet!

      //and no code has been executed thus far, chence
      this.bytecodeExecuted = new int[initialArraySize];
      this.opcodesOfExecutedBytecode = new int[initialArraySize];
      Arrays.fill(bytecodeExecuted, -1 /* default value, encodes EMPTY (not method called, that is Integer.Min) */);
      writeBytecodeAtIndex = 0;

      //and nothing was called so far
      this.calledMethods = new ObservedMethod[initialArraySize];
      writeCallIndex = 0;
   }

   @Override
   public String toString(){
      return methodIdentifier;
   }

   public static void resetTraceIndexCounter(){
      entryPointCounter = 0;
   }

   /**
    * Stores that another method has just been called from this method.
    * Needs to be stored in this instance to get the order of calls right as just
    * remembering the caller contains no data about the ordering of calls.
    * 
    * @param m : the called method (as ObservedMethod)
    */
   public synchronized void storeCallToMethod(ObservedMethod m){
      expandCalledMethodsArray();
      calledMethods[writeCallIndex] = m;
      ++writeCallIndex;

      //Integer-Min encodes a function call to an instrumented method!
      updateCoverage(Integer.MIN_VALUE, Integer.MIN_VALUE);
   }

   /**
    * Stores that a statement was just executed.
    * 
    * @param opcode : opcode of the statement
    * @param bytecodeLine : the index of this bytecode command in the method
    */
   public void updateCoverage(int opcode, int bytecodeLine) {
      expandBytecodeIndexArray();
      opcodesOfExecutedBytecode[writeBytecodeAtIndex] = opcode;
      bytecodeExecuted[writeBytecodeAtIndex] = bytecodeLine;
      ++writeBytecodeAtIndex;
   }

   /**
    * Transforms this data collection into an exportable JSON object.
    * 
    * @return the content as JSON
    */
   public synchronized JSONObject toJSON(){
      JSONObject thisMethod =  new JSONObject();

      if (caller == null){
         //it is an entry point
         thisMethod.put("traceIndex", ++entryPointCounter);
      }

      //store the method name first, scan for constructors to make them look nicer
      String s;
      if (methodIdentifier.contains("<init>"))
         s="CONSTRUCTOR";
      else if (methodIdentifier.contains("<cinit>"))
         s="STATIC_INITIALIZER";
      else s="METHOD";
      thisMethod.put("type", s);
      thisMethod.put("called", methodIdentifier);

      //store the parameters next, use a JSON array to keep the order intact!
      JSONArray parametersArray = new JSONArray();
      for (int i = 0; i<parameters.length; ++i){
         parametersArray.put(CoverageObserver.stringifyObjectForExport(parameters[i]));
      }
      thisMethod.put("calledWithParameters", parametersArray);


      //store actual execution data!
      int calledFunctionIndex = 0;
      JSONArray coverageData = new JSONArray();

      for (int i = 0; i<bytecodeExecuted.length; ++i){
         int indexOfInstr = bytecodeExecuted[i];
         if (indexOfInstr == -1) break; //no more data
         coverageData.put( indexOfInstr == Integer.MIN_VALUE
               ? calledMethods[calledFunctionIndex++].toJSON()//call to a function
                     : "["+indexOfInstr+"]> "+ //normal instruction execute
                 OpcodeTranslator.getInstructionStr(opcodesOfExecutedBytecode[i]));
      }
      thisMethod.put("trace", coverageData);

      //finally, store the returned value!
      thisMethod.put("returned", CoverageObserver.stringifyObjectForExport(returnValue));
      return thisMethod;
   }

   /**
    * Stores that this method has just returned. The returned object is being stored
    * as well, a Void Singleton is used in case this method does not return anything.
    * 
    * @param o : returned object
    */
   public synchronized void storeReturnValue(Object o) {
      assert(o != null);

      if (returnValue != NotReturnedYetSingleton.getInstance()){
         /* This should not happen of course, it means the observer has an internal error!
          * We silently ignore those at the moment, making only a debug print...  
          * 
          * This problem did not occur thus far, which is good ;-)  */
         Tool.printDebug("tried to set return value twice for "+methodIdentifier+
               ", old value was : '"+CoverageObserver.stringifyObjectForExport(returnValue)+
               "', new value was : '"+CoverageObserver.stringifyObjectForExport(o)+"'");
      }
      returnValue = o;
   }

   /**
    * Special equals()-method, comparing the identifiers directly. Used as an additional\
    * check during coverage, since Thread IDs are not completely reliable!
    * 
    * @param otherIdentifier : other method identifier to compare against
    * @return 'true' if this method data instance has the same identifier
    */
   public boolean matchesIdentifier(String otherIdentifier) {
      return methodIdentifier.equals(otherIdentifier);
   }

   /**
    * @return data container which called this method
    */
   public ObservedMethod getCaller() {
      assert(wasCalledFromInstrumentedCode());
      return caller;
   }

   /**
    * @return 'true' if another instrumented method was responsible for calling this method
    */
   public boolean wasCalledFromInstrumentedCode() {
      return caller!=null;
   }

   /**
    * @return identifier of the method aka 'ClassName.FullMethodSignature'
    */
   public String getIdentifier() {
      return methodIdentifier;
   }

   /**
    *  This method basically implements an array-list's expand operation (manually) for
    *  the called methods array. Does nothing if the index is still inside the boundaries
    *  of the array.
    *  While this could be solved by a generic method, there is a slight speed advantage
    *  when accessing the parameters directly and I really do not want to increase the
    *  costs of coverage even further...
    */
   private void expandCalledMethodsArray(){
      if (writeCallIndex >= calledMethods.length){
         ObservedMethod[] doubleTheSize = new ObservedMethod[calledMethods.length*2];
         System.arraycopy(calledMethods, 0, doubleTheSize, 0, calledMethods.length);
         calledMethods = doubleTheSize;
      }
   }

   /**
    *  This method basically implements an array-list's expand operation (manually) for
    *  the bytecode index array as well as the opcode storage array. Does nothing if
    *  the index is still inside the boundaries of the array.
    *  While this could be solved by a generic method, there is a slight speed advantage
    *  when accessing the parameters directly and I really do not want to increase the
    *  costs of coverage even further...
    */
   private void expandBytecodeIndexArray(){
      if (writeBytecodeAtIndex >= bytecodeExecuted.length){
         int[] doubleTheSize = new int[bytecodeExecuted.length*2];
         System.arraycopy(bytecodeExecuted, 0, doubleTheSize, 0, bytecodeExecuted.length);
         Arrays.fill(doubleTheSize, bytecodeExecuted.length, doubleTheSize.length, -1);
         bytecodeExecuted = doubleTheSize;
         doubleTheSize = new int[bytecodeExecuted.length];
         System.arraycopy(opcodesOfExecutedBytecode, 0, doubleTheSize, 0, opcodesOfExecutedBytecode.length);
         //no need to fill the rest, we do -1 checks only on the first array
         opcodesOfExecutedBytecode = doubleTheSize;
      }
   }
}
