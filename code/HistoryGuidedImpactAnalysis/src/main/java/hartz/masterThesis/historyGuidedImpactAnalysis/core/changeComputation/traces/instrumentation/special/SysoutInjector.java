package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.special;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
/**
 * Exists only for debugging purposes, not actively used in the source code...
 */
public class SysoutInjector extends MethodVisitor{

   private final String sysoutValue;

   public SysoutInjector(int api, MethodVisitor methodVisitor, String sysoutValue) {
      super(api, methodVisitor);
      this.sysoutValue = sysoutValue;
   }

   @Override
   public void visitCode() {
      super.visitCode();
      /* Since all non-overwritten methods are delegated to the writer, 'visiting' something means 'writing' code!.
       * Here, I am loading two constants and invoking a method with these! */
      
      super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
      super.visitLdcInsn(sysoutValue);
      super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream",
            "println", "(Ljava/lang/String;)V", false /* sysout is not defined in an interface! */);
   }


   
}