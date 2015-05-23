package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestOnTestRepo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class FullComputationTest extends TestOnTestRepo {

    @Test
    public void fullExecutionTest() throws IOException {
        Tool.main(new String[] {
                Commands.compute,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.nameOfRepo, "UDS_Master_TestRepo",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.runCommand, "ant test",
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommand, "ant compile",
                Commands.versions, "343c0f09364e4f14ecb82faabefcb07d1ea82b18"
        });

        assertEquals(2 + 1/*=history file*/,testingOutputFolder.listFiles().length);

        //---------------       verify version 16, our base version     --------------------------------//
        //-----     "cleaned up stuff, removed main methods, added 'capital' functionality!"    --------//
        assertFalse(new File(new File(testingOutputFolder, Integer.toString(16)), "testingTargets").exists());
        assertFalse(new File(new File(testingOutputFolder,Integer.toString(16)), "testingTargets_syntacticalChanges").exists());
        assertFalse(new File(new File(testingOutputFolder,Integer.toString(16)), "testingTargets_traceDivergences").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(16)), "observedTrace").exists());
        assertTrue(new File(new File(testingOutputFolder, Integer.toString(16)), "observedTrace_1").exists());


        //------------------------       verify version 17     -----------------------------------------//
        //-----------       "added compilation options with debug flags"             -------------------//
        File testingTargets17 = new File(new File(testingOutputFolder,Integer.toString(17)),
                "testingTargets");
        assertFalse(new File(new File(testingOutputFolder,Integer.toString(17)), "testingTargets_syntacticalChanges").exists());
        assertFalse(new File(new File(testingOutputFolder,Integer.toString(17)), "testingTargets_traceDivergences").exists());

        /** This is actually an interesting case, because the syntax change causes a different object state.
         *  The resulting divergence cannot be mapped to a syntax change higher up in the call chain */
        JSONArray results17 = new JSONArray(FileUtils.readData(testingTargets17));
        assertEquals(2,results17.length());
    }

    @Test
    public void complexFullExecutionTest() throws IOException {
        Tool.main(new String[] {
                Commands.compute,
                Commands.loud,
                Commands.generateTargetsForSyntax,
                Commands.generateTargetsForDivergences,
                Commands.pathToLocalRepo, pathToTestRepo,
                Commands.nameOfRepo, "UDS_Master_TestRepo",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.executeMainShort,
                "[       [],[]," +
                        "[],[],"+
                        "[],[mainPackage.HelloWorldMain], " + //this class has NO main method, no trace should be generated!
                        "[], [mainPackage.helloWorld.HelloWorldMain,otherPackage.IhateTheWorld]]", //only the last has valid mains!
                Commands.executeMainArgs,
                "[ [],[],   [],[],     [],[],    []," + //only the last version has main methods!
                        "[\"firstArgument secondArgument\",\"aaa bbb ccc\"]]",
                Commands.runCommand,
                "[[ant test], [ant test],"+
                        "[ant test], [ant test],"+
                        "[ant test], [ant test],"+
                        "[], [ant test]]",
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommand,
                "[ant compile, ant compile,"+
                        "ant compile, ant compile,"+
                        "ant compile, ant compileDebug,"+
                        "ant compileDebug, ant compileDebug]",
                Commands.executeTests,
                "[[],[],"+
                        "[tests.TestSuite], [tests.TestSuite, tests.TestSuite2],"+ //mix it up here, combine with ANT!
                        "[], [],"+
                        "[tests.TestSuite2]," /* mix it up again, alone this time! */ +" []]",
                Commands.versions,
                "[7dd879abd07ce4b44cd32e3118d77cb6f629354c, d54e78a961875c98104078344c7b22e4e4a27a5e," +
                        "c04b319ad5e0ca100f386f41f0d4d3acd63e584c, 058139da914af41fd7048d5fc144cc40c9f38c15,"+
                        "343c0f09364e4f14ecb82faabefcb07d1ea82b18, f482944d22c2279271b1195ebf4053245455aa20,"+
                        "4e4c79c85a3b4f8937c046f18be3bc943ae95748, 697c21a0fff918d4a3398062fdce17eccb2b4545]"
        });

        assertEquals(8 + 1/*=history file*/,testingOutputFolder.listFiles().length);

        //verify correct amount of generated traces
        int[] expectedTraces = new int[]{
                1, /* only one trace created by ANT*/                 2, /* two tests, created via ANT */
                3, /* two via ANT and one test called directly*/      3, /* two via ANT and two called (but they share one trace file!)*/
                2, /* two via ANT and different compile command*/     2, /* two via ANT and NONE by main method, as it is invalid! */
                1, /* only one test called directly */                4 /* finally, two tests via ANT and two main method calls!*/
        };
        for (int i = 13 ; i != 21; ++i){
            File dir = new File(testingOutputFolder,Integer.toString(i));
            assertTrue(dir.exists() && dir.isDirectory());

            int traceCount = 0;
            for (File f : dir.listFiles()){
                if (f.getName().startsWith("observedTrace")) ++traceCount;
            }
            assertEquals(expectedTraces[i-13], traceCount);
        }

        //-----------------   verify version 13, our base version     ----------------------------------//
        File testingTargets13 = new File(new File(testingOutputFolder,Integer.toString(13)),
                "testingTargets");
        File testingTargetsSyntax13 = new File(new File(testingOutputFolder,Integer.toString(13)),
                "testingTargets_syntacticalChanges");
        File testingTargetsDivergences13 = new File(new File(testingOutputFolder,Integer.toString(13)),
                "testingTargets_traceDivergences");
        File observedTrace13_1 = new File(new File(testingOutputFolder,Integer.toString(13)),
                "observedTrace");
        File observedTrace13_2 = new File(new File(testingOutputFolder,Integer.toString(13)),
                "observedTrace_1");
        assertFalse(testingTargets13.exists()); //it is the base against we compare, this must not exist!
        assertFalse(testingTargetsSyntax13.exists());
        assertFalse(testingTargetsDivergences13.exists());
        assertTrue(observedTrace13_1.exists()); //this must exist, since we need trace data to compare against!
        assertFalse(observedTrace13_2.exists()); //this version has only one test class!


        //------------------------       verify version 14     -----------------------------------------//
        //------     "added second test class with two tests and assertions inside"    -----------------//
        File testingTargets14 = new File(new File(testingOutputFolder,Integer.toString(14)),
                "testingTargets");
        File testingTargetsSyntax14 = new File(new File(testingOutputFolder,Integer.toString(14)),
                "testingTargets_syntacticalChanges");
        File testingTargetsDivergences14 = new File(new File(testingOutputFolder,Integer.toString(14)),
                "testingTargets_traceDivergences");
        File observedTrace14_1 = new File(new File(testingOutputFolder,Integer.toString(14)),
                "observedTrace");
        File observedTrace14_2 = new File(new File(testingOutputFolder,Integer.toString(14)),
                "observedTrace_1");
        JSONArray results14 = new JSONArray(FileUtils.readData(testingTargets14));
        assertEquals(0,results14.length());
        results14 = new JSONArray(FileUtils.readData(testingTargetsSyntax14));
        assertEquals(0,results14.length());
        results14 = new JSONArray(FileUtils.readData(testingTargetsDivergences14));
        assertEquals(0,results14.length());
        assertTrue(observedTrace14_1.exists());
        assertTrue(observedTrace14_2.exists());


        //------------------------       verify version 15     -----------------------------------------//
        //------     "IDENTICAL TO PREDECESSOR, only space and tab changes in both classes"    ---------//
        File testingTargets15 = new File(new File(testingOutputFolder,Integer.toString(15)),
                "testingTargets");
        File testingTargetsSyntax15 = new File(new File(testingOutputFolder,Integer.toString(15)),
                "testingTargets_syntacticalChanges");
        File testingTargetsDivergences15 = new File(new File(testingOutputFolder,Integer.toString(15)),
                "testingTargets_traceDivergences");
        File observedTrace15_1 = new File(new File(testingOutputFolder,Integer.toString(15)),
                "observedTrace");
        File observedTrace15_2 = new File(new File(testingOutputFolder,Integer.toString(15)),
                "observedTrace_1");
        JSONArray results15 = new JSONArray(FileUtils.readData(testingTargets15));
        assertEquals(0,results15.length());
        results15 = new JSONArray(FileUtils.readData(testingTargetsSyntax15));
        assertEquals(0,results15.length());
        results15 = new JSONArray(FileUtils.readData(testingTargetsDivergences15));
        assertEquals(0,results15.length());
        assertTrue(observedTrace15_1.exists());
        assertTrue(observedTrace15_2.exists());


        //------------------------       verify version 16     -----------------------------------------//
        //-----     "cleaned up stuff, removed main methods, added 'capital' functionality!"    --------//
        File testingTargets16 = new File(new File(testingOutputFolder,Integer.toString(16)),
                "testingTargets");
        File testingTargetsSyntax16 = new File(new File(testingOutputFolder,Integer.toString(16)),
                "testingTargets_syntacticalChanges");
        File testingTargetsDivergences16 = new File(new File(testingOutputFolder,Integer.toString(16)),
                "testingTargets_traceDivergences");
        File observedTrace16_1 = new File(new File(testingOutputFolder,Integer.toString(16)),
                "observedTrace");
        File observedTrace16_2 = new File(new File(testingOutputFolder,Integer.toString(16)),
                "observedTrace_1");

        assertTrue(observedTrace16_1.exists());
        assertTrue(observedTrace16_2.exists());
        assertTrue(testingTargetsDivergences16.exists());
        JSONArray results16 = new JSONArray(FileUtils.readData(testingTargetsSyntax16));
        assertEquals(6, results16.length()); //there where 6 methods changed overall!
        results16 = new JSONArray(FileUtils.readData(testingTargets16));
        assertEquals(7, results16.length()); //six methods but one divergence cannot be mapped -> seven!


        //------------------------       verify version 18     -----------------------------------------//
        //-----------       "added compilation options with debug flags"             -------------------//
        File testingTargets18 = new File(new File(testingOutputFolder,Integer.toString(18)),
                "testingTargets");
        File testingTargetsSyntax18 = new File(new File(testingOutputFolder,Integer.toString(18)),
                "testingTargets_syntacticalChanges");
        File testingTargetsDivergences18 = new File(new File(testingOutputFolder,Integer.toString(18)),
                "testingTargets_traceDivergences");
        JSONArray results18 = new JSONArray(FileUtils.readData(testingTargets18));
        assertEquals(0,results18.length());
        results18 = new JSONArray(FileUtils.readData(testingTargetsSyntax18));
        assertEquals(0,results18.length());
        results18 = new JSONArray(FileUtils.readData(testingTargetsDivergences18));
        assertEquals(0,results18.length());



        //TODO verify more versions

    }
}
