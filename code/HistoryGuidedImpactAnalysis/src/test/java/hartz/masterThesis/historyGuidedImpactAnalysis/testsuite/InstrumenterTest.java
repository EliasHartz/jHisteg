package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution.Executor;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.instrumentation.Instrumenter;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.traces.traceData.MethodData;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.versions.Version;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.CoverageObserverNoHook;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.junitAdapter.JUnitAdapter;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.MemorizingClassLoader;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestingConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

public class InstrumenterTest extends TestingConstants {

    private File resourceDir;
    private File testResultDir;

    public InstrumenterTest() throws URISyntaxException {
        resourceDir = new File(InstrumenterTest.class.getResource("/for_instrumenter_test").toURI());
    }

    private MethodData[] getData(File f) throws IOException {
        if (!f.exists() || !f.isFile()) fail("File IO error, "+f.getAbsolutePath()+" is invalid!");

        JSONObject result = new JSONObject(FileUtils.readData(f));
        JSONArray traces = result.getJSONArray("executionTraces");

        if (result.has("errors"))
            assertTrue(result.getJSONArray("errors").length() == 0);

        MethodData[] data = new MethodData[traces.length()];
        for (int i = 0; i<traces.length(); ++i){
            Version dummyVersion = new Version(0,"a","","","");
            dummyVersion.enrichWithToolData(f,".",".",".",".","", new String[0],new String[0],new String[0],
                    new String[0],dummyVersion,false,false);
            MethodData m = new MethodData(dummyVersion, traces.getJSONObject(i), null,
                    new HashMap<String, List<SyntaxChange>>());
            data[i] = m;
        }
        return data;
    }

    @Test
    public void testCoverageObservationMinimal() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_minimal_test";
        File coverageFile = new File(testResultDir,testName);

