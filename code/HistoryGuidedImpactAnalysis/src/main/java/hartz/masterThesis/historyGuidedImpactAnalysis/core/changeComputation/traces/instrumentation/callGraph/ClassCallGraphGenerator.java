package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.List;

/** Special instrumenter that does not instrument anything but only goes through source code to create a call graph !*/
public class ClassCallGraphGenerator extends ClassVisitor{

   private final String fullyQualifiedClassName;
   private final HashMap<String, List<String>> callGraph;

   public ClassCallGraphGenerator(int api, String fullyQualifiedClassName, HashMap<String, List<String>> callGraph) {
      super(api);
      this.fullyQualifiedClassName = fullyQualifiedClassName;
      this.callGraph = callGraph;
   }

   @Override
   public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
      MethodVisitor methodVisitor = super.visitMethod(access, methodName, methodDescriptor, signature, exceptions);

      String identifierOfMethod = fullyQualifiedClassName+"."+methodName+methodDescriptor;
      return new MethodCallGraphGenerator(api, methodVisitor, identifierOfMethod, callGraph);
   }
}

