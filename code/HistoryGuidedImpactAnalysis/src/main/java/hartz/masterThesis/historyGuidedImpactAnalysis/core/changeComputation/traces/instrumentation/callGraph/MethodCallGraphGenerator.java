package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/** Special instrumenter that does not instrument anything but only goes through source code to create a call graph !*/
public class MethodCallGraphGenerator extends MethodVisitor{

    private final String myIdentifier;
    private final HashMap<String, List<String>> callGraph;

    public MethodCallGraphGenerator(int api, MethodVisitor methodVisitor, String myIdentifier,
                                    HashMap<String, List<String>> callGraph) {
        super(api, methodVisitor);
        this.myIdentifier = myIdentifier;
        this.callGraph = callGraph;
    }

    /** Stores that a method can call another :-) */
    private void addEdge(String called){
        if (!callGraph.containsKey(myIdentifier))
            callGraph.put(myIdentifier, new LinkedList<String>());
        callGraph.get(myIdentifier).add(called);
    }

    @Override
    /**
     * INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
     */
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        addEdge(owner.replace("/",".")+"."+name+desc);
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
    }


    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle boostrapMethod, Object... boostrapMethodArgs) {
        /** We cannot determine where this call leads as it will only be known at runtime. We currently exclude
         *  th */
        super.visitInvokeDynamicInsn(name, desc, boostrapMethod, boostrapMethodArgs);
    }
}