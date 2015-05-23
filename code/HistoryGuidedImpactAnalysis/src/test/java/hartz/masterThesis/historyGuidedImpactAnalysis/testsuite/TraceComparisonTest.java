package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;


import hartz.masterThesis.historyGuidedImpactAnalysis.core.Core;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.Trace;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodSource;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.divergences.*;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.MainFunctionality;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class TraceComparisonTest {

    private File dummyVersion;

    private JSONObject getAdditionalCall(){
        return new JSONObject(
                "      {\n" +
                "         \"called\": \"myPackage.someClass.additional()V\",\n" +
                "         \"calledWithParameters\": [],\n" +
                "         \"returned\": {\n" +
                "            \"class\": \"---\",\n" +
                "            \"stringRepresentation\": \"void\"\n" +
                "         },\n" +
                "         \"trace\": [\n" +
                "            \"[0]> RETURN\"\n" +
                "         ],\n" +
                "         \"type\": \"METHOD\"\n" +
                "      }");
    }

    /**
     * Injects the full source of the short examples. Traces may use only sections.
     * Take note that the source is completely non-sensical and probably violates
     * a whole bunch of JVM rules. It is FOR TESTING OF TRACE COMPARISONS ONLY!
     *
     * @param method : the method to inject code for
     */
    private void injectMiniFunction(Version v, String method) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", method);
        String[] sourceData = {
                "0: ALOAD",
                "1: INVOKESPECIAL",
                "2: ALOAD",
                "3: ALOAD",
                "4: PUTFIELD",
                "5: RETURN",
        };

        jsonObject.put("source", new JSONArray(sourceData));
        Core.DEBUG_ONLY_injectSourceCode(v, method, new MethodSource(jsonObject));
    }


    @Test
    public void testFullNonRecursiveComparison() throws IOException, URISyntaxException {
        /* scaffolding...    its okay, we will not access this stuff anyway */
        Version dummyVersion1 = new Version(0,"a","","","");
        Version dummyVersion2 = new Version(1,"b","","","");
        dummyVersion1.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);
        dummyVersion2.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);

        /** We want to test the main comparison method based on amount and localization of diverging traces! */
        MethodData.setTraceDistanceBasedComparison(false);
        MethodData.setCoverageBasedComparison(false);

        /**----------------------- Compare the traces with no source available ------------------------*/
        File oldFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/nonRecursiveOld").toURI());
        File newFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/nonRecursiveNew").toURI());
        Trace oldTraceData = new Trace(dummyVersion1, new JSONObject(FileUtils.readData(oldFile)),
                new HashMap<String, List<SyntaxChange>>(), "oldtrace");
        Trace newTraceData = new Trace(dummyVersion2, new JSONObject(FileUtils.readData(newFile)),
                new HashMap<String, List<SyntaxChange>>(), "newtrace");

        List<TraceDivergence> traceDivergences = oldTraceData.compareAgainstNewerTrace(newTraceData, false);
        assertTrue(traceDivergences.size() == 2);
        assertTrue(traceDivergences.get(0) instanceof MetricDivergence);
        assertTrue(traceDivergences.get(1) instanceof MetricDivergence);
        MetricDivergence result1 = (MetricDivergence) traceDivergences.get(0);
        MetricDivergence result2 = (MetricDivergence) traceDivergences.get(1);

        //verify
        assertEquals(9, result1.metricValue); //distance
        assertEquals(3, result2.metricValue); //how often it diverged




        /**------------------ Compare the traces with source lookup available -----------------------*/

        //inject source
        File sourceFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/source").toURI());
        JSONObject jsonObject = new JSONObject(FileUtils.readData(sourceFile));
        jsonObject.put("method","myPackage.myClass.myMethod()V");
        Core.DEBUG_ONLY_injectSourceCode(dummyVersion2, "myPackage.myClass.myMethod()V", new MethodSource(jsonObject));

        traceDivergences.clear();

        //compare
        traceDivergences = oldTraceData.compareAgainstNewerTrace(newTraceData, false);
        assertTrue(traceDivergences.size() == 2);
        assertTrue(traceDivergences.get(0) instanceof MetricDivergence);
        assertTrue(traceDivergences.get(1) instanceof MetricDivergence);
        result1 = (MetricDivergence) traceDivergences.get(0);
        result2 = (MetricDivergence) traceDivergences.get(1);

        //Must be identical to the result from before since there are no method calls!
        assertEquals(9, result1.metricValue); //distance
        assertEquals(3, result2.metricValue); //how often it diverged
    }

    @Test
    public void testMethodInvocationMappingEqualNumberOfCalls() throws IOException, URISyntaxException {
        /* scaffolding...    its okay, we will not access this stuff anyway */
        Version dummyVersion1 = new Version(0,"a","","","");
        Version dummyVersion2 = new Version(1,"b","","","");
        dummyVersion1.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);
        dummyVersion2.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);

        /** We want to test the main comparison method based on amount and localization of diverging traces! */
        MethodData.setTraceDistanceBasedComparison(false);
        MethodData.setCoverageBasedComparison(false);

        /**----------------------- Compare the traces with no source available ------------------------*/
        File oldFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/recursiveOld").toURI());
        File newFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/recursiveNew").toURI());
        JSONObject trace1 = new JSONObject(FileUtils.readData(oldFile));
        JSONObject trace2 = new JSONObject(FileUtils.readData(newFile));
        Trace oldTraceData = new Trace(dummyVersion1, trace1, new HashMap<String, List<SyntaxChange>>(), "oldtrace");
        Trace newTraceData = new Trace(dummyVersion2, trace2, new HashMap<String, List<SyntaxChange>>(), "newtrace");

        List<TraceDivergence> traceDivergences = oldTraceData.compareAgainstNewerTrace(newTraceData, false);
        assertEquals( 2 + //two additional methods
                      2 + //two methods less
                      1 + //one matchable method (has a different return value to generate a diverge instance)
                      0, //no distance and number of divergences were found!
                traceDivergences.size());


        /** ----------------------    verify the resulting method call matching -----------------------*/
        MethodCallDivergence[] actual = new MethodCallDivergence[traceDivergences.size()];
        int i = 0;
        for (TraceDivergence t : traceDivergences) {
            if (t instanceof MethodCallDivergence) {
                MethodCallDivergence c = (MethodCallDivergence) t;
                actual[i] = c;
            }
            ++i;
        }
        assertEquals("myPackage.myClass.onlyInNewTraceAtStart()V", actual[0].methodCalledInNew); //added method invocation
        assertEquals("myPackage.myClass.onlyInOldTrace()I", actual[1].methodCalledInOld); //method was not called
        assertEquals("myPackage.myClass.onlyInOldTraceAtEnd()V", actual[2].methodCalledInOld); //method was not called
        assertEquals("myPackage.myClass.onlyInNewTrace()I", actual[3].methodCalledInNew); //added method invocation

        //we matched one function call but found a single difference when recursing!
        assertEquals("myPackage.myClass.matchableInner()I", traceDivergences.get(4).getMethodData().getIdentifier());
    }

    @Test
    public void testMethodInvocationMappingOldTraceLonger() throws IOException, URISyntaxException {
        /* scaffolding...    its okay, we will not access this stuff anyway */
        Version dummyVersion1 = new Version(0,"a","","","");
        Version dummyVersion2 = new Version(1,"b","","","");
        dummyVersion1.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);
        dummyVersion2.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);

        /** We want to test the main comparison method based on amount and localization of diverging traces! */
        MethodData.setTraceDistanceBasedComparison(false);
        MethodData.setCoverageBasedComparison(false);


        File oldFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/recursiveOld").toURI());
        File newFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/recursiveNew").toURI());
        JSONObject oldTraceJSON = new JSONObject(FileUtils.readData(oldFile));
        JSONObject newTraceJSON = new JSONObject(FileUtils.readData(newFile));

        /** Inject additional method calls in one of the traces! */
        JSONArray tmp = oldTraceJSON.getJSONArray("trace");
        JSONArray overwrite = new JSONArray();
        overwrite.put(getAdditionalCall());
        overwrite.put(getAdditionalCall());
        overwrite.put(tmp.get(0));
        overwrite.put(tmp.get(1));
        overwrite.put(tmp.get(2));
        overwrite.put(tmp.get(3));
        overwrite.put(tmp.get(4));
        overwrite.put(tmp.get(5));
        overwrite.put(tmp.get(6));
        overwrite.put(getAdditionalCall());
        overwrite.put(getAdditionalCall());
        overwrite.put(getAdditionalCall());
        oldTraceJSON.put("trace", overwrite); //overwrite source of trace 1

        /** Compare the traces, we want the mapping! */
        Trace oldTraceData = new Trace(dummyVersion1,oldTraceJSON, new HashMap<String, List<SyntaxChange>>(), "oldtrace");
        Trace newTraceData = new Trace(dummyVersion2, newTraceJSON, new HashMap<String, List<SyntaxChange>>(), "newtrace");
        List<TraceDivergence> traceDivergences = oldTraceData.compareAgainstNewerTrace(newTraceData, false);
        assertEquals( 2 + //two additional methods
                        7 + //two method less from the file and five were injected before!
                        1 + //one matchable method (has a different return value to generate a diverge instance)
                        2,  //Due to our modifications, we have a trace difference now. TAKE NOTE that in a real
                            //example with code that ACTUALLY makes sense, a difference based only on method
                            //invocations, aka only negative numbers and no actual instructions could not occur!
                traceDivergences.size());


        /** ----------------------    verify the resulting method call matching -----------------------*/
        MethodCallDivergence[] actual = new MethodCallDivergence[traceDivergences.size()];
        int i = 0;
        for (TraceDivergence t : traceDivergences) {
            if (t instanceof MethodCallDivergence) {
                MethodCallDivergence c = (MethodCallDivergence) t;
                actual[i] = c;
            }
            ++i;
        }
        assertTrue(traceDivergences.get(0) instanceof MetricDivergence); //caused by our tinkering...
        assertTrue(traceDivergences.get(1) instanceof MetricDivergence);

        assertEquals("myPackage.someClass.additional()V", actual[2].methodCalledInOld); //injected method not called
        assertEquals("myPackage.someClass.additional()V", actual[3].methodCalledInOld); //injected method not called
        assertEquals("myPackage.myClass.onlyInNewTraceAtStart()V", actual[4].methodCalledInNew); //added method invocation
        assertEquals("myPackage.myClass.onlyInOldTrace()I", actual[5].methodCalledInOld); //method was not called
        assertEquals("myPackage.myClass.onlyInOldTraceAtEnd()V", actual[6].methodCalledInOld); //method was not called
        assertEquals("myPackage.someClass.additional()V", actual[7].methodCalledInOld); //injected method not called
        assertEquals("myPackage.someClass.additional()V", actual[8].methodCalledInOld); //injected method not called
        assertEquals("myPackage.someClass.additional()V", actual[9].methodCalledInOld); //injected method not called
        assertEquals("myPackage.myClass.onlyInNewTrace()I", actual[10].methodCalledInNew); //added method invocation

        //we matched one function call but found a single difference when recursing!
        assertEquals("myPackage.myClass.matchableInner()I", ((ObjectValueDivergence) traceDivergences.get(11)).getMethodData().getIdentifier());
    }


    @Test
    public void testMethodInvocationMappingNewTraceLonger() throws IOException, URISyntaxException {
        /* scaffolding...    its okay, we will not access this stuff anyway */
        Version dummyVersion1 = new Version(0,"a","","","");
        Version dummyVersion2 = new Version(1,"b","","","");
        dummyVersion1.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);
        dummyVersion2.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);

        /** We want to test the main comparison method based on amount and localization of diverging traces! */
        MethodData.setTraceDistanceBasedComparison(false);
        MethodData.setCoverageBasedComparison(false);

        File oldFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/recursiveOld").toURI());
        File newFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/recursiveNew").toURI());
        JSONObject oldTraceJSON = new JSONObject(FileUtils.readData(oldFile));
        JSONObject newTraceJSON = new JSONObject(FileUtils.readData(newFile));

        /** Inject additional method calls in one of the traces! */
        JSONArray tmp = newTraceJSON.getJSONArray("trace");
        JSONArray overwrite = new JSONArray();
        overwrite.put(getAdditionalCall());
        overwrite.put(getAdditionalCall());
        overwrite.put(tmp.get(0));
        overwrite.put(tmp.get(1));
        overwrite.put(tmp.get(2));
        overwrite.put(tmp.get(3));
        overwrite.put(tmp.get(4));
        overwrite.put(tmp.get(5));
        overwrite.put(tmp.get(6));
        overwrite.put(getAdditionalCall());
        overwrite.put(getAdditionalCall());
        overwrite.put(getAdditionalCall());
        newTraceJSON.put("trace", overwrite); //overwrite source of trace 1

        /** Compare the traces, we want the mapping! */
        Trace oldTraceData = new Trace(dummyVersion1,oldTraceJSON, new HashMap<String, List<SyntaxChange>>(), "oldtrace");
        Trace newTraceData = new Trace(dummyVersion2, newTraceJSON, new HashMap<String, List<SyntaxChange>>(), "newtrace");
        List<TraceDivergence> traceDivergences = oldTraceData.compareAgainstNewerTrace(newTraceData, false);
        assertEquals( 7 + //two additional methods from the file and five were injected before!
                        2 + //two method less
                        1 + //one matchable method (has a different return value to generate a diverge instance)
                        2,  //Due to our modifications, we have a trace difference now. TAKE NOTE that in a real
                //example with code that ACTUALLY makes sense, a difference based only on method
                //invocations, aka only negative numbers and no actual instructions could not occur!
                traceDivergences.size());


        /** ----------------------    verify the resulting method call matching -----------------------*/
        MethodCallDivergence[] actual = new MethodCallDivergence[traceDivergences.size()];
        int i = 0;
        for (TraceDivergence t : traceDivergences) {
            if (t instanceof MethodCallDivergence) {
                MethodCallDivergence c = (MethodCallDivergence) t;
                actual[i] = c;
            }
            ++i;
        }
        assertTrue(traceDivergences.get(0) instanceof MetricDivergence); //caused by our tinkering...
        assertTrue(traceDivergences.get(1) instanceof MetricDivergence);

        assertEquals("myPackage.someClass.additional()V", actual[2].methodCalledInNew); //added method invocation
        assertEquals("myPackage.someClass.additional()V", actual[3].methodCalledInNew); //added method invocation
        assertEquals("myPackage.myClass.onlyInNewTraceAtStart()V", actual[4].methodCalledInNew); //added method invocation
        assertEquals("myPackage.myClass.onlyInOldTrace()I", actual[5].methodCalledInOld); //method was not called
        assertEquals("myPackage.myClass.onlyInOldTraceAtEnd()V", actual[6].methodCalledInOld); //method was not called
        assertEquals("myPackage.myClass.onlyInNewTrace()I", actual[7].methodCalledInNew); //added method invocation
        assertEquals("myPackage.someClass.additional()V", actual[8].methodCalledInNew); //added method invocation
        assertEquals("myPackage.someClass.additional()V", actual[9].methodCalledInNew); //added method invocation
        assertEquals("myPackage.someClass.additional()V", actual[10].methodCalledInNew); //added method invocation



        //we matched one function call but found a single difference when recursing!
        assertEquals("myPackage.myClass.matchableInner()I", ((ObjectValueDivergence) traceDivergences.get(11)).getMethodData().getIdentifier());
    }


    @Test
    public void testAlternativeComparisonMethods() throws IOException, URISyntaxException {
        /* scaffolding...    its okay, we will not access this stuff anyway */
        Version dummyVersion1 = new Version(0,"a","","","");
        Version dummyVersion2 = new Version(1,"b","","","");
        dummyVersion1.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);
        dummyVersion2.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);

        /** Enable both alternative methods */
        MethodData.setTraceDistanceBasedComparison(true);
        MethodData.setCoverageBasedComparison(true);

        /**---------------- Compare the traces using coverage and trace distance! ------------------------*/
        File oldFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/nonRecursiveOld").toURI());
        File newFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/nonRecursiveNew").toURI());
        Trace oldTraceData = new Trace(dummyVersion1, new JSONObject(FileUtils.readData(oldFile)),
                new HashMap<String, List<SyntaxChange>>(), "oldtrace");
        Trace newTraceData = new Trace(dummyVersion2, new JSONObject(FileUtils.readData(newFile)),
                new HashMap<String, List<SyntaxChange>>(), "newtrace");

        List<TraceDivergence> traceDivergences = oldTraceData.compareAgainstNewerTrace(newTraceData, false);



        /**---------------- Verify results of coverage ------------------------*/
        //Coverage differs in 11 lines: 2, 3, 4, 5, 10, 11, 12, 14, 15, 39, 40
        assertEquals(12, traceDivergences.size() /* 11 coverage + 1 distance */);
        assertEquals(2, ((CoverageDivergence) traceDivergences.get(0)).instrIndex);
        assertEquals(3, ((CoverageDivergence) traceDivergences.get(1)).instrIndex);
        assertEquals(4, ((CoverageDivergence) traceDivergences.get(2)).instrIndex);
        assertEquals(5, ((CoverageDivergence) traceDivergences.get(3)).instrIndex);
        assertEquals(10, ((CoverageDivergence) traceDivergences.get(4)).instrIndex);
        assertEquals(11, ((CoverageDivergence) traceDivergences.get(5)).instrIndex);
        assertEquals(12, ((CoverageDivergence) traceDivergences.get(6)).instrIndex);
        assertEquals(14, ((CoverageDivergence) traceDivergences.get(7)).instrIndex);
        assertEquals(15, ((CoverageDivergence) traceDivergences.get(8)).instrIndex);
        assertEquals(39, ((CoverageDivergence) traceDivergences.get(9)).instrIndex);
        assertEquals(40, ((CoverageDivergence) traceDivergences.get(10)).instrIndex);
        for (int i = 0; i<11; ++i){
            CoverageDivergence c = (CoverageDivergence) traceDivergences.get(i);
            assertEquals(1, Math.abs(c.numberOfExecutionsInNew-c.numberOfExecutionsInOld));
        }


        /**---------------- Verify results of distance ------------------------*/
        assertTrue(traceDivergences.get(11) instanceof MetricDivergence);
        assertEquals(DivergenceType.TRACE_DISTANCE, traceDivergences.get(11).type);
        assertEquals(9, ((MetricDivergence) traceDivergences.get(11)).metricValue );
    }

    @Test
    public void testFullRecursiveComparison() throws IOException, URISyntaxException {
        /* scaffolding...    its okay, we will not access this stuff anyway */
        Version dummyVersion1 = new Version(0,"a","","","");
        Version dummyVersion2 = new Version(1,"b","","","");
        dummyVersion1.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);
        dummyVersion2.enrichWithToolData(dummyVersion,"","","","","", new String[0],new String[0],new String[0],
                new String[0],dummyVersion1,false,false);
        MethodData.setTraceDistanceBasedComparison(false);
        MethodData.setCoverageBasedComparison(false);

        /** Inject source code for lookup */
        File sourceFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/source").toURI());
        JSONObject jsonObject = new JSONObject(FileUtils.readData(sourceFile));
        jsonObject.put("method","myPackage.myClass.myMethod()V");
        Core.DEBUG_ONLY_injectSourceCode(dummyVersion2, "myPackage.myClass.myMethod()V", new MethodSource(jsonObject));
        injectMiniFunction(dummyVersion2,"myPackage.myClass.methodTakingString(Ljava/lang/String;)V");
        injectMiniFunction(dummyVersion2,"myPackage.myClass.anotherMethod()I");
        injectMiniFunction(dummyVersion2,"myPackage.myClass.methodTakingTwoStrings(Ljava/lang/String;Ljava/lang/String;)V");
        injectMiniFunction(dummyVersion2,"myPackage.myClass.someMethod()V");
        injectMiniFunction(dummyVersion2,"myPackage.myClass.someMethod2()V");


        /**---------------- Compare the traces with source available ------------------------*/
        File oldFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/reallyLongExampleWithNoSourceOld").toURI());
        File newFile = new File (TraceComparisonTest.class.getResource("/for_trace_comparison_test/reallyLongExampleWithNoSourceNew").toURI());
        Trace oldTraceData = new Trace(dummyVersion1, new JSONObject(FileUtils.readData(oldFile)),
                new HashMap<String, List<SyntaxChange>>(), "oldtrace");
        Trace newTraceData = new Trace(dummyVersion2, new JSONObject(FileUtils.readData(newFile)),
                new HashMap<String, List<SyntaxChange>>(), "newtrace");
        List<TraceDivergence> traceDivergences = oldTraceData.compareAgainstNewerTrace(newTraceData, false);

        /**--------------------------- Verify all results ------------------------------------*/

        /** verify results for top method */
        assertTrue(traceDivergences.size() >= 2);
        assertTrue(traceDivergences.get(0) instanceof MetricDivergence);
        assertTrue(traceDivergences.get(1) instanceof MetricDivergence);
        MetricDivergence distanceForTopMethod = (MetricDivergence) traceDivergences.get(0);
        MetricDivergence divergenceForTopMethod = (MetricDivergence) traceDivergences.get(1);
        assertEquals(8, distanceForTopMethod.metricValue); /** distance */
        assertEquals(2, divergenceForTopMethod.metricValue); /** amount of diverging sections */
        assertTrue(traceDivergences.get(2) instanceof MethodCallDivergence); //different method called!
        assertTrue(traceDivergences.get(3) instanceof MethodCallDivergence); //cannot match different methods!
        MethodCallDivergence oldFunctionCalled = (MethodCallDivergence) traceDivergences.get(2);
        MethodCallDivergence newFunctionCalled = (MethodCallDivergence) traceDivergences.get(3);
        assertEquals("myPackage.myClass.nonMatchableMethodInOld()V", oldFunctionCalled.methodCalledInOld);
        assertEquals("myPackage.myClass.nonMatchableMethodInNew()V", newFunctionCalled.methodCalledInNew);

        /** verify results for sub methods (of the recursion) */
        assertTrue(traceDivergences.get(4) instanceof ObjectValueDivergence); //first sub-method returns different value
        ObjectValueDivergence differentReturnValue = (ObjectValueDivergence) traceDivergences.get(4);
        assertEquals("myPackage.myClass.anotherMethod()I", differentReturnValue.getMethodData().getIdentifier());


        assertTrue(traceDivergences.get(5) instanceof ObjectValueDivergence); //second sub-method was called with different parameter
        ObjectValueDivergence differentParameter = (ObjectValueDivergence) traceDivergences.get(5);
        assertEquals("myPackage.myClass.myMethod()V", //parameter differences are stored in the caller!!!
                differentParameter.getMethodData().getIdentifier());


        //and finally, the third sub-method has another sub-method where there is a trace difference!
        assertTrue(traceDivergences.get(6) instanceof MetricDivergence);
        assertTrue(traceDivergences.get(7) instanceof MetricDivergence);
        MetricDivergence distanceForNestedMethod = (MetricDivergence) traceDivergences.get(6);
        MetricDivergence divergenceForNestedMethod = (MetricDivergence) traceDivergences.get(7);
        assertEquals(4, distanceForNestedMethod.metricValue); /** distance for that nested method */
        assertEquals(1, divergenceForNestedMethod.metricValue); /** amount of diverging sections */
    }


    @Test
    public void testGatheringOfMultipleTracesWithManualMatching() throws IOException, URISyntaxException {
        /* scaffolding...    its okay, we will not access this stuff anyway */
        Version dummyVersion1 = new Version(0, "a", "", "", "");
        Version dummyVersion2 = new Version(1, "b", "", "", "");
        File oldDir = new File(TraceComparisonTest.class.getResource("/for_trace_comparison_test/oldVersion").toURI());
        File newDir = new File(TraceComparisonTest.class.getResource("/for_trace_comparison_test/newVersion").toURI());
        dummyVersion1.enrichWithToolData(oldDir, "","","","","", new String[0], new String[0], new String[0],
                new String[0], null, false, false);
        dummyVersion2.enrichWithToolData(newDir, "","","","","", new String[0], new String[0], new String[0],
                new String[0], dummyVersion1, false, false);

        /** We want to test the main comparison method based on amount and localization of diverging traces! */
        MethodData.setTraceDistanceBasedComparison(false);
        MethodData.setCoverageBasedComparison(false);

        TreeMap<Version,HashMap<String, List<SyntaxChange>>> noSyntaxChanges = new TreeMap<>();
        noSyntaxChanges.put(dummyVersion1, new HashMap<String, List<SyntaxChange>>());
        noSyntaxChanges.put(dummyVersion2, new HashMap<String, List<SyntaxChange>>());

        /**----------------------- Test the actual trace gathering functionality ------------------------*/
        Version[] versions = new Version[]{dummyVersion1, dummyVersion2};
        TreeMap<Version, Trace[]> traces = Core.gatherTraces(versions, noSyntaxChanges, false, false);
        Trace[] tracesInOld = traces.get(dummyVersion1);
        Trace[] tracesInNew = traces.get(dummyVersion2);
        assertEquals(6, tracesInOld.length); //there are six traces contained in 2 files
        assertEquals(7, tracesInNew.length); //there are seven traces contained in 3 files

        /**----------------------- Test the trace comparison on that data -------------------------------*/
        TreeMap<Version, TraceDivergence[][]> traceDivergences = MainFunctionality.step7_compareTraces(versions, traces,
                false, true, false);
        assertFalse(traceDivergences.keySet().contains(dummyVersion1)); //computed only for more recent version!
        assertTrue(traceDivergences.keySet().contains(dummyVersion2));
        assertEquals(1, traceDivergences.size()); //no magic versions have been added out of nowhere
        assertEquals(7, traceDivergences.get(dummyVersion2).length); //7 traces are in newer version
        int numberOfIdentical = 0;
        int numberOfUnmatched = 0;
        for (TraceDivergence[] d : traceDivergences.get(dummyVersion2)){
            if (d == null){
                //could not be matched
                ++numberOfUnmatched;
            }
            else if (d.length == 0){
                //no divergences detected
                ++numberOfIdentical;
            }
        }
        assertEquals(1, numberOfUnmatched); //one trace was not matched
        assertEquals(4, numberOfIdentical); //four traces are identical
        //and 2 have detected divergences
        File divergenceFileOld = new File(dummyVersion1.getMainDirectory().getAbsoluteFile() + "/traceDivergences");
        File divergenceFileNew = new File(dummyVersion2.getMainDirectory().getAbsoluteFile() + "/traceDivergences");
        File expectedFile = new File(dummyVersion2.getMainDirectory().getAbsoluteFile() + "/expectedDivergences");

        JSONArray actualDivergences = new JSONArray();
        JSONArray expectedDivergences = new JSONArray(FileUtils.readData(expectedFile));

        boolean failBecauseOldExists = false;
        boolean failBecauseNewDoesNotExist = false;
        if (divergenceFileOld.exists()){
            divergenceFileOld.delete();
            failBecauseOldExists = true;
        }
        if (divergenceFileNew.exists()){
            String expected = FileUtils.readData(divergenceFileNew);
            actualDivergences = new JSONArray(expected);
            divergenceFileNew.delete();
        } else failBecauseNewDoesNotExist = true;
        if (failBecauseNewDoesNotExist) fail("traceDivergences-file does not exist!");
        if (failBecauseOldExists) fail("traceDivergences-file was in the wrong version!");

        //verify the results!
        assertEquals(expectedDivergences.length(), actualDivergences.length() -1 /* we will report the unmatched one */);

        JSONObject expectedFirstTrace = expectedDivergences.getJSONObject(0);
        JSONObject actualFirstTrace = actualDivergences.getJSONObject(1);
        assertEquals(expectedFirstTrace.length(), actualFirstTrace.length());
        assertEquals(expectedFirstTrace.getJSONArray("methodsThatDiverged").length(),
                actualFirstTrace.getJSONArray("methodsThatDiverged").length());

        for (int i = 0; i<expectedFirstTrace.getJSONArray("methodsThatDiverged").length(); ++i){
            JSONObject expectedDiv = expectedFirstTrace.getJSONArray("methodsThatDiverged").getJSONObject(i);
            JSONObject actualDiv = actualFirstTrace.getJSONArray("methodsThatDiverged").getJSONObject(i);
            assertEquals(expectedDiv.getJSONArray("traceDivergences").length(),
                    actualDiv.getJSONArray("traceDivergences").length());
        }

        JSONObject expectedSecondTrace = expectedDivergences.getJSONObject(1);
        JSONObject actualSecondTrace = actualDivergences.getJSONObject(2);
        assertEquals(expectedSecondTrace.length(), actualSecondTrace.length());
        assertEquals(expectedSecondTrace.getJSONArray("methodsThatDiverged").length(),
                actualSecondTrace.getJSONArray("methodsThatDiverged").length());

        for (int i = 0; i<expectedSecondTrace.getJSONArray("methodsThatDiverged").length(); ++i){
            JSONObject expectedDiv = expectedSecondTrace.getJSONArray("methodsThatDiverged").getJSONObject(i);
            JSONObject actualDiv = actualSecondTrace.getJSONArray("methodsThatDiverged").getJSONObject(i);
            assertEquals(expectedDiv.getJSONArray("traceDivergences").length(),
                    actualDiv.getJSONArray("traceDivergences").length());
        }
    }


    @Before
    public void setUp() throws Exception {
        Tool.activateDebugMode();
        dummyVersion = Files.createTempDirectory("unit_testing_of_master_thesis____shouldNotBeAccessedAnyway").toFile();
    }

    @After
    public void cleanUp() throws Exception {
        FileUtils.removeDirectory(dummyVersion);
    }
}
