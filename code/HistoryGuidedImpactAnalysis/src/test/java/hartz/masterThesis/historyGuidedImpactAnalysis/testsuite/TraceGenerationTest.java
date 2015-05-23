package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestOnTestRepo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class TraceGenerationTest extends TestOnTestRepo {

    @Test
    public void testTraceGenerationViaCommand() throws IOException {
        Tool.main(new String[] {
                Commands.analyze,
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommand, /*ant*/ "compile",
                Commands.loud,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.nameOfRepo, "UDS_Master_TestRepo",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.runCommand,
                "ant test",
                Commands.versions,
                "[",
                "c04b319ad5e0ca100f386f41f0d4d3acd63e584c",
                "058139da914af41fd7048d5fc144cc40c9f38c15",
                "343c0f09364e4f14ecb82faabefcb07d1ea82b18",
                "]"
        });
        assertEquals(3 + 1/*=history file*/,testingOutputFolder.listFiles().length);

        //verify extraction!
        File dir15 = new File(testingOutputFolder,"15");
        File dir16 = new File(testingOutputFolder,"16");
        File dir17 = new File(testingOutputFolder,"17");
        assertTrue(dir15.exists() && dir15.isDirectory());
        assertTrue(dir16.exists() && dir16.isDirectory());
        assertTrue(dir17.exists() && dir17.isDirectory());

        //verify that all tests were run and that there are two trace files (because ANT starts the JVM twice!)
        File coverageOfFirstTest15 = new File(dir15,"observedTrace");
        File coverageOfFirstTest16 = new File(dir16,"observedTrace");
        File coverageOfFirstTest17 = new File(dir17,"observedTrace");
        assertTrue(coverageOfFirstTest15.exists() && coverageOfFirstTest15.isFile());
        assertTrue(coverageOfFirstTest16.exists() && coverageOfFirstTest16.isFile());
        assertTrue(coverageOfFirstTest17.exists() && coverageOfFirstTest17.isFile());
        File coverageOfSecondTest15 = new File(dir15,"observedTrace_1");
        File coverageOfSecondTest16 = new File(dir16,"observedTrace_1");
        File coverageOfSecondTest17 = new File(dir17,"observedTrace_1");
        assertTrue(coverageOfSecondTest15.exists() && coverageOfSecondTest15.isFile());
        assertTrue(coverageOfSecondTest16.exists() && coverageOfSecondTest16.isFile());
        assertTrue(coverageOfSecondTest17.exists() && coverageOfSecondTest17.isFile());
    }

    @Test
    public void testTraceGenerationViaTestCommand() throws IOException {
        Tool.main(new String[] {
                Commands.analyze,
                Commands.loud,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.nameOfRepo, "UDS_Master_TestRepo",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions, "058139da914af41fd7048d5fc144cc40c9f38c15",
                Commands.executeTests, "tests.TestSuite",
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommand, /*ant*/ "compile",
        });
        assertEquals(2 + 1/*=history file*/,testingOutputFolder.listFiles().length);

        //verify extraction (account for automatic predecessor extraction)!
        File dir15 = new File(testingOutputFolder,"15");
        File dir16 = new File(testingOutputFolder,"16");
        assertTrue(dir15.exists() && dir15.isDirectory());
        assertTrue(dir16.exists() && dir16.isDirectory());

        //verify that only one test was run for each version!
        File coverageOfFirstTest15 = new File(dir15,"observedTrace");
        File coverageOfFirstTest16 = new File(dir16,"observedTrace");
        assertTrue(coverageOfFirstTest15.exists() && coverageOfFirstTest15.isFile());
        assertTrue(coverageOfFirstTest16.exists() && coverageOfFirstTest16.isFile());
    }

    @Test
    public void testTraceGenerationViaMainMethod() throws IOException {
        Tool.main(new String[] {
                Commands.analyze,
                Commands.loud,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.nameOfRepo, "UDS_Master_TestRepo",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions, "697c21a0fff918d4a3398062fdce17eccb2b4545",
                Commands.executeMainShort, "mainPackage.helloWorld.HelloWorldMain",
                Commands.executeMainArgs, "[lala,lalala]",
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommand, /*ant*/ "compile",
        });
        assertEquals(2 + 1/*=history file*/,testingOutputFolder.listFiles().length);

        //verify extraction (account for automatic predecessor extraction)!
        File dir19 = new File(testingOutputFolder,"19");
        File dir20 = new File(testingOutputFolder,"20");
        assertTrue(dir19.exists() && dir19.isDirectory());
        assertTrue(dir20.exists() && dir20.isDirectory());

        //verify that only one test was run for each version!
        File mainMethodIn19 = new File(dir19,"observedTrace");
        File mainMethodIn20 = new File(dir20,"observedTrace");
        assertFalse(mainMethodIn19.exists()); //there was NO main method to call in this version!
        assertTrue(mainMethodIn20.exists() && mainMethodIn20.isFile());
    }
}
