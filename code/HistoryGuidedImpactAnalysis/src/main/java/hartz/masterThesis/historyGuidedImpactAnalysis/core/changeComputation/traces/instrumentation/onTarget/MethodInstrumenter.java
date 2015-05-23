package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.onTarget;

import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserver;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.OpcodeTranslator;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Instruments methods so that they observe and store their own execution. This instrumenter injects
 * functionality that requires some classes of our own, these must be added to the subject's classpath
 * later on.
 * Note that if those classes are modified, you need to adapt this visitor as well!
 */
public class MethodInstrumenter extends MethodVisitor{
   /*
    * This class uses the writer to write everything it just read. The exception is the visitCode()
    * method where we write some stuff ourselves, effectively adding code!
    */

    private final String methodName;
    private final String methodDescriptor;
    private final String className;
    private final String identifierOfMethod;
    private final boolean isStatic;
    private final Type[] parameterTypes;

    private final ArrayList<String> sourceCode;
    private final HashMap<Integer, Integer> bytecodeIndexToJavaLineMapping;
    private final HashMap<String, Integer> maxJavaLineEncountered;

    private final int maxStackIncreaser = 12;

    private int bytecodeInstructionCounter;
    private int javaLineNumber; //only used if there are debug line number statements

    public MethodInstrumenter(int api, boolean isStatic, MethodVisitor methodVisitor, String fullyQualifiedClassName, String methodDescriptor,
                              String methodName, HashMap<String, ArrayList<String>> methodSourceCodeContainer,
                              HashMap<String, HashMap<Integer, Integer>> lineMappingContainer,
                              HashMap<String, Integer> maxJavaLinesContainer) {
        super(api, methodVisitor);
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.className = fullyQualifiedClassName;
        this.identifierOfMethod = fullyQualifiedClassName+"."+methodName+methodDescriptor;
        this.parameterTypes = Type.getArgumentTypes(methodDescriptor);
        this.isStatic = isStatic;
        this.javaLineNumber = -1; //not all classes come with line labels

        if (methodSourceCodeContainer != null){
            /* Store source code */
            assert(lineMappingContainer != null);
            assert(maxJavaLinesContainer != null);
            this.sourceCode = new ArrayList<>();
            this.bytecodeIndexToJavaLineMapping = new HashMap<>();
            methodSourceCodeContainer.put(identifierOfMethod, this.sourceCode);
            lineMappingContainer.put(identifierOfMethod, this.bytecodeIndexToJavaLineMapping);
            maxJavaLineEncountered = maxJavaLinesContainer;
        } else {
            /* Do not store source code! */
            assert(lineMappingContainer == null);
            assert(maxJavaLinesContainer == null);
            this.sourceCode = null;
            this.bytecodeIndexToJavaLineMapping = null;
            maxJavaLineEncountered = null;
        }

        bytecodeInstructionCounter = 0;
    }

