package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils;

import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class TestOnTestRepo extends TestingConstants{

    protected File testingOutputFolder;
    protected File testRepoZip;
    protected File locationOfTestRepo;
    protected String pathToTestRepo;

    public TestOnTestRepo() {
        try {
            testRepoZip = new File(TestOnTestRepo.class.getResource("/testrepo.zip").toURI());
        } catch (URISyntaxException e) {
            fail("required resources not found!");
        }
    }

    @Before
    public void setUp() throws Exception {
        Tool.activateDebugMode();
        File f = Files.createTempDirectory("unit_testing_of_master_thesis").toFile();
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
        locationOfTestRepo = new File(r,"uds_master_test");
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
