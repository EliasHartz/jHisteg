package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import hartz.masterThesis.historyGuidedImpactAnalysis.configuration.constants.Globals;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.syntax.SyntaxChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestingConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;

import static org.junit.Assert.*;

public class SyntaxChangeComputationTest extends TestingConstants {

    private final File locationOfAlreadyExtracted;

    public SyntaxChangeComputationTest() throws URISyntaxException {
        locationOfAlreadyExtracted = new File(SyntaxChangeComputationTest.class.getResource("/for_syntax_analysis_test/extractedversions").toURI());
    }

    private void verify(int versionIndexStart, int versionIndexEndInclusive,
                        int[] amountOfDetectedChanges, SyntaxChange[][] expectedChanges) throws IOException {
        for (int i = versionIndexStart; i<=versionIndexEndInclusive; ++i) {
            File syntaxChangeFile = new File(locationOfAlreadyExtracted.getAbsolutePath()+"/"+i+"/syntacticCodeChanges");
            if (i == versionIndexStart){
                assertFalse(syntaxChangeFile.exists());
                continue;
            }
            else{
                Tool.print(" - starting verification of analysis for commit "+i);
                assertTrue(syntaxChangeFile.exists());
            }

            String data = FileUtils.readData(syntaxChangeFile);
            JSONArray a = new JSONArray(data);
            HashSet<SyntaxChange> changes = new HashSet<>();
            for (int j=0; j<a.length();++j){
                SyntaxChange change = SyntaxChange.fromJSON(a.getJSONObject(j));
                changes.add(change);
            }

            //verify the size
            if (amountOfDetectedChanges[i-versionIndexStart-1] >= 0)
                assertEquals(amountOfDetectedChanges[i-versionIndexStart-1], changes.size());

            for (SyntaxChange expectedChange : expectedChanges[i-versionIndexStart-1]){
                if (changes.contains(expectedChange)){
                    changes.remove(expectedChange);
                }
                else fail("expected {"+expectedChange+"} was not detected");

            }
            assertTrue(changes.isEmpty()); //we found everything we were looking for and nothing more!
        }
    }

    @Test
    public void testSyntaxAnalysisSimpleCases() throws IOException {
        Tool.main(new String[]{
                Commands.analyze,
                Commands.nameOfRepo, "testrepo",
                Commands.noRepo,
                Commands.outputFolder, locationOfAlreadyExtracted.getAbsolutePath(),
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommandShort, "compile",
                Commands.versions,
                "[",
                "2189a143aeb3884ae451eec1bb6ea71d4391053c,",
                "07492ff26e3db7648ff71caab8e5f96b3153a16d",
                "]",
                Commands.loud //we want to see everything in the log!
        });
        /** the expected values for this test are stored on the hard drive in the exported directory! */
        File expectedChangesFileFor1= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/1/expectedSyntacticChanges");
        String data1 = FileUtils.readData(expectedChangesFileFor1);
        JSONArray a1 = new JSONArray(data1);
        SyntaxChange[] expectedChangesFor1 = new SyntaxChange[a1.length()];
        for (int j = 0; j < a1.length(); ++j) expectedChangesFor1[j] = SyntaxChange.fromJSON(a1.getJSONObject(j));
        SyntaxChange[][] expectedArr = new SyntaxChange[][] { expectedChangesFor1 };
        verify(0, 1, new int[]{6}, expectedArr);
    }


