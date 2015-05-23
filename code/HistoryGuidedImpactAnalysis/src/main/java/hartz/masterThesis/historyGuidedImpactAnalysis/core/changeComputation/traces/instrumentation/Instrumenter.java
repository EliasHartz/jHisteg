package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph.ClassCallGraphGenerator;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.onTarget.ClassInstrumenter;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.onTarget.ClassWriterWithCustomLoader;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.special.CoverageObserverInstrumenter;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.MemorizingClassLoader;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
/** Class implementing functionality to instrument subject classes to make them register and export execution traces */
public class Instrumenter {

   /**
    * Debug method, when called on a class, the Instrumenter will instrument it in-place
    * (hence overwriting the original code!)
    *
    * @param args : first argument is the classpath, second the fully qualified class name
    *               and the third is interpreted as a file where the observed data is to be
    *               stored upon execution of the instrumented class
    */
   public static void main(String[] args) throws IOException{
      if (args.length<3)
         throw new IllegalArgumentException("Expecting at least three arguments: The classpath for loader,"+
                 " the fully qualified name of the class to instrument and an absolute path to a file where to store observed coverage data! " +
                 "Any additional arguments are methods to be ignored, e.g. 'toString()Ljava/lang/String;'");
      Tool.activateDebugMode();

      File baseDir = new File(args[0]);
      File classFile = new File(baseDir.getAbsolutePath()+"/"+args[1].replace(".", "/")+".class");
      if (!baseDir.exists() || !baseDir.isDirectory())
         throw new IllegalArgumentException("Invalid fully qualified classname argument '"+classFile+"', directory does not exist!");
      if (!classFile.exists() || !classFile.isFile())
         throw new IllegalArgumentException("Invalid fully qualified classname argument '"+classFile+"', file does not exist!");
      File outputTargetDir = new File(args[2]);

      String fullyQualifiedClassName = args[1];
      MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(baseDir);
      InputStream clazzAsStream = loader.loadClassAsStream(fullyQualifiedClassName);

      //read methods to ignore
      HashSet<String> ignoreMethods = new HashSet<>();
      int i = 3;
      while (args.length>i){
         ignoreMethods.add(args[i]);
         ++i;
      }

      if (clazzAsStream !=null){
         byte[] instrumentedCode = instrumentClass(clazzAsStream, fullyQualifiedClassName, ignoreMethods, loader, false);
         if (instrumentedCode!=null){
            classFile.createNewFile(); //overwrite classfile in-place!
            DataOutputStream dout = new DataOutputStream(new FileOutputStream(classFile));
            dout.write(instrumentedCode);
            dout.close();
            Tool.print(" * instrumented class '"+fullyQualifiedClassName+"'");
         } else Tool.printError("Instrumentation failed for class '" + fullyQualifiedClassName +
                 "' (class on disc has not been changed)");

         FileUtils.clearCodeFromProjectUnderTest(baseDir); //remove previously injected code
         FileUtils.injectCodeIntoProjectUnderTest(baseDir, outputTargetDir);
      }
      else Tool.printError("Instrumentation failed for class '"+fullyQualifiedClassName+"' since it could not be loaded");
   }

   /**
    * Instruments a class out of an Input Stream. The stream WILL BE CLOSED BY THIS METHOD!
    *
    * @param clazzAsStream : fresh input stream on the class that is to be instrumented
    * @param fullyQualifiedClassName : the name of the class that is to be instrumented, must
    *                                  match the contents of the stream or the generated traces
    *                                  will have corrupt output!
    * @param methodsToIgnore : which methods are to be excluded from instrumentation, given by
    *                          name and type descriptior e.g. 'main([Ljava/lang/String;)V'
    * @param loader : class loader which with this class and referenced classes can be loaded
    * @param safeMode : set to 'true' to use a static max-stack increase instead of recomputing!
    * @throws IOException if class cannot be accessed/read
    */
   public static byte[] instrumentClass(InputStream clazzAsStream, String fullyQualifiedClassName,
                                        HashSet<String> methodsToIgnore, ClassLoader loader, boolean safeMode) throws IOException{
      return instrumentClass(clazzAsStream, fullyQualifiedClassName, methodsToIgnore, null, null, null, loader, safeMode);
   }