        //instrument both source and desired test!
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.SomeClass",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "tests.SomeMinimalTest",
                coverageFile.getAbsolutePath()
        });

        new CoverageObserverNoHook(coverageFile); //see documentation
        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.SomeMinimalTest", new LinkedList<String>()));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }

    /**
     * Since I need equals() to be reference-equality for MethodData so that other
     * parts of the code working with collections of MethoData objects function,
     * I cannot use arrayEquals! Therefore, this method exists.
     */
    private void verify(MethodData[] expectedData, MethodData[] actualData) {
        assertEquals(expectedData.length, actualData.length);
        for (int i = 0; i<actualData.length; ++i){
            MethodData expected = expectedData[i];
            MethodData actual = actualData[i];
            assertTrue(expected.equalsMethodData(actual));
        }
    }

    private void verify(HashSet<MethodData> expectedData, HashSet<MethodData> actualData) {
        assertEquals(expectedData.size(), actualData.size());
        for (MethodData expected : expectedData){
            boolean found = false;
            Iterator<MethodData> iter = actualData.iterator();

           while(iter.hasNext()){
                   MethodData actual = iter.next();
                   if (expected.equalsMethodData(actual)) {
                       if (!found){
                           found = true;
                           iter.remove(); //avoid dupilcate finds!
                       }
                   }
            }
            if (!found) fail("NOT FOUND:" +expected);
        }
    }

    @Test
    public void testCoverageObservationTwoTestFiles() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_two_test_files";
        File coverageFile = new File(testResultDir,testName);

        //instrument both source and desired test!
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.SomeClass",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "tests.SomeMinimalTest",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "tests.AnotherMinimalTest",
                coverageFile.getAbsolutePath()
        });

        new CoverageObserverNoHook(coverageFile); //see documentation
        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.SomeMinimalTest"));
        Result r2 = JUnitAdapter.runJUnitTests(loader.loadClass("tests.AnotherMinimalTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(r2.wasSuccessful());
        assertEquals(1, r2.getRunCount());
        assertEquals(0, r2.getFailureCount());
        assertEquals(0, r2.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }


    @Test
    public void testSingeLineLoopObservation() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_single_line_loop_special_case";
        File coverageFile = new File(testResultDir,testName);

        //instrument only source, we do not need more
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.SomeClass",
                coverageFile.getAbsolutePath()
        });

        new CoverageObserverNoHook(coverageFile); //see documentation
        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.SingleLineLoopTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }

    @Test
    public void testCoverageObservationIgnoredMethods() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_ignored_methods";
        File coverageFile = new File(testResultDir,testName);
        new CoverageObserverNoHook(coverageFile); //see documentation

        //instrument both source and desired test!
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.SomeClass",
                coverageFile.getAbsolutePath(),
                "toString()Ljava/lang/String;",
                "hashCode()I",
                "equals(Ljava/lang/Object;)Z"
        });

        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.IgnoredMethodsTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }

    @Test
    public void testCoverageObservationSingleThread() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_single_thread";
        File coverageFile = new File(testResultDir,testName);
        new CoverageObserverNoHook(coverageFile); //see documentation

        //instrument both source and desired test!
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.SomeClass",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "tests.SingleThreadedTest",
                coverageFile.getAbsolutePath()
        });

        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.SingleThreadedTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }




    @Test
    public void testCoverageObservationMultiThread() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_multiple_threads";
        File coverageFile = new File(testResultDir,testName);
        new CoverageObserverNoHook(coverageFile); //see documentation

        //instrument both source and desired test!
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.SomeClass",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "tests.MultithreadedTest",
                coverageFile.getAbsolutePath()
        });

        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.MultithreadedTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //since the result is non-deterministic, verification is a bit more forgiving here!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        assertEquals(expectedData.length, actualData.length);

        HashSet<MethodData> expected = new HashSet<>(Arrays.asList(expectedData));
        HashSet<MethodData> actual = new HashSet<>(Arrays.asList(actualData));
        verify(expected, actual);
    }

    @Test
    public void testCoverageObservationTestNotInstrumented() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_test_not_instrumented";
        File coverageFile = new File(testResultDir,testName);
        new CoverageObserverNoHook(coverageFile); //see documentation

        //instrument only source, we expect therefore only ONE call to be observed as the test just calls a static method!
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.SomeClass",
                coverageFile.getAbsolutePath()
        });

        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.SomeMinimalTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }


    @Test
    public void testCoverageObservationInnerClassTrace() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_inner_class_trace";
        File coverageFile = new File(testResultDir,testName);
        new CoverageObserverNoHook(coverageFile); //see documentation

        //no need to instrument the test, we do not need to bloat our trace...
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.ClassWithInnerClass",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.ClassWithInnerClass$InnerClass",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.ClassWithInnerClass$PrivateInnerClass",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.ClassWithInnerClass$StaticInnerClass",
                coverageFile.getAbsolutePath()
        });

        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.InnerClassTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());

        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }

    @Test
    public void testCoverageObservationAnonymousInnerClassTrace() throws IOException {
        File classpath = new File(resourceDir.getAbsolutePath()+"/build");
        String testName = "coverage_anonymous_inner_class_trace";
        File coverageFile = new File(testResultDir,testName);
        new CoverageObserverNoHook(coverageFile); //see documentation

        //no need to instrument the test, we do not need to bloat our trace...
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.ReallyEvilInnerClassCase",
                coverageFile.getAbsolutePath()
        });
        Instrumenter.main(new String[]{
                classpath.getAbsolutePath(),
                "mainPackage.ReallyEvilInnerClassCase$1",
                coverageFile.getAbsolutePath()
        });

        MemorizingClassLoader loader = MemorizingClassLoader.getClassLoader(classpath);
        Result r = JUnitAdapter.runJUnitTests(loader.loadClass("tests.ReallyEvilInnerClassCaseTest"));
        CoverageObserverNoHook.getCurrentInstance().export();
        assertTrue(r.wasSuccessful());
        assertEquals(1, r.getRunCount());
        assertEquals(0, r.getFailureCount());
        assertEquals(0, r.getIgnoreCount());
        assertTrue(coverageFile.exists());
        
        //verify result!
        MethodData[] expectedData = getData(new File(resourceDir.getAbsolutePath()+"/expected/"+testName));
        MethodData[] actualData = getData(coverageFile);
        verify(expectedData, actualData);
    }

    @Before
    public void setUp() throws Exception {
        Tool.activateDebugMode();
        if (!resourceDir.exists() || !resourceDir.isDirectory()) fail("required resources for testing not found!");
        FileUtils.clearCodeFromProjectUnderTest(new File(resourceDir,"build"));
        Executor.execute("ant clean", resourceDir);
        Executor.execute("ant compileDebug", resourceDir);
        testResultDir =  Files.createTempDirectory("unit_testing___uds_master_instrumentation_test").toFile();
        testResultDir.mkdirs();

    }

    @After
    public void tearDown() throws Exception {
        if (!resourceDir.exists() || !resourceDir.isDirectory()) fail("resources were nowhere to be found after testing");
        Executor.execute("ant clean", resourceDir);
        FileUtils.removeDirectory(testResultDir);
        Tool.print("\n\n");
    }
}
