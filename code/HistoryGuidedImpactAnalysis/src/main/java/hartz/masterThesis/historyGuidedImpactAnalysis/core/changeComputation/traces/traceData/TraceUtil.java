package hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.DivergenceType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.MethodCallDivergence;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.TraceDivergence;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 * Class that holds functionality to compare the instructions executed by two different traces and to match method calls
 * onto each other. Complex code below, beware!
 */
public class TraceUtil {

    /**
     * Runs a comparison algorithm based on Levenshtein. All method calls will be considered equal!
     *
     * @param newTrace : array containing executed instruction indices for the newer trace
     * @param oldTrace : array containing executed instruction indices for the older trace
     * @param offset : can be used to ignore a certain part of the arrays above, everything below the offset
     *                 will not be compared (that means you must provide 0 to compare the full arrays)
     * @return positive integer distance metric on how different the two arrays are
     */
    public static int computeTraceDistanceQuick(int[] newTrace, int[] oldTrace, int offset) {
        return computeTraceDistanceQuick(newTrace, null, oldTrace, null, offset);
    }

    /**
     * Runs a comparison algorithm based on Levenshtein. If the 'desc'-arrays are null, every negative
     * number will be treated as equal to every other negative number, aka every method call is equal
     * to every method call. If the arrays are not 'null', the identifier of the called method will
     * be used for comparison.
     *
     * @param newTrace : array containing executed instruction indices for the newer trace
     * @param newDesc : array containing the actually executed instructions (their type) for the newer trace
     * @param oldTrace : array containing executed instruction indices for the older trace
     * @param oldDesc : array containing the actually executed instructions (their type) for the older trace
     * @param offset : can be used to ignore a certain part of the arrays above, everything below the offset
     *                 will not be compared (that means you must provide 0 to compare the full arrays)
     * @return positive integer distance metric on how different the two arrays are
     */
    public static int computeTraceDistanceQuick(int[] newTrace, String[] newDesc, int[] oldTrace, String[] oldDesc, int offset) {
        assert(newTrace.length >= 0);
        assert(oldTrace.length >= 0);
        assert (newDesc == null || newTrace.length == newDesc.length);
        assert (oldDesc == null ||  oldTrace.length == oldDesc.length);
        assert ((newDesc == null && oldDesc == null) || (newDesc != null && oldDesc != null));

        int lengthOfNewTrace = newTrace.length - offset;
        int lengthOfOldTrace = oldTrace.length - offset;

        int[] costs = new int[lengthOfNewTrace + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= lengthOfOldTrace; i++) {
            costs[0] = i;
            int costBuffer = i - 1;
            for (int j = 1; j <= lengthOfNewTrace; j++) {
                int newValue = newTrace[(j + offset) - 1];
                int oldValue = oldTrace[(i + offset) - 1];

                int costForCell = 1 + Math.min(costs[j], costs[j - 1]);
                if (newValue < 0 || oldValue < 0)
                    //if at least one is a method call, we need to use the more expensive comparison!
                    costForCell = Math.min(
                            costForCell,
                            ( newDesc == null ? ((newValue < 0 && oldValue < 0)
                                                     ? costBuffer /** = 'two method calls are always equal'-mode */
                                                     : costBuffer + 1
                                                )
                                              : ( newDesc[(j + offset) - 1].equals(oldDesc[(i + offset) - 1])
                                                     ? costBuffer /** = 'compare called method'-mode */
                                                     : costBuffer + 1
                                                )
                           )
                    );
                else costForCell = Math.min(costForCell, newValue == oldValue ? costBuffer : costBuffer + 1);
                costBuffer = costs[j];
                costs[j] = costForCell;
            }

            if (Tool.isDebugMode() && lengthOfNewTrace<1000 && lengthOfOldTrace<1000){
                /** This is for DEBUG purposes only and prints out nicely how the algorithm works! Produces nice
                 *  matrices for traces not longer than 999 entries! Otherwise, it is disabled automatically! */

                String s =  "";
                if (i == 1){
                    String headerLine = "         e ";
                    for (int j = 0; j < lengthOfNewTrace; j++){
                        if (Math.abs(newTrace[j+offset]) >= 100)
                            headerLine += (newTrace[j+offset]>=0?" ":"") + newTrace[j+offset] + " ";
                        else if (Math.abs(newTrace[j+offset]) >= 10)
                            headerLine += (newTrace[j+offset]>=0?"  ":" ") + newTrace[j+offset] + " ";
                        else headerLine += (newTrace[j+offset]>=0?"   ":"  ") + newTrace[j+offset] + " ";
                    }
                    Tool.printDebug(headerLine);

                    String hLine = "     ";
                    for (int j = 0; j < costs.length; j++) hLine+="-----";
                    Tool.printDebug(hLine);

                    String initialLine = "   e |";
                    for (int j = 0; j < costs.length; j++) {
                        if (j >= 100) initialLine += " "+j + " ";
                        else if (j >= 10) initialLine += "  " + j + " ";
                        else initialLine += "   " + j + " ";
                    }
                    Tool.printDebug(initialLine);
                }

                int rowVal = oldTrace[(i + offset) - 1];
                if (Math.abs(rowVal) >= 100) s += (rowVal>=0?" ":"") + rowVal;
                else if (Math.abs(rowVal) >= 10) s += (rowVal>=0?"  ":" ") + rowVal;
                else s += (rowVal>=0?"   ":"  ") +rowVal;
                s+= " |";

                for (int j = 0; j < costs.length; j++) {
                    if (costs[j] >= 100) s += " "+costs[j] + " ";
                    else if (costs[j] >= 10) s += "  " + costs[j] + " ";
                    else s += "   " + costs[j] + " ";
                }
                Tool.printDebug(s);
            }
        }


        return costs[lengthOfNewTrace];
    }

