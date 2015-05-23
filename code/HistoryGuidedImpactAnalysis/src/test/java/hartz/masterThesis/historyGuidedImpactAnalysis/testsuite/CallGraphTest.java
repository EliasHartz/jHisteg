package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.callGraph.CallGraph;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CallGraphTest {

    public List<String> getList (String... edgesTo){
        List<String> l = new LinkedList<>();
        for (String s : edgesTo){
            l.add(s);
        }
        return l;
    }

    public HashSet<String> getHashSet (String... targets){
        HashSet<String> l = new HashSet<>();
        for (String s : targets){
            l.add(s);
        }
        return l;
    }


    private void verifyMultiTarget(CallGraph g, String from, String[] to, int[] expected) {
        assertTrue(to.length==expected.length);
        HashMap<String, Integer> actual = g.getDistanceTo(from, getHashSet(to));
        for (int i = 0; i<expected.length; ++i){
            assertTrue(actual.containsKey(to[i]));
            assertEquals(expected[i], actual.get(to[i]).intValue());
        }

    }

    @Test
    public void testGraphDistance1() {
        HashMap<String, List<String>> graph = new HashMap<>();

        graph.put("A", getList("B", "C"));
        graph.put("B", getList("D", "E"));
        graph.put("C", getList("F", "G", "H", "I"));
        graph.put("E", getList("C"));
        graph.put("F", getList("D"));
        graph.put("G", getList("J","K"));
        graph.put("J", getList("M"));
        graph.put("H", getList("L", "M", "F"));
        graph.put("L", getList("I"));
        graph.put("I", getList("H"));
        graph.put("J", getList("M"));

        CallGraph g = new CallGraph(graph);

        assertEquals(1,g.getDistanceToCaller("C","A"));
        assertEquals(3,g.getDistanceToCaller("M","A"));
        assertEquals(2,g.getDistanceToCaller("D","H"));
        assertEquals(0,g.getDistanceToCaller("H","H"));
        assertEquals(4,g.getDistanceToCaller("D","L"));
        assertEquals(3,g.getDistanceToCaller("K","A"));
        assertEquals(Integer.MIN_VALUE,g.getDistanceToCaller("A","E"));


        verifyMultiTarget(g,"A",
                new String[]{"C", "B"},
                new int[]{    1,   1}
        );

        verifyMultiTarget(g,"A",
                new String[]{"D", "E"},
                new int[]{    2,   2}
        );

        verifyMultiTarget(g,"A",
                new String[]{"F", "C", "M"},
                new int[]{    2,   1,   3}
        );

        verifyMultiTarget(g,"H",
                new String[]{"I", "H", "L"},
                new int[]{    2,   0,   1}
        );

        verifyMultiTarget(g,"K",
                new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"},
                new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                        Integer.MIN_VALUE, Integer.MIN_VALUE ,Integer.MIN_VALUE,
                        Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE,
                        Integer.MIN_VALUE}
        );
    }

    @Test
    public void testGraphDistance2() {
        HashMap<String, List<String>> graph = new HashMap<>();

        graph.put("A", getList("D", "B", "H"));
        graph.put("B", getList("J", "A", "C"));
        graph.put("C", getList("E", "B", "F"));
        graph.put("D", getList("A", "F"));
        graph.put("E", getList("C", "H"));
        graph.put("F", getList("D", "C", "G"));
        graph.put("G", getList("F","H", "I"));
        graph.put("H", getList("A", "G", "E"));
        graph.put("I", getList("G"));
        graph.put("J", getList("B"));

        CallGraph g = new CallGraph(graph);

        assertEquals(1,g.getDistanceToCaller("H","A"));
        assertEquals(1,g.getDistanceToCaller("F","C"));
        assertEquals(2,g.getDistanceToCaller("G","C"));
        assertEquals(2,g.getDistanceToCaller("C","A"));
        assertEquals(3,g.getDistanceToCaller("D","E"));
        assertEquals(5,g.getDistanceToCaller("I","J"));

        verifyMultiTarget(g,"J",
                new String[]{"A", "C", "B", "J", "F", "H", "I"},
                new int[]{    2,   2,   1,   0,   3,   3,   5}
        );
        verifyMultiTarget(g,"G",
                new String[]{"A", "C", "I", "D", "E"},
                new int[]{    2,   2,   1,   2,   2}
        );
    }

    @Test
    public void testGraphDistance3() {
        HashMap<String, List<String>> graph = new HashMap<>();

        graph.put("1", getList("2", "3", "4", "5"));
        graph.put("2", getList("6"));
        graph.put("3", getList("6", "7"));
        graph.put("4", getList("12", "8"));
        graph.put("5", getList("8"));
        graph.put("6", getList("9"));
        graph.put("7", getList("10","11"));
        graph.put("8", getList("13", "14"));
        graph.put("9", getList("5"));
        graph.put("10", getList("15", "16"));
        graph.put("11", getList("17"));
        graph.put("12", getList("17"));
        graph.put("13", getList("17"));
        graph.put("14", getList("19"));
        graph.put("15", getList("20"));
        graph.put("16", getList("18"));
        graph.put("17", getList("19"));
        graph.put("18", getList("20"));
        graph.put("19", getList("16"));
        graph.put("20", getList("1"));

        CallGraph g = new CallGraph(graph);

        assertEquals(5,g.getDistanceToCaller("20","1"));
        assertEquals(1,g.getDistanceToCaller("5","1"));
        assertEquals(3,g.getDistanceToCaller("5","3"));
        assertEquals(6,g.getDistanceToCaller("20","4"));
        assertEquals(10,g.getDistanceToCaller("1","2"));

        verifyMultiTarget(g,"1",
                new String[]{"2", "3", "4", "5"},
                new int[]{    1,   1,   1,   1}
        );
        verifyMultiTarget(g,"1",
                new String[]{"6", "7", "8"},
                new int[]{    2,   2,   2}
        );
        verifyMultiTarget(g,"1",
                new String[]{"9", "10", "11", "12", "13", "14"},
                new int[]{    3,   3,    3,    2,    3,    3}
        );
        verifyMultiTarget(g,"1",
                new String[]{"15", "16", "17"},
                new int[]{    4,    4,    3}
        );
    }
}