    public String getSignatureOfMethod(){
        return methodName+methodDescriptor;
    }


    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        javaLineNumber = line; //update current line number
    }

    @Override
    public void visitCode() {
      /* Since all non-overwritten methods are delegated to the writer, 'visiting' something in super()
       * in any of these methods basically means 'writing' new code here! Gotta love ASM...   */

        //prepare the call to our coverage method, get an instance of the observer first!
        super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);//instance to call for

        super.visitLdcInsn(identifierOfMethod); //first parameter of coverage method call!

      /* Second parameter is more difficult, its an Object-Array holding the method's parameters! */
        super.visitLdcInsn(parameterTypes.length); //how large the array should be
        super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object"); //will leave a reference to the new array on the stack

        //if there are parameters, we need to fill the array now!
        if (parameterTypes.length != 0){
            int storeAtPosInArray = 0;
            for (Type type : parameterTypes /* which is sadly not an enum in ASM for some stupid reason :-( */) {

                super.visitInsn(Opcodes.DUP); //duplicate value reference to the array currently on the stack
                super.visitLdcInsn(storeAtPosInArray); //this is position to store the parameter in the array!

                int loadParam = (isStatic?storeAtPosInArray:storeAtPosInArray+1); //non-static methods have *this* as first parameter!
            /* One could of course distinguish between the instances as well, but at the moment I consider this to be too
             * much of an overhead for too little gain, after all it will probably be a different one for each execution. */

            /* Next, get the object to store on the stack! We box all the primitives in my non-boxed
             * classes to remember that they were primitives! */
                if (type.equals(Type.INT_TYPE)) {
                    super.visitVarInsn(Opcodes.ILOAD, loadParam);
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC, //static-access is just easier to construct than the whole new-object-init calls...
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedInteger",
                            "staticAccessToConstructor",
                            "(I)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedInteger;",
                            false
                    );
                }
                else if (type.equals(Type.BOOLEAN_TYPE)) {
                    super.visitVarInsn(Opcodes.ILOAD, loadParam); //booleans are encoded in 32-bit integers
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedBoolean",
                            "staticAccessToConstructor",
                            "(Z)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedBoolean;",
                            false
                    );
                }
                else if (type.equals(Type.BYTE_TYPE)) {
                    super.visitVarInsn(Opcodes.ILOAD, loadParam); //booleans are encoded in 32-bit integers
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedByte",
                            "staticAccessToConstructor",
                            "(B)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedByte;",
                            false
                    );
                }
                else if (type.equals(Type.CHAR_TYPE)) {
                    super.visitVarInsn(Opcodes.ILOAD, loadParam); //booleans are encoded in 32-bit integers
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedChar",
                            "staticAccessToConstructor",
                            "(C)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedChar;",
                            false
                    );
                }
                else if (type.equals(Type.SHORT_TYPE)) {
                    super.visitVarInsn(Opcodes.ILOAD, loadParam); //booleans are encoded in 32-bit integers
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedShort",
                            "staticAccessToConstructor",
                            "(S)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedShort;",
                            false
                    );
                }
                else if (type.equals(Type.FLOAT_TYPE)) {
                    super.visitVarInsn(Opcodes.FLOAD, loadParam);
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedFloat",
                            "staticAccessToConstructor",
                            "(F)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedFloat;",
                            false
                    );
                }
                else if (type.equals(Type.LONG_TYPE)) {
                    super.visitVarInsn(Opcodes.LLOAD, loadParam);
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedLong",
                            "staticAccessToConstructor",
                            "(J)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedLong;",
                            false
                    );
                }
                else if (type.equals(Type.DOUBLE_TYPE)) {
                    super.visitVarInsn(Opcodes.DLOAD, loadParam);
                    super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "hartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedDouble",
                            "staticAccessToConstructor",
                            "(D)Lhartz/masterThesis/historyGuidedImpactAnalysis/coverageObservation/data/NonBoxedDouble;",
                            false
                    );
                }
                else super.visitVarInsn(Opcodes.ALOAD, loadParam); //load an object

                super.visitInsn(Opcodes.AASTORE);//store parameter in the array
                ++storeAtPosInArray;
            }
        }

        //finally, load the third parameter for our coverage method call!
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()L" + "java/lang/Thread" +";", false);

        //call the observer method with the parameters on the stack
        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                CoverageObserver.coverageObserverClass,
                "enterNewMethod",
                "(Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Thread;)V",
                false /* my method is not an interface */);

        super.visitCode();
    }

    private void registerLine(int opcode){
        super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()L" + "java/lang/Thread" +";", false);//first parameter!
        super.visitLdcInsn(identifierOfMethod);//second parameter!
        super.visitLdcInsn(opcode); //third parameter!
        super.visitLdcInsn(bytecodeInstructionCounter); //fourth parameter!
        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                "storeExecutedInstruction", "(Ljava/lang/Thread;Ljava/lang/String;II)V", false);

        //source code exportation feature can be disabled, in which case the variable is 'null'
        if (sourceCode != null) {
            assert(bytecodeIndexToJavaLineMapping != null);
            assert(maxJavaLineEncountered != null);

            //store the source for later export
            sourceCode.add(OpcodeTranslator.getInstructionStr(opcode));
            if (javaLineNumber != -1) {
                //store the mapping from java line numbers to bytecode instructions if available
                bytecodeIndexToJavaLineMapping.put(bytecodeInstructionCounter, javaLineNumber);
                if (maxJavaLineEncountered.containsKey(identifierOfMethod)) {
                    if (maxJavaLineEncountered.get(identifierOfMethod) < javaLineNumber) {
                        int i = maxJavaLineEncountered.put(identifierOfMethod, javaLineNumber);
                        assert(i < javaLineNumber); //we must have overwritten something!
                    }
                } else maxJavaLineEncountered.put(identifierOfMethod, javaLineNumber);
            }
        }

        ++bytecodeInstructionCounter; //increase the counter
    }


    @Override
    /**
     * ZERO-OPERAND INSTRUCTIONS!
     */
    public void visitInsn(int opcode) {
        registerLine(opcode); //line must be registered as well of course!

        //instrument all types of returns:
        switch(opcode){
            case Opcodes.IRETURN :{
                super.visitInsn(Opcodes.DUP); //duplicate value that is being returned!
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                        "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);//instance to call for
                super.visitInsn(Opcodes.SWAP); //swap instance with duplicate to get first parameter!
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                        "()L" + "java/lang/Thread" +";", false);//second parameter!
                super.visitLdcInsn(identifierOfMethod);//third parameter!

                //the int-case is a bit of a bitch, since 32-bit integers can encode a whole lot of primitives!
                Type returns = Type.getReturnType(identifierOfMethod);
                if (returns.equals(Type.BYTE_TYPE))
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                            "returnFromMethodWithByte", "(BLjava/lang/Thread;Ljava/lang/String;)V", false);
                else if (returns.equals(Type.SHORT_TYPE))
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                            "returnFromMethodWithShort", "(SLjava/lang/Thread;Ljava/lang/String;)V", false);
                else if (returns.equals(Type.CHAR_TYPE))
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                            "returnFromMethodWithChar", "(CLjava/lang/Thread;Ljava/lang/String;)V", false);
                else if (returns.equals(Type.INT_TYPE))
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                            "returnFromMethodWithInteger", "(ILjava/lang/Thread;Ljava/lang/String;)V", false);
                else if (returns.equals(Type.BOOLEAN_TYPE))
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                            "returnFromMethodWithBoolean", "(ZLjava/lang/Thread;Ljava/lang/String;)V", false);
                else throw new InternalError("This should never be reached because a 32-bit integer can only encode BYTE, SHORT, CHAR, INT or BOOLEAN."+
                            " Since it was reached however, this means that either Java's specification has changed or the ASM framework is broken...s");
                break;
            }
            case Opcodes.FRETURN :{
                super.visitInsn(Opcodes.DUP); //duplicate return value
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                        "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);//instance to call for
                super.visitInsn(Opcodes.SWAP); //swap instance with duplicate to get first parameter!
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                        "()L" + "java/lang/Thread" +";", false);//second parameter!
                super.visitLdcInsn(identifierOfMethod);//third parameter!
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                        "returnFromMethodWithFloat", "(FLjava/lang/Thread;Ljava/lang/String;)V", false);
                break;
            }
            case Opcodes.LRETURN :{
                super.visitInsn(Opcodes.DUP2); //duplicate return value (one will be the first parameter)
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                        "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);//instance to call for
                super.visitInsn(Opcodes.DUP_X2); //duplicate instance and move it between the double-duplicates
                super.visitInsn(Opcodes.POP); //remove the duplicated instance on the stack's top, now the first parameter is on top!
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                        "()L" + "java/lang/Thread" +";", false);//second parameter!
                super.visitLdcInsn(identifierOfMethod);//third parameter!
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                        "returnFromMethodWithLong", "(JLjava/lang/Thread;Ljava/lang/String;)V", false);
                break;
            }
            case Opcodes.DRETURN :{
                super.visitInsn(Opcodes.DUP2); //duplicate return value (one will be the first parameter)
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                        "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);//instance to call for
                super.visitInsn(Opcodes.DUP_X2); //duplicate instance and move it between the double-duplicates
                super.visitInsn(Opcodes.POP); //remove the duplicated instance on the stack's top, now the first parameter is on top!
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                        "()L" + "java/lang/Thread" +";", false);//second parameter!
                super.visitLdcInsn(identifierOfMethod);//third parameter!
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                        "returnFromMethodWithDouble", "(DLjava/lang/Thread;Ljava/lang/String;)V", false);
                break;
            }
            case Opcodes.ARETURN :{
                super.visitInsn(Opcodes.DUP); //duplicate return value
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                        "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);//instance to call for
                super.visitInsn(Opcodes.SWAP); //swap instance with duplicate to get first parameter!
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                        "()L" + "java/lang/Thread" +";", false);//second parameter!
                super.visitLdcInsn(identifierOfMethod);//third parameter!
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                        "returnFromMethodWithObject", "(Ljava/lang/Object;Ljava/lang/Thread;Ljava/lang/String;)V", false);
                break;
            }
            //void-case, nothing is returned
            case Opcodes.RETURN :{
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageObserver.coverageObserverClass,
                        "getCurrentInstance", "()L" + CoverageObserver.coverageObserverClass +";", false);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
                        "()L" + "java/lang/Thread" +";", false);//first parameter!
                super.visitLdcInsn(identifierOfMethod);//third parameter!
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CoverageObserver.coverageObserverClass,
                        "returnFromMethodWithVoid", "(Ljava/lang/Thread;Ljava/lang/String;)V", false);
                break;
            }
        }
        super.visitInsn(opcode);
    }

    @Override
    /**
     * BIPUSH, SIPUSH or NEWARRAY.
     */
    public void visitIntInsn(int opcode, int operand) {
        registerLine(opcode); //line must be registered as well of course!
        super.visitIntInsn(opcode, operand);
    }


    @Override
    /**
     * ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET.
     */
    public void visitVarInsn(int opcode, int var) {
        registerLine(opcode); //line must be registered as well of course!
        super.visitVarInsn(opcode, var);
    }


    @Override
    /**
     * NEW, ANEWARRAY, CHECKCAST or INSTANCEOF
     */
    public void visitTypeInsn(int opcode, String type) {
        registerLine(opcode); //line must be registered as well of course!
        super.visitTypeInsn(opcode, type);
    }


    @Override
    /**
     * GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD
     */
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        registerLine(opcode); //line must be registered as well of course!
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    /**
     * INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
     */
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc, boolean itf) {
        registerLine(opcode);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }


    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                       Object... bsmArgs) {
        registerLine(Opcodes.INVOKEDYNAMIC);
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }


    @Override
    /**
     * IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
     * IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
     */
    public void visitJumpInsn(int opcode, Label label) {
        registerLine(opcode);
        super.visitJumpInsn(opcode, label);
    }


    @Override
    /**
     * LDC (loading a constant instruction)
     */
    public void visitLdcInsn(Object cst) {
        registerLine(Opcodes.LDC);
        super.visitLdcInsn(cst);
    }


    @Override
    /**
     * IINC (incrementation instruction)
     */
    public void visitIincInsn(int var, int increment) {
        registerLine(Opcodes.IINC);
        super.visitIincInsn(var, increment);
    }


    @Override
    /**
     * TABLESWITCH
     */
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        registerLine(Opcodes.TABLESWITCH);
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }


    @Override
    /**
     * LOOKUPSWITCH
     */
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        registerLine(Opcodes.LOOKUPSWITCH);
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        registerLine(Opcodes.MULTIANEWARRAY);
        super.visitMultiANewArrayInsn(desc, dims);
    }

    /* No need to visit try-catch blocks because they are label-based and declared on top of the method */


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        //we have added operations and need to increase the stack! Recompute the values!
        super.visitMaxs(maxStackIncreaser /** is ingnored in non-safe mode, see
         {@link hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.Instrumenter} */,
                maxLocals);
    }


}