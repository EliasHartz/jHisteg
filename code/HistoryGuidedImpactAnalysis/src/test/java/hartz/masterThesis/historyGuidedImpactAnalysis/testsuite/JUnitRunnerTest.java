package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution.Executor;
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
import java.util.HashSet;

import static org.junit.Assert.*;

public class JUnitRunnerTest extends TestingConstants {
   
   private File testDir;

    public JUnitRunnerTest() throws URISyntaxException {
        testDir = new File(JUnitRunnerTest.class.getResource("/for_junit_runner_test").toURI());
    }

    /**
    * Tests that JUnit tests can be run from inside this program.
    */

   @Test
   public void testJUnitRunner() throws IOException {
      File classpath = new File(testDir.getAbsolutePath()+"/build");
      File testDirectory = new File(testDir.getAbsolutePath()+"/build/tests");
      Result r = JUnitAdapter.runJUnitTests(MemorizingClassLoader.getClassLoader(classpath), testDirectory, new HashSet<String>());
      assertTrue(r.wasSuccessful());
      assertEquals(3, r.getRunCount()); //three tests in two files!
      assertEquals(0, r.getFailureCount());
      assertEquals(0, r.getIgnoreCount());
   }
   
   
   @Before
   public void setUp() throws Exception {
      Tool.activateDebugMode();
      if (!testDir.exists() || !testDir.isDirectory()) fail("required resources for testing not found!");
      Executor.execute("ant compile", testDir);
   }
 
   
   @After
   public void tearDown() throws Exception {
      if (!testDir.exists() || !testDir.isDirectory()) fail("resources not found after test execution!");
      Executor.execute("ant clean", testDir);
      Tool.print("\n\n");
   }
}