    @Test
    public void testSyntaxAnalysisNewAndInnerClasses() throws IOException {
        Tool.main(new String[]{
                Commands.analyze,
                Commands.nameOfRepo, "testrepo",
                Commands.noRepo,
                Commands.outputFolder, locationOfAlreadyExtracted.getAbsolutePath(),
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommandShort, "compile",
                Commands.versions,
                "[",
                "07492ff26e3db7648ff71caab8e5f96b3153a16d",
                "4c81252a4b93c06db49b45e5b3428714c88a5522",
                "7dda0e5489868da786d9a4c058a7f336b56bdd06",
                "]",
                Commands.loud //we want to see everything in the log!
        });

        //TODO the 'MyOtherClass' is not detected by ChangeDistiller *fail*

        /** the expected values for this test are stored on the hard drive in the exported directory! */
        File expectedChangesFileFor2= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/2/expectedSyntacticChanges");
        File expectedChangesFileFor3= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/3/expectedSyntacticChanges");
        String data2 = FileUtils.readData(expectedChangesFileFor2);
        String data3 = FileUtils.readData(expectedChangesFileFor3);
        JSONArray a2 = new JSONArray(data2);
        JSONArray a3 = new JSONArray(data3);
        SyntaxChange[] expectedChangesFor2 = new SyntaxChange[a2.length()];
        SyntaxChange[] expectedChangesFor3 = new SyntaxChange[a3.length()];
        for (int j = 0; j < a2.length(); ++j) expectedChangesFor2[j] = SyntaxChange.fromJSON(a2.getJSONObject(j));
        for (int j = 0; j < a3.length(); ++j) expectedChangesFor3[j] = SyntaxChange.fromJSON(a3.getJSONObject(j));
        SyntaxChange[][] expectedArr = new SyntaxChange[][] { expectedChangesFor2, expectedChangesFor3};
        verify(1, 3, new int[]{expectedChangesFor2.length, expectedChangesFor3.length}, expectedArr);
    }



    @Test
    public void testSyntaxAnalysisForComplexAndNestedCases() throws IOException {
        Tool.main(new String[]{
                Commands.analyze,
                Commands.nameOfRepo, "testrepo",
                Commands.noRepo,
                Commands.outputFolder, locationOfAlreadyExtracted.getAbsolutePath(),
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommandShort, "compile",
                Commands.versions,
                "[",
                "7dda0e5489868da786d9a4c058a7f336b56bdd06",
                "49bdcd2d9173a3ab2f7d390d1264b413909f0bf9",
                "68001032532869d0dcc08ae7ee856a7df95f425e",
                "]",
                Commands.loud //we want to see everything in the log!
        });

        /** the expected values for this test are stored on the hard drive in the exported directory! */
        File expectedChangesFileFor4= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/4/expectedSyntacticChanges");
        File expectedChangesFileFor5= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/5/expectedSyntacticChanges");
        String data4 = FileUtils.readData(expectedChangesFileFor4);
        String data5 = FileUtils.readData(expectedChangesFileFor5);
        JSONArray a4 = new JSONArray(data4);
        JSONArray a5 = new JSONArray(data5);
        SyntaxChange[] expectedChangesFor4 = new SyntaxChange[a4.length()];
        SyntaxChange[] expectedChangesFor5 = new SyntaxChange[a5.length()];
        for (int j = 0; j < a4.length(); ++j) expectedChangesFor4[j] = SyntaxChange.fromJSON(a4.getJSONObject(j));
        for (int j = 0; j < a5.length(); ++j) expectedChangesFor5[j] = SyntaxChange.fromJSON(a5.getJSONObject(j));
        SyntaxChange[][] expectedArr = new SyntaxChange[][] { expectedChangesFor4, expectedChangesFor5};
        verify(3, 5, new int[]{expectedChangesFor4.length, expectedChangesFor5.length}, expectedArr);
    }