   /**
    * Instruments a class out of an Input Stream. The stream WILL BE CLOSED BY THIS METHOD!
    *
    * @param clazzAsStream : fresh input stream on the class that is to be instrumented
    * @param fullyQualifiedClassName : the name of the class that is to be instrumented, must
    *                                  match the contents of the stream or the generated traces
    *                                  will have corrupt output!
    * @param methodsToIgnore : which methods are to be excluded from instrumentation, given by
    *                          name and type descriptior e.g. 'main([Ljava/lang/String;)V'
    * @param sourceCodeContainer : will contain the original bytecode for each instrumented
    *                              method when this method returns, accessible via
    *                              classname.methodnameWithTypeDescriptor, e.g.
    *                              'mypackage.otherpack.MyClass.main([Ljava/lang/String;)V'
    * @param lineMappingContainer : will contain a mapping from bytecode instruction indices to
    *                               java line numbers if such a mapping can be derived from the
    *                               compiled source only ( = if there are line labels) for all
    *                               all instrumented methods. Accessible akin to above.
    * @param maxJavaLinesContainer : will contain the highest java line number encountered during
    *                                instrumentation, accessible akin to above
    * @param loader : class loader which with this class and referenced classes can be loaded
    * @param safeMode : set to 'true' to use a static max-stack increase instead of recomputing!
    * @return instrumented code of the entire class!
    * @throws IOException if class cannot be accessed/read
    */
   public static byte[] instrumentClass(InputStream clazzAsStream, String fullyQualifiedClassName,
                                        HashSet<String> methodsToIgnore, HashMap<String, ArrayList<String>> sourceCodeContainer,
                                        HashMap<String, HashMap<Integer, Integer>> lineMappingContainer,
                                        HashMap<String, Integer> maxJavaLinesContainer, ClassLoader loader, boolean safeMode)
           throws IOException{
      ClassReader classReader = new ClassReader(clazzAsStream);
      ClassWriter actualWriter = safeMode ? new ClassWriterWithCustomLoader(0, loader) :
              new ClassWriterWithCustomLoader(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, loader);

      //wrap a standard class writer inside the custom reader we call a 'instrumenter class writer'
      ClassInstrumenter instrumenter = new ClassInstrumenter(Opcodes.ASM5, actualWriter, fullyQualifiedClassName,
              methodsToIgnore, sourceCodeContainer, lineMappingContainer, maxJavaLinesContainer);
      classReader.accept(instrumenter,0);
      clazzAsStream.close();

      try{
         byte[] resultingClassCode = actualWriter.toByteArray();
         return resultingClassCode;
      }
      catch(Exception e){
         /* A single method in a Java class may be at most 64KB of bytecode and instrumentation obviously enlarges
          * present methods. This may be a problem. */
         Tool.printDebug(e);
         Tool.printError("Problem occurred during instrumentation: '"+e.getMessage()+"'");
      }
      return null;
   }

    /**
     * Goes through a class and updates the given call graph with that class' information.
     *
     * @param clazzAsStream : class to be read, stream will be closed by this method
     * @param fullyQualifiedClassName : fully qualified name of the class contained in the
     *                                  stream
     * @param callGraph : call graph to update, is obviously modified by this method
     * @throws IOException : in case stream is broken or invalid
     */
   public static void updateCallGraph(InputStream clazzAsStream, String fullyQualifiedClassName, HashMap<String, List<String>> callGraph) throws IOException {
      ClassCallGraphGenerator callGraphGen = new ClassCallGraphGenerator(Opcodes.ASM5, fullyQualifiedClassName, callGraph);
      ClassReader c = new ClassReader(clazzAsStream);
      c.accept(callGraphGen,0);
      clazzAsStream.close();
   }

   /**
    * Special instrumentation routine intended for the 'CoverageObserver'-class only!
    *
    * @param fullyQualifiedClassName : name of coverage observer class to instrument
    * @param storeWhere : file in which to store the data of this run
    * @return instrumented bytecode of the given class
    */
   public static byte[] instrumentCoverageObserver(String fullyQualifiedClassName, File storeWhere) throws IOException{
      ClassReader classReader = new ClassReader(fullyQualifiedClassName);
      ClassWriter actualWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

      //wrap a standard class writer inside the custom reader we call a 'instrumenter class writer'
      CoverageObserverInstrumenter instrumenter = new CoverageObserverInstrumenter(Opcodes.ASM5, actualWriter, storeWhere);
      classReader.accept(instrumenter,0);
      return actualWriter.toByteArray();
   }

    /**
     * Simply returns the bytecode of a class.
     *
     * @param fullyQualifiedClassName : fully qualified name of the class
     * @return bytecode as found in that class
     * @throws IOException : in case stream is broken or invalid
     */

   public static byte[] getBytecodeOfClass(String fullyQualifiedClassName) throws IOException {
      ClassReader classReader = new ClassReader(fullyQualifiedClassName);
      ClassWriter classWriter = new ClassWriter(classReader, 0);
      classReader.accept(classWriter, 0);
      return classWriter.toByteArray();
   }
}