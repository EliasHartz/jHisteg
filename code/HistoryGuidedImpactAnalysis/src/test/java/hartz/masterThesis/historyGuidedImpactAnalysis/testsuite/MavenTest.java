package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestOnTestRepo;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestingConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class MavenTest extends TestingConstants {

    protected File testingOutputFolder;
    protected File testRepoZip;
    protected File locationOfTestRepo;
    protected String pathToTestRepo;

    public MavenTest() {
        try {
            testRepoZip = new File(TestOnTestRepo.class.getResource("/for_maven_test.zip").toURI());
        } catch (URISyntaxException e) {
            fail("required resources not found!");
        }
    }

    @Test
    public void dealWithMavenRepoTest() throws IOException {
        Tool.main(new String[]{
                Commands.compute,
                Commands.loud,
                Commands.generateTargetsForSyntax,
                Commands.generateTargetsForDivergences,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.nameOfRepo, "MavenTest",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.relativePathToSources, "src/main/java",
                Commands.relativePathToCompiledSources, "target/classes",
                Commands.relativePathToAdditionalCompiledSources, "target/test-classes",
                Commands.buildCommand, "clean install -DskipTests",
                Commands.runCommand, "mvn test",
                Commands.versions, "[dda9fd0e4768b6025b991a4200ab495a98aee5ce, 3e3b7266df84e6b9b9b1b45d8fc574612c2927b4]"
        });

        assertEquals(2 + 1/*=history file*/, testingOutputFolder.listFiles().length);
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(0)), "code_before_instrumentation").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(0)), "code_under_observation").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(0)), "code_before_instrumentation_additional").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(0)), "code_under_observation_additional").exists());
        assertFalse(new File(new File(testingOutputFolder, Integer.toString(0)), "testingTargets_traceDivergences").exists());
        assertFalse(new File(new File(testingOutputFolder, Integer.toString(0)), "testingTargets").exists());
        assertFalse(new File(new File(testingOutputFolder, Integer.toString(0)), "testingTargets_syntacticalChanges").exists());
        assertFalse(new File(new File(testingOutputFolder, Integer.toString(0)), "testingTargets_traceDivergences").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(0)), "observedTrace").exists());
        assertFalse(new File(new File(testingOutputFolder, Integer.toString(0)), "observedTrace_1").exists());

        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "code_before_instrumentation").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "code_under_observation").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "code_before_instrumentation_additional").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "code_under_observation_additional").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "testingTargets").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "testingTargets_syntacticalChanges").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "testingTargets_traceDivergences").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(1)), "observedTrace").exists());
        assertFalse(new File(new File(testingOutputFolder, Integer.toString(1)), "observedTrace_1").exists());
    }

    @Before
    public void setUp() throws Exception {
        Tool.activateDebugMode();
        File f = Files.createTempDirectory("maven_testing").toFile();
        if (f.exists()){
            if (f.isDirectory()) {
                if (f.listFiles().length != 0) {
                    FileUtils.removeDirectory(f);
                    f.mkdirs();
                }
                //else it is okay because it is empty:-)
            }
            else throw new InternalError("There is a file named '" + f.getName()+ "' present but expected is a directory!");
        }
        else f.mkdirs(); //simply create it
        testingOutputFolder = f;

        //extract our test repository
        File r = testRepoZip.getParentFile();
        FileUtils.unzip(testRepoZip, r);
        locationOfTestRepo = new File(r,"maventest");
        assertTrue(locationOfTestRepo.exists());
        assertTrue(locationOfTestRepo.isDirectory());
        pathToTestRepo = locationOfTestRepo.getAbsolutePath();
    }


    @After
    public void tearDown() throws Exception {
        FileUtils.removeDirectory(testingOutputFolder);
        FileUtils.removeDirectory(locationOfTestRepo);
        Tool.print("\n\n");
    }
}
