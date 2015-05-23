package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

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
import java.nio.file.Files;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class VolatileExtractionTest extends TestingConstants  {

    private File testingOutputFolder = null /* filled in setUp() */;

    @Test
    public void testExtractionGitFromWeb() throws IOException {
      /* This test is volatile since its not a project under my control and will fail once
       * github takes down this repo or the repo is moved or something other (but terrible)
       * thing occurs. In that case, delete this test since it stopped having a purpose then!
       * For the scope of the thesis however, a test like this helps me to find bugs.
       */
        Tool.main(new String[] {
                Commands.extractOnly,
                Commands.loud,
                Commands.cloneShort, "https://github.com/flibitijibibo/LWJake2.git",
                Commands.nameOfRepo, "Quake2 Java Port",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[",
                "e21774164d9e56bc6b5b98c52ce23bd8d299142c",
                "a307db5206d5c1d6da12c5c9bd3b117ef9e786a3",
                "]"
        });

        File[] extractedFiles = testingOutputFolder.listFiles();

        //check that the number of versions is correct!
        assertEquals(2 + 1/*=history file*/,extractedFiles.length);

        //verify that correct versions were extracted!
        for (File f : extractedFiles){
            if (f.isFile()){
                if(!f.getName().equalsIgnoreCase("history_of_Quake2_Java_Port"))
                    fail("found unexpected file "+f.getName());
            }
            else{
                int index = Integer.parseInt(f.getName());
                assertTrue(index == 23 || index == 24);
            }
        }

    }

    @Test
    public void testExtractionSVNFromWeb() throws IOException {
      /* This test is volatile since its not a project under my controld and will fail once
       * sourceforge takes down this repo or the repo is moved or something other (but terrible)
       * thing occurs. In that case, delete this test since it stopped having a purpose then!
       * For the scope of the thesis however, a test like this helps me to find bugs.
       */
        Tool.main(new String[] {
                Commands.extractOnly,
                Commands.loud,
                Commands.cloneShort, "svn://svn.code.sf.net/p/javaopenchess/svn/",
                Commands.nameOfRepo, "Java Open Chess",
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[",
                "71",
                "72", //these are SVN revisions (which start with index 1)
                "73",
                "]"
        });

        File[] extractedFiles = testingOutputFolder.listFiles();

        //check that the number of versions is correct!
        assertEquals(3 + 1/*=history file*/,extractedFiles.length);



        //make sure that everything is a directory
        File historyFile = null;
        File version70 = null;
        File version71 = null;
        File version72 = null;
        LinkedList<Integer> numbers = new LinkedList<Integer>();
        for (File f : extractedFiles){
            if (f.isFile()){
                if(f.getName().equalsIgnoreCase("history_of_Java_Open_Chess"))
                    historyFile = f;
                else fail("found unexpected file "+f.getName());
            }
            else{
                int index = Integer.parseInt(f.getName());
                assertTrue(numbers.add(index)); //no double occurrences
                switch(index){
              /* SVN revisions start with the index 1 but our VCS agnostic counter
               * starts with index 0, therefore these numbers do not match the
               * given revision numbers... */
                    case 70 : version70 = f; break;
                    case 71 : version71 = f; break;
                    case 72 : version72 = f; break;
                    default : fail("found unexpected file "+f.getName());
                }
            }
        }

        //make sure the history file still contains all the commit information
        JSONArray a = new JSONArray(FileUtils.readData(historyFile));
        numbers.clear();
        for (int i = 0; i < a.length(); ++i){
            int index = a.getJSONObject(i).getInt("index");
            assertTrue(numbers.isEmpty() || index == numbers.getLast()+1); //version list is sorted
            assertTrue(numbers.add(index)); //no double occurrences
        }

        //verify that code was extracted
        assertTrue((new File(version70.getAbsolutePath()+"/version/src/jchess")).exists());
        assertTrue((new File(version71.getAbsolutePath()+"/version/src/jchess")).exists());
        assertTrue((new File(version72.getAbsolutePath()+"/version/src/jchess")).exists());
    }


    @Before
    public void setUp() throws Exception {
        Tool.activateDebugMode();
        File f =  Files.createTempDirectory("unit_testing___uds_master_volatile_extraction_test").toFile();
        if (f.exists()){
            if (f.isDirectory()){
                if (f.listFiles().length == 0){
                    testingOutputFolder = f;
                    return; //we already have a clean output directory for testing
                }
                else{
                    //delete directory and create it anew
                    FileUtils.removeDirectory(f);
                    f.mkdir();
                }
            }
            else throw new InternalError("There is a file named 'unit_testing___uds_master_volatile_extraction_test' present but expected is a directory!");
        }
        else{
            f.mkdir(); //simply create it
        }
        testingOutputFolder = f;
    }


    @After
    public void tearDown() throws Exception {
        FileUtils.removeDirectory(testingOutputFolder);
        Tool.print("\n\n");
    }
}