    /**
     * Runs a comparison algorithm based on Levenshtein, returns more information but is slower and eats
     * more memory than the alternative implementation. All method calls will be considered equal!
     *
     * @param newTrace : array containing executed instruction indices for the newer trace
     * @param oldTrace : array containing executed instruction indices for the older trace
     * @param offset : can be used to ignore a certain part of the arrays above, everything below the offset
     *                 will not be compared (that means you must provide 0 to compare the full arrays)
     * @return a TraceInstrDiff object, which contains the concrete differences as well as a distance metric!
     */
    public static TraceInstrDiff computeTraceDistanceFull(int[] newTrace, int[] oldTrace, int offset) {
        return computeTraceDistanceFull(newTrace, null, oldTrace, null, offset);
    }

    /**
     * Runs a comparison algorithm based on Levenshtein, returns more information but is slower and eats
     * more memory than the alternative implementation!  If the 'desc'-arrays are null, every negative
     * number will be treated as equal to every other negative number, aka every method call is equal
     * to every method call. If the arrays are not 'null', the identifier of the called method will be
     * used for comparison.
     *
     * @param newTrace : array containing executed instruction indices for the newer trace
     * @param newDesc : array containing the actually executed instructions (their type) for the newer trace
     * @param oldTrace : array containing executed instruction indices for the older trace
     * @param oldDesc : array containing the actually executed instructions (their type) for the older trace
     * @param offset : can be used to ignore a certain part of the arrays above, everything below the offset
     *                 will not be compared (that means you must provide 0 to compare the full arrays)
     * @return a TraceInstrDiff object, which contains the concrete differences as well as a distance metric!
     */
    public static TraceInstrDiff computeTraceDistanceFull(int[] newTrace, String[] newDesc, int[] oldTrace, String[] oldDesc, int offset) {
        assert (newTrace.length >= 0);
        assert (oldTrace.length >= 0);
        assert (newDesc == null || newTrace.length == newDesc.length);
        assert (oldDesc == null ||  oldTrace.length == oldDesc.length);
        assert ((newDesc == null && oldDesc == null) || (newDesc != null && oldDesc != null));

        int lengthOfNewTrace = newTrace.length - offset;
        int lengthOfOldTrace = oldTrace.length - offset;

         /*
            Levenshtein is a dynamic programming algorithm to determine the minimal edit-distance (or Levenshtein-distance)
            between two strings, the classical example is the 'republican' vs 'democrat' case seen here.

                  r  e  p  u  b  l  i  c  a  n
                  ------------------------------
               0 | 1  2  3  4  5  6  7  8  9  10
            d  1 | 1  2  3  4  5  6  7  8  9  10
            e  2 | 2  1  2  3  4  5  6  7  8  9
            m  3 | 3  2  2  3  4  5  6  7  8  9
            o  4 | 4  3  3  3  4  5  6  7  8  9
            c  5 | 5  4  4  4  4  5  6  6  7  8
            r  6 | 5  5  5  5  5  5  6  7  7  8
            a  7 | 6  6  6  6  6  6  6  7  7  8
            t  8 | 7  7  7  7  7  7  7  7  8 [8] <- this is the result, the distance between the strings!

            An optimal path through the matrix from the upper left encodes insertion, deletion or replacement operations!
            Horizontal move means insert from upper string, vertical move is deletion from left string, diagonal move means
            either do nothing if the number stayed the same or a replacement operation if it did not.
        */


        //construct the full cost matrix, then select possible operations
        int[/*column*/][/*row*/] costs = new int[lengthOfNewTrace + 1][ lengthOfOldTrace + 1];
        for (int col = 0; col < costs.length; col++)
            costs[col][0] = col;
        for (int row = 0; row < costs[0].length; row++)
            costs[0][row] = row;

        for (int row = 1; row <= lengthOfOldTrace; row++) {
            for (int col = 1; col <= lengthOfNewTrace; col++) {
                int newValue = newTrace[(col + offset) - 1];
                int oldValue = oldTrace[(row + offset) - 1];

                boolean equals = (newValue < 0 || oldValue < 0) ? // if one is a method call
                        ( //we are comparing indices from which at least one is a method call, need to use expensive comparison
                                newDesc == null ? (newValue < 0 && oldValue < 0) /** = 'two method calls are always equal'-mode */
                                                : newDesc[(col + offset) - 1].equals(oldDesc[(row + offset) - 1])
                        ) : newValue == oldValue; //no method call, simply compare the instruction index

                int keep = equals ? costs[col - 1][row - 1] : Integer.MAX_VALUE;
                int diagonal = costs[col - 1][row - 1]+ 1;
                int up = costs[col][row - 1] + 1;
                int left = costs[col - 1][row] + 1;

                //store costs
                costs[col][row] = Math.min(Math.min(up,left),Math.min(diagonal,keep));
            }
        }

        int distance = costs[lengthOfNewTrace][lengthOfOldTrace];

        if (Tool.isDebugMode() && lengthOfNewTrace<1000 && lengthOfOldTrace<1000){
            /** This is for DEBUG purposes only and prints out nicely how the algorithm works! Produces nice
             *  matrices for traces not longer than 999 entries! Otherwise, it is disabled automatically! */

            String headerLine = "         e ";
            for (int j = 0; j < lengthOfNewTrace; j++){
                if (Math.abs(newTrace[j+offset]) >= 100)
                    headerLine += (newTrace[j+offset]>=0?" ":"") + newTrace[j+offset] + " ";
                else if (Math.abs(newTrace[j+offset]) >= 10)
                    headerLine += (newTrace[j+offset]>=0?"  ":" ") + newTrace[j+offset] + " ";
                else headerLine += (newTrace[j+offset]>=0?"   ":"  ") + newTrace[j+offset] + " ";
            }
            Tool.printDebug(headerLine);

            String hLine = "     ";
            for (int j = 0; j < costs.length; j++) hLine+="-----";
            Tool.printDebug(hLine);


            for (int row = 0; row <= lengthOfOldTrace; row++) {
                String s;
                if (row == 0){
                    s = "   e";
                } else{
                    int rowVal = oldTrace[(row + offset) - 1];
                    if (Math.abs(rowVal) >= 100) s = (rowVal>=0?" ":"") + rowVal;
                    else if (Math.abs(rowVal) >= 10) s = (rowVal>=0?"  ":" ") + rowVal;
                    else s = (rowVal>=0?"   ":"  ") +rowVal;
                }

                s+= " |";

                for (int col = 0; col <= lengthOfNewTrace; col++) {
                    if (costs[col][row] >= 100) s += " "+costs[col][row] + " ";
                    else if (costs[col][row] >= 10) s += "  " + costs[col][row] + " ";
                    else s += "   " + costs[col][row] + " ";
                }
                Tool.printDebug(s);
            }
            Tool.printDebug("");
        }


        //start backtracking step to obtain location of changes, begin at result cell (lowest, right cell)!
        int columnCostIndex = lengthOfNewTrace; //columns
        int rowCostIndex = lengthOfOldTrace; //rows

        TreeMap<Integer, int[][]> differences = new TreeMap<>();
        LinkedList<Integer>[] diff = null; //as long as this is 'null', the traces have not diverged!
        while (columnCostIndex > 0 && rowCostIndex > 0) { //until we reach the left or top edge of the table!
            int currentCosts = costs[columnCostIndex][rowCostIndex];

            if(currentCosts == 0 /* = we are done, strings are equal from here on! */){
                if (diff != null){
                    assert (!differences.containsKey(columnCostIndex));

                    //store the differences as sections
                    int[] newArr = new int[diff[0].size()];
                    int cou0 = diff[0].size()-1;
                    for (Integer i : diff[0]){newArr[cou0--] = i.intValue();}
                    int[] oldArr = new int[diff[1].size()];
                    int cou1 = diff[1].size()-1;
                    for (Integer i : diff[1]){oldArr[cou1--] = i.intValue();}
                    differences.put(columnCostIndex /* store diff based on new*/, new int[][]{newArr,oldArr});
                    diff = null;
                }
                break;
            }

            //obtain costs for possible operations (= where we could have come from)
            int diagonal = costs[columnCostIndex - 1][rowCostIndex - 1];
            int up = costs[columnCostIndex][rowCostIndex - 1];
            int left = costs[columnCostIndex - 1][rowCostIndex];

            /** There can be multiple optimal paths. We simply favor diagonal!
             *  If one would want something else, just overwrite the 'if'-below... */

            if (diagonal <= up && diagonal <= left) {
                --columnCostIndex;
                --rowCostIndex; //take diagonal
                if (diagonal < currentCosts) {
                    //was a replacement!

                    int newValue = newTrace[(columnCostIndex + offset)];
                    int oldValue = oldTrace[(rowCostIndex + offset)];

                    if (diff == null) {
                        /* we are in a diverging section now */
                        diff = new LinkedList[]{new LinkedList<>(), new LinkedList<>()};
                        diff[0].add(newValue);
                        diff[1].add(oldValue);
                    } else {
                        /* we are still inside a diverging section */
                        diff[0].add(newValue);
                        diff[1].add(oldValue);
                    }
                } else {
                    //no-op, we assume the trace has not changed
                    if (diff != null){
                        //if we come from a state were the trace had been changed, store that difference now!
                        assert (!differences.containsKey(columnCostIndex));
                        int[] newArr = new int[diff[0].size()];
                        int cou0 = diff[0].size()-1;
                        for (Integer i : diff[0]){newArr[cou0--] = i.intValue();}
                        int[] oldArr = new int[diff[1].size()];
                        int cou1 = diff[1].size()-1;
                        for (Integer i : diff[1]){oldArr[cou1--] = i.intValue();}
                        differences.put(columnCostIndex /* store diff based on new*/, new int[][]{newArr,oldArr});

                        diff = null; //trace has merged again
                    }
                }
            } else {
                if (left <= up && left <= currentCosts) {
                    --columnCostIndex; //take left, was an insert!
                    int newValue = newTrace[(columnCostIndex + offset)];

                    if (diff == null) {
                        /* we are in a diverging section now */
                        diff = new LinkedList[]{new LinkedList<>(), new LinkedList<>()};
                        diff[0].add(newValue);
                    } else {
                        /* we are still inside a diverging section */
                        diff[0].add(newValue);
                    }

                } else {
                    --rowCostIndex; //take top, was a delete!
                    int oldValue = oldTrace[(rowCostIndex + offset)];

                    if (diff == null) {
                        /* we are in a diverging section now */
                        diff = new LinkedList[]{new LinkedList<>(), new LinkedList<>()};
                        diff[1].add(oldValue);
                    } else {
                        /* we are still inside a diverging section */
                        diff[1].add(oldValue);
                    }
                }
            }
        }

        if (diff != null){
            /* This means that there was basically one section that lasted through the end of the loop! */
            int[] newArr = new int[diff[0].size()];
            int cou0 = diff[0].size()-1;
            for (Integer i : diff[0]){newArr[cou0--] = i.intValue();}
            int[] oldArr = new int[diff[1].size()];
            int cou1 = diff[1].size()-1;
            for (Integer i : diff[1]){oldArr[cou1--] = i.intValue();}
            differences.put(columnCostIndex /* store diff based on new*/, new int[][]{newArr,oldArr});
        }

        return new TraceInstrDiff(distance, differences);
    }

