package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.special;

import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserver;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.File;

/**
 *  This instrumenter serves a single purpose, to instrument the 'CoverageObserver' class with
 *  execution-specific data, mainly where to export the output to. Do not use it on any other classes!!!
 */
public class CoverageObserverInstrumenter extends ClassVisitor{

   private final String replacementForTraceFileValue;

   public CoverageObserverInstrumenter(int api, ClassWriter writer, File fileToFill) {
      super(api, writer);
      this.replacementForTraceFileValue = fileToFill.getAbsolutePath();
   }

   @Override
   public MethodVisitor visitMethod(int access, String methodName, String desc,
         String signature, String[] exceptions) {   
      MethodVisitor methodVisitor = super.visitMethod(access, methodName, desc, signature, exceptions);
      MethodVisitor instrumenter = new MethodVisitor(api,methodVisitor) {

         @Override
         public void visitLdcInsn(Object cst) {
            //replace the target directory to store output!
            if (cst.equals(CoverageObserver.ldcValueToReplace))
               super.visitLdcInsn(replacementForTraceFileValue);
            else super.visitLdcInsn(cst);
         }
         
      };
      return instrumenter;
   }
}

