package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.onTarget;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Instruments classes so that they observe and store their own execution. Injects functionality that requires
 * some classes of our own, these must be added to the subject's classpath later.
 */
public class ClassInstrumenter extends ClassVisitor{
   /*
    * This class will delegate all 'read' things to its writer, basically just writing everything
    * again just as it has read this stuff. The exception are methods of course, which are handled
    * by another custom pair of wrapped reader and writer.
    */

   private final int asmAPI;
   private final String className;
   private final HashSet<String> methodsToIgnore;

   private final HashMap<String, ArrayList<String>> methodSourceCode;
   private final HashMap<String, HashMap<Integer, Integer>> methodJavaLinesToBytecodeInstr;
   private final HashMap<String, Integer> maxJavaLines;

    /**
     * Creates a new class instrumenter that will add code to make the subject class
     * observe and register its own execution. See the thesis for more information.
     *
     * @param api : ASM api verison
     * @param writer : class writer to use
     * @param fullyQualifiedClassName : fully qualified name of the class that is
     *                                  being instrumented
     * @param methodsToIgnore : signatures of methods to skip
     * @param methodSourceCodeContainer : container to store encountered source code in,
     *                                    can be set to 'null' to disable this feature
     * @param lineMappingContainer : container to store encountered jave line lables in,
     *                               can be set to 'null' to disable this feature
     * @param maxJavaLinesContainer : container to store the maximum line label
     *                                encountered in, can be set to 'null' to disable
     *                                this feature
     */
   public ClassInstrumenter(int api, ClassWriter writer, String fullyQualifiedClassName, HashSet<String> methodsToIgnore,
                            HashMap<String, ArrayList<String>> methodSourceCodeContainer,
                            HashMap<String, HashMap<Integer, Integer>> lineMappingContainer,
                            HashMap<String, Integer> maxJavaLinesContainer) {
      super(api, writer);
      this.asmAPI = api;
      this.className = fullyQualifiedClassName;
      this.methodsToIgnore = methodsToIgnore;
      this.methodSourceCode = methodSourceCodeContainer;
      this.methodJavaLinesToBytecodeInstr = lineMappingContainer;
      this.maxJavaLines = maxJavaLinesContainer;
      assert( //either the feature is disabled or enabled but no hybrid setting may be used!
              (methodSourceCode != null && methodJavaLinesToBytecodeInstr != null) ||
              (methodSourceCode == null && methodJavaLinesToBytecodeInstr == null)
      );
   }

   @Override
   public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {   
      MethodVisitor methodVisitor = super.visitMethod(access, methodName, methodDescriptor, signature, exceptions);    
      boolean isStatic = ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC); //this works, tested by the singleThread Test!
      MethodInstrumenter instrumenter = new MethodInstrumenter(asmAPI, isStatic, methodVisitor, className, methodDescriptor, methodName,
              methodSourceCode, methodJavaLinesToBytecodeInstr, maxJavaLines);
      if (methodsToIgnore.contains(instrumenter.getSignatureOfMethod()))
         return methodVisitor; //we need to ignore it, return the standard visitor after all
      else return instrumenter; //instrument it!
   }
}