    /**
     * Matches method invocations from two different traces onto one another.
     *
     * @param newCalls : method identifiers from the newer trace
     * @param oldCalls : method identifiers from the newer trace
     * @param method : the newer MethodData object, is stored in all generated Divergence instances for unmatchable methods!
     * @param divergenceContainer : container into which unmatchable method calls are to be added, is modified by this method!
     * @return an int[] at the length of 'newCalls', where at each index either the index of the matching method call in 'oldCalls' is
     *         stored OR 'Integer.MIN_VALUE' resides if that particular method call in the new trace could not be matched!
     */
    public static int[] matchMethodCalls(String[] newCalls, String[] oldCalls, MethodData method, List<TraceDivergence> divergenceContainer) {
        assert (newCalls.length >= 0);
        assert (oldCalls.length >= 0);
        int amountOfCallsInNew = newCalls.length;
        int amountOfCallsInOld = oldCalls.length;


        /* For the mapping, we use Levenshtein again although we do not care about the distance all to much. However
         * in this version we only use insertion and deletion as operations, no replacement. That means basically
         * that the diagonal+1 option is not present. Note that this change affects both computation and backtracking! */
        int[/*column*/][/*row*/] costs = new int[amountOfCallsInNew + 1][ amountOfCallsInOld + 1];
        for (int col = 0; col < costs.length; col++)
            costs[col][0] = col;
        for (int row = 0; row < costs[0].length; row++)
            costs[0][row] = row;
        for (int row = 1; row <= amountOfCallsInOld; row++) {
            for (int col = 1; col <= amountOfCallsInNew; col++) {
                String newValue = newCalls[col-1];
                String oldValue = oldCalls[row-1];
                int keep = newValue.equals(oldValue) ? costs[col - 1][row - 1] : Integer.MAX_VALUE;
                int up = costs[col][row - 1] + 1;
                int left = costs[col - 1][row] + 1;
                costs[col][row] = Math.min(Math.min(up,left),keep);
            }
        }


        if (Tool.isDebugMode() && amountOfCallsInNew<1000 && amountOfCallsInOld<1000){
            /** This is for DEBUG purposes only and prints out nicely how the algorithm works! Produces nice
             *  matrices for traces not longer than 999 entries! Otherwise, it is disabled automatically! */

            String header = "Method Matching Table";
            int chars = 1;
            for (int j = 0; j < costs.length; j++) chars+=5;
            boolean front = true;
            while (chars > header.length()){
                header = front ? "-"+header : header+"-";
                front = !front;
            }
            Tool.printDebug(header);


            for (int row = 0; row <= amountOfCallsInOld; row++) {
                String s= " |";
                for (int col = 0; col <= amountOfCallsInNew; col++) {
                    if (costs[col][row] >= 100) s += " "+costs[col][row] + " ";
                    else if (costs[col][row] >= 10) s += "  " + costs[col][row] + " ";
                    else s += "   " + costs[col][row] + " ";
                }
                Tool.printDebug(s);
            }
            Tool.printDebug("");
        }

        //init with non-default value meaning non-matched method call!
        int[] result = new int[amountOfCallsInNew];
        Arrays.fill(result, Integer.MIN_VALUE);

        if (costs[amountOfCallsInNew][amountOfCallsInOld] == 0){
            /** We can perfectly match each method call, construct a perfect result array and return! */
            assert(amountOfCallsInOld == amountOfCallsInNew); //otherwise this is impossible
            for (int i = 0; i<amountOfCallsInNew; ++i)
                result[i] = i;
            return result;
        } else {
            /** We could not match each call, now it gets tricky! In contrast to our backtracking operation for
             *  the instructions, we MUST reach the upper left cell in this case and create a complete mapping
             *  for the function calls! */
            int columnCostIndex = amountOfCallsInNew; //columns
            int rowCostIndex = amountOfCallsInOld; //rows

            LinkedList<TraceDivergence> tmpDifferences = new LinkedList<>();

            while (columnCostIndex > 0 || rowCostIndex > 0) { //until we reach top, left cell!
                int currentCosts = costs[columnCostIndex][rowCostIndex];

                //obtain costs for possible operations (= where we could have come from)
                int diagonal = (columnCostIndex > 0 && rowCostIndex > 0) ? costs[columnCostIndex - 1][rowCostIndex - 1] : Integer.MAX_VALUE;
                int up = (rowCostIndex > 0) ?costs[columnCostIndex][rowCostIndex - 1] : Integer.MAX_VALUE;
                int left = (columnCostIndex > 0) ? costs[columnCostIndex - 1][rowCostIndex] : Integer.MAX_VALUE;


                if (columnCostIndex > 0 && rowCostIndex > 0 && diagonal == currentCosts && diagonal <= up && diagonal <= left) {
                    --columnCostIndex;
                    --rowCostIndex; //take diagonal
                    //we could perfectly map this function call!
                    assert(newCalls[columnCostIndex].equals(oldCalls[rowCostIndex]));
                    result[columnCostIndex] = rowCostIndex;
                } else {
                    if (columnCostIndex > 0 && left <= up && left <= currentCosts) {
                        --columnCostIndex; //take left, was an insert!
                        String functionCalledInNew = newCalls[columnCostIndex];

                        tmpDifferences.addFirst(new MethodCallDivergence(
                                        method, DivergenceType.ADDITIONAL_METHOD_CALLED, null, functionCalledInNew)
                        );

                    } else if (rowCostIndex > 0){
                        --rowCostIndex; //take top, was a delete!
                        String functionCalledInOld = oldCalls[rowCostIndex];

                        tmpDifferences.addFirst(new MethodCallDivergence(
                                        method, DivergenceType.NOT_CALLED_METHOD, functionCalledInOld, null)
                        );
                    } else throw new InternalError("Unknown error during Method Call Matching Operation, this should actually never happen!");
                }
            }


            divergenceContainer.addAll(tmpDifferences);
            return result;
        }

    }



    /**
     * Contains a positive integer distance metric on how different two traces are as well
     * as a map with the exact differences. The map encodes at which *INDEX* in the new
     * trace the diverging section begins and is associated with a pair of arrays that
     * holds the *INSTRUCTIONS* of the two different sections (think int[][]{newArr,oldArr}; )
     */
    public static class TraceInstrDiff {
        public final int distanceMetricValue;
        public final TreeMap<Integer, int[][]> differences;

        public TraceInstrDiff(int val, TreeMap<Integer, int[][]> diff){
            differences = diff;
            distanceMetricValue = val;
        }
    }
}