    @Test
    public void testSyntaxAnalysisForInnerClassesAndTheirConstructors() throws IOException {
        Tool.main(new String[]{
                Commands.analyze,
                Commands.nameOfRepo, "testrepo",
                Commands.noRepo,
                Commands.outputFolder, locationOfAlreadyExtracted.getAbsolutePath(),
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommandShort, "compile",
                Commands.versions,
                "[",
                "7c186b7b81b5b92e09ab351aff3799d8f278f06c",
                "cbc1da02d5fc2249c3e6d040765c91417e3e75ba",
                "31402c893b7d3e6d1746b6db5ce764d37ca0e2d9",
                "]",
                Commands.loud //we want to see everything in the log!
        });


        /** the expected values for this test are stored on the hard drive in the exported directory! */
        File expectedChangesFileFor7= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/7/expectedSyntacticChanges");
        File expectedChangesFileFor8= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/8/expectedSyntacticChanges");
        String data7 = FileUtils.readData(expectedChangesFileFor7);
        String data8 = FileUtils.readData(expectedChangesFileFor8);
        JSONArray a7 = new JSONArray(data7);
        JSONArray a8 = new JSONArray(data8);
        SyntaxChange[] expectedChangesFor7 = new SyntaxChange[a7.length()];
        SyntaxChange[] expectedChangesFor8 = new SyntaxChange[a8.length()];
        for (int j = 0; j < a7.length(); ++j) expectedChangesFor7[j] = SyntaxChange.fromJSON(a7.getJSONObject(j));
        for (int j = 0; j < a8.length(); ++j) expectedChangesFor8[j] = SyntaxChange.fromJSON(a8.getJSONObject(j));
        SyntaxChange[][] expectedArr = new SyntaxChange[][]{expectedChangesFor7, expectedChangesFor8};
        verify(6, 8, new int[]{expectedChangesFor7.length, expectedChangesFor8.length}, expectedArr);

    }


    @Test
    public void testSyntaxAnalysisForComplexDataTypes() throws IOException {
        Tool.main(new String[]{
                Commands.analyze,
                Commands.nameOfRepo, "testrepo",
                Commands.noRepo,
                Commands.outputFolder, locationOfAlreadyExtracted.getAbsolutePath(),
                Commands.relativePathToSources, "src",
                Commands.relativePathToCompiledSources, "build",
                Commands.buildCommandShort, "compile",
                Commands.versions,
                "[",
                "31402c893b7d3e6d1746b6db5ce764d37ca0e2d9",
                "ba5093b7d3e6d1746b66b6db5ce764d37ca0e2d9",
                "]",
                Commands.loud //we want to see everything in the log!
        });


        /* TODO: in this test, no changes to TestClass are detected which is completely wrong! Note that this is
         * ChangeDistiller's fault, which can be confirmed by executing {@link testThatRevealsThatChangeDistillerHasProblems} */
        File expectedChangesFileFor9= new File(locationOfAlreadyExtracted.getAbsolutePath() +
                "/9/expectedSyntacticChanges");
        String data9 = FileUtils.readData(expectedChangesFileFor9);
        JSONArray a9 = new JSONArray(data9);
        SyntaxChange[] expectedChangesFor9 = new SyntaxChange[a9.length()];
        for (int j = 0; j < a9.length(); ++j) expectedChangesFor9[j] = SyntaxChange.fromJSON(a9.getJSONObject(j));
        verify(8, 9, new int[]{expectedChangesFor9.length}, new SyntaxChange[][]{expectedChangesFor9});
    }

