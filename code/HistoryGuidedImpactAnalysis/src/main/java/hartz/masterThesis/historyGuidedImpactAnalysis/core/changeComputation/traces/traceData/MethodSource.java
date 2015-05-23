package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData;


import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.OpcodeTranslator;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;

import java.util.ArrayList;

/**
 * Class modelling the actual compiled source of a instrumented method. Could be used to perform detailed analysis on which
 * basic blocks changed or how the control-flow was modified by a user's syntax change. Is currently not used to its full extend
 * due to timing constraints.
 *
 * LOTS OF POTENTIAL HERE!
 */
public class MethodSource {

    public final String identifier;
    public final String[] instructions;

    /* TODO: the block analysis could be expanded with the edges by storing the labels during instrumentation */
    public final int[][] basicBlocksToInstr;
    public final int[] instrToBasicBlocks;

    public final int[] javaLines; //may be null

    public MethodSource(JSONObject o){
        identifier = o.getString("method");

        JSONArray a = o.getJSONArray("source");
        int[] javaLineTmp = new int[a.length()];
        instructions = new String[a.length()];
        instrToBasicBlocks = new int[a.length()];

        ArrayList<int[]> tmp = new ArrayList<>();

        boolean sawJavaLines = false;
        int basicBlockCounter = 0;
        int lastBasicBlock = 0;

        for (int i = 0; i<a.length(); ++i){
            String s = a.getString(i);
            instructions[i] = s.substring(s.indexOf(":")).trim();

            //deal with building basic blocks
            instrToBasicBlocks[i] = basicBlockCounter;
            if (OpcodeTranslator.modifiesControlFlow(s)) {
                /** so a GOTO or IF is the *last* instruction in a block */
                int[] instrOfThisBlock = new int[i-lastBasicBlock];
                int counter = 0;
                for (int j = lastBasicBlock; j<=i; ++j)
                    instrOfThisBlock[counter++] = j;
                tmp.add(instrOfThisBlock);

                ++basicBlockCounter;
            }

            //if there is a line mapping, import it as well
            if (s.contains("(") && s.contains(")")) {
                javaLineTmp[i] = Integer.parseInt(s.substring(s.indexOf("(") + 1, s.indexOf(")")).trim());
                sawJavaLines = true;
            }
            else javaLineTmp[i] = -1;
        }

        basicBlocksToInstr = tmp.toArray(new int[tmp.size()][]);
        javaLines = sawJavaLines ? javaLineTmp : null;

    }
}
