package hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data;

import java.util.HashSet;

/**
 * Adapter containing extracted source from ASM 5.0, since this god-forsaken library lacks any opcode-int
 * to opcode-string converter! Their reason being their insistence on not using an enum for opcodes... :-(
 * If a new ASM version is to be used, we will need to update this functionality!
 */
public class OpcodeTranslator {

    private static final String[] OPCODES;
    static {
        String s = "NOP,ACONST_NULL,ICONST_M1,ICONST_0,ICONST_1,ICONST_2,"
                + "ICONST_3,ICONST_4,ICONST_5,LCONST_0,LCONST_1,FCONST_0,"
                + "FCONST_1,FCONST_2,DCONST_0,DCONST_1,BIPUSH,SIPUSH,LDC,,,"
                + "ILOAD,LLOAD,FLOAD,DLOAD,ALOAD,,,,,,,,,,,,,,,,,,,,,IALOAD,"
                + "LALOAD,FALOAD,DALOAD,AALOAD,BALOAD,CALOAD,SALOAD,ISTORE,"
                + "LSTORE,FSTORE,DSTORE,ASTORE,,,,,,,,,,,,,,,,,,,,,IASTORE,"
                + "LASTORE,FASTORE,DASTORE,AASTORE,BASTORE,CASTORE,SASTORE,POP,"
                + "POP2,DUP,DUP_X1,DUP_X2,DUP2,DUP2_X1,DUP2_X2,SWAP,IADD,LADD,"
                + "FADD,DADD,ISUB,LSUB,FSUB,DSUB,IMUL,LMUL,FMUL,DMUL,IDIV,LDIV,"
                + "FDIV,DDIV,IREM,LREM,FREM,DREM,INEG,LNEG,FNEG,DNEG,ISHL,LSHL,"
                + "ISHR,LSHR,IUSHR,LUSHR,IAND,LAND,IOR,LOR,IXOR,LXOR,IINC,I2L,"
                + "I2F,I2D,L2I,L2F,L2D,F2I,F2L,F2D,D2I,D2L,D2F,I2B,I2C,I2S,LCMP,"
                + "FCMPL,FCMPG,DCMPL,DCMPG,IFEQ,IFNE,IFLT,IFGE,IFGT,IFLE,"
                + "IF_ICMPEQ,IF_ICMPNE,IF_ICMPLT,IF_ICMPGE,IF_ICMPGT,IF_ICMPLE,"
                + "IF_ACMPEQ,IF_ACMPNE,GOTO,JSR,RET,TABLESWITCH,LOOKUPSWITCH,"
                + "IRETURN,LRETURN,FRETURN,DRETURN,ARETURN,RETURN,GETSTATIC,"
                + "PUTSTATIC,GETFIELD,PUTFIELD,INVOKEVIRTUAL,INVOKESPECIAL,"
                + "INVOKESTATIC,INVOKEINTERFACE,INVOKEDYNAMIC,NEW,NEWARRAY,"
                + "ANEWARRAY,ARRAYLENGTH,ATHROW,CHECKCAST,INSTANCEOF,"
                + "MONITORENTER,MONITOREXIT,,MULTIANEWARRAY,IFNULL,IFNONNULL,";
        OPCODES = new String[200];
        int i = 0;
        int j = 0;
        int l;
        while ((l = s.indexOf(',', j)) > 0) {
            OPCODES[i++] = j + 1 == l ? null : s.substring(j, l);
            j = l + 1;
        }
    }

    private static final HashSet<String> branchingInstr = new HashSet<>();
    static{
        String[] str = new String[]{ /** we consider these 28 instr to be branch altering */
                "IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE", "IF_ICMPEQ","IF_ICMPNE","IF_ICMPLT","IF_ICMPGE",
                "IF_ICMPGT","IF_ICMPLE","IF_ACMPEQ","IF_ACMPNE","GOTO","JSR","RET","TABLESWITCH","LOOKUPSWITCH",
                "IRETURN","LRETURN","FRETURN","DRETURN","ARETURN","RETURN","IFNULL","IFNONNULL","ATHROW"};
        for (String s : str) branchingInstr.add(s);
    }

    public static String getInstructionStr(int opcode) {
        assert(opcode>=0 && opcode < OPCODES.length);
        return OPCODES[opcode];
    }

    public static boolean modifiesControlFlow(int opcode) {
        assert(opcode>=0 && opcode < OPCODES.length);
        return modifiesControlFlow(OPCODES[opcode]);
    }

    public static boolean modifiesControlFlow(String instr) {
        assert(instr != null && !instr.isEmpty());
        return branchingInstr.contains(instr);
    }
}