    @Test
    public void ChangeDistillerCanNowHandleJava7() throws IOException {
        File f1 = new File(locationOfAlreadyExtracted.getAbsolutePath() +"/8/version/src/thisPackageOfMine/TestClass.java");
        File f2 = new File(locationOfAlreadyExtracted.getAbsolutePath() +"/9/version/src/thisPackageOfMine/TestClass.java");
        assertTrue(f1.exists() && !f1.isDirectory());
        assertTrue(f2.exists() && !f2.isDirectory());
        assertFalse(f1.equals(f2)); //this is a MODIFIED with actual modifications!
        assertNotEquals(FileUtils.readData(f1), FileUtils.readData(f2));

        FileDistiller distiller = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);
        distiller.extractClassifiedSourceCodeChanges(f1, f2);
        assertEquals(15, distiller.getSourceCodeChanges().size());
    }

    /*
    @Test
    public void testIgnoringOfRenaming() throws IOException {
        /* TODO: ChangeDistiller's renaming functionality is to stupid, implement a better one to make this test run successful!
         * Whatever changes I have made to the repository, they are not detected as renaming operations thus far...
        {
            Tool.main(new String[]{
                    Commands.analyze,
                    Commands.nameOfRepo, "testrepo",
                    Commands.pathToLocalRepo, locationOfRepo.getAbsolutePath(),
                    Commands.outputFolder, locationOfAlreadyExtracted.getAbsolutePath(),
                    Commands.relativePathToSources, "src",
                    Commands.relativePathToCompiledSources, "build",
                    Commands.buildCommandShort, "compile",
                    Commands.coverRenaming, // with renaming
                    Commands.versions,
                    "[",
                    "68001032532869d0dcc08ae7ee856a7df95f425e",
                    "7c186b7b81b5b92e09ab351aff3799d8f278f06c",
                    "]",
                    Commands.loud //we want to see everything in the log!
            });

            // the expected values for this test are stored on the hard drive in the exported directory!
            File expectedChangesFileWithRenaming = new File(locationOfAlreadyExtracted.getAbsolutePath() +
                    "/6/expectedChangesOfTestWithRenaming");
            String data = FileUtils.readData(expectedChangesFileWithRenaming);
            JSONArray a = new JSONArray(data);
            SyntaxChange[] expectedChangesWithRenaming = new SyntaxChange[a.length()];
            for (int j = 0; j < a.length(); ++j)
                expectedChangesWithRenaming[j] = SyntaxChange.fromJSON(a.getJSONObject(j));
            SyntaxChange[][] expectedArr = new SyntaxChange[][]{expectedChangesWithRenaming};
            verify(5, 6, new int[]{expectedChangesWithRenaming.length}, expectedArr);
        }

        //------------------------------------------------------------------------------------------------------

        // delete the detected changes and analyze them again, this time without renaming
        new File(locationOfAlreadyExtracted.getAbsolutePath()+ "/6/syntacticCodeChanges").delete();

        //------------------------------------------------------------------------------------------------------

        {
            Tool.main(new String[]{
                    Commands.analyze,
                    Commands.nameOfRepo, "testrepo",
                    Commands.pathToLocalRepo, locationOfRepo.getAbsolutePath(),
                    Commands.outputFolder, locationOfAlreadyExtracted.getAbsolutePath(),
                    Commands.relativePathToSources, "src",
                    Commands.relativePathToCompiledSources, "build",
                    Commands.buildCommandShort, "compile",
                    // without renaming
                    Commands.versions,
                    "[",
                    "68001032532869d0dcc08ae7ee856a7df95f425e",
                    "7c186b7b81b5b92e09ab351aff3799d8f278f06c",
                    "]",
                    Commands.loud //we want to see everything in the log!
            });

            //the expected values for this test are stored on the hard drive in the exported directory!
            File expectedChangesFileWithoutRenaming = new File(locationOfAlreadyExtracted.getAbsolutePath() +
                    "/6/expectedChangesOfTestWithoutRenaming");
            String data = FileUtils.readData(expectedChangesFileWithoutRenaming);
            JSONArray a = new JSONArray(data);
            SyntaxChange[] expectedChangesWithoutRenaming = new SyntaxChange[a.length()];
            for (int j = 0; j < a.length(); ++j)
                expectedChangesWithoutRenaming[j] = SyntaxChange.fromJSON(a.getJSONObject(j));
            SyntaxChange[][] expectedArr = new SyntaxChange[][]{expectedChangesWithoutRenaming};
            verify(5, 6, new int[]{expectedChangesWithoutRenaming.length}, expectedArr);
        }
    }
*/

    @Before
    public void setUp() throws Exception {
        Tool.activateDebugMode();
        Globals.performMachineDependentVersionChecks = false; //otherwise it would be hard-coded on one machine...
        if (!locationOfAlreadyExtracted.exists() || !locationOfAlreadyExtracted.isDirectory()) fail("required resources for testing not found!");
    }


    @After
    public void tearDown() throws Exception {
        for (File f : locationOfAlreadyExtracted.listFiles()){
            if (f.isDirectory()){
                File syntaxFile = new File(f,"syntacticCodeChanges");
                if (syntaxFile.exists()) syntaxFile.delete();
            }
        }
    }
}
