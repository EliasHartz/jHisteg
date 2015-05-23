package hartz.masterThesis.historyGuidedImpactAnalysis.testsuite;

import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileDiffType;
import hartz.masterThesis.historyGuidedImpactAnalysis.core.changeComputation.fileLevelChanges.FileLevelChange;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONArray;
import hartz.masterThesis.historyGuidedImpactAnalysis.coverageObservation.data.json.JSONObject;
import hartz.masterThesis.historyGuidedImpactAnalysis.fileUtils.FileUtils;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Commands;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;
import hartz.masterThesis.historyGuidedImpactAnalysis.testsuite.utils.TestOnTestRepo;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class ExtractionTest extends TestOnTestRepo {

    /**
    * Verifies that all versions can be correctly extracted at once!
    * 
    * @throws IOException
    */

   @Test
   public void testExtractionOfAllVersions() throws IOException {
     Tool.main(new String[] {
           Commands.extractOnly,
           Commands.loud,
           Commands.pathToLocalRepo, pathToTestRepo,
           Commands.nameOfRepo, "UDS_Master_TestRepo",
           Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
           Commands.allVersions
     });
     
     File[] extractedFiles = testingOutputFolder.listFiles();
     
     //check that the number of versions is correct!
     assertEquals(expectedNumberOfVersions + 1/*=history file*/,extractedFiles.length);
     
     //make sure that everything is a directory
     File historyFile = null;
     LinkedList<Integer> numbers = new LinkedList<Integer>();
     for (File f : extractedFiles){
        if (f.isFile()){
           if(f.getName().equalsIgnoreCase("history_of_UDS_Master_TestRepo"))
              historyFile = f;
           else fail("found unexpected file "+f.getName());
        }
        else{
           int index = Integer.parseInt(f.getName());
           assertTrue(index>=0 && index < expectedNumberOfVersions);
           assertTrue(numbers.add(index)); //no double occurrences
        }
     }
     
     //make sure the history file is correct as well
     JSONArray a = new JSONArray(FileUtils.readData(historyFile));
     assertEquals(expectedNumberOfVersions, a.length());
     numbers.clear();
     for (int i = 0; i < expectedNumberOfVersions; ++i){
        int index = a.getJSONObject(i).getInt("index");
        assertTrue(index>=0 && index < expectedNumberOfVersions);
        assertTrue(numbers.isEmpty() || index == numbers.getLast()+1); //version list is sorted
        assertTrue(numbers.add(index)); //no double occurrences
     }
   }

   
   /**
    * Tests extraction and compilation of three individual versions which are not consecutive!
    */

   @Test
   public void testExtractionAndCompilation() throws IOException {      
     Tool.main(new String[] {
           Commands.compile,
           Commands.relativePathToSources, "src",
           Commands.relativePathToCompiledSources, "build",
           Commands.buildCommand, /*ant*/ "compile",
           Commands.loud,
           Commands.pathToLocalRepo, pathToTestRepo,
           Commands.nameOfRepo, "UDS_Master_TestRepo",
           Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
           Commands.versions,
           "[",
           "1017187158c02e5bf4934629c29f221593832e6c",
           "f31bfd4f9b6b55083cf0102dea13143c7364c204",
           "efa564a96d3b978fe539fee5226985a6ca18ada9",
           "]"
     });
     
     File[] extractedFiles = testingOutputFolder.listFiles();
     
     //only what we specified should be extracted!
     assertEquals(3 + 1/*=history file*/,extractedFiles.length);
     
     
     //make sure that everything is a directory
     File historyFile = null;
     File version0 = null;
     File version4 = null;
     File version6 = null;
     LinkedList<Integer> numbers = new LinkedList<Integer>();
     for (File f : extractedFiles){
        if (f.isFile()){
           if(f.getName().equalsIgnoreCase("history_of_UDS_Master_TestRepo"))
              historyFile = f;
           else fail("found unexpected file "+f.getName());
        }
        else{
           int index = Integer.parseInt(f.getName());
           assertTrue(numbers.add(index)); //no double occurrences
           switch(index){
              case 0 : version0 = f; break;
              case 4 : version4 = f; break;
              case 6 : version6 = f; break;
              default : fail("found unexpected file "+f.getName()); 
           }
        }
     }
     
     //make sure the history file still contains all the commit information
     JSONArray a = new JSONArray(FileUtils.readData(historyFile));
     assertEquals(expectedNumberOfVersions, a.length());
     numbers.clear();
     for (int i = 0; i < expectedNumberOfVersions; ++i){
        int index = a.getJSONObject(i).getInt("index");
        assertTrue(index>=0 && index < expectedNumberOfVersions);
        assertTrue(numbers.isEmpty() || index == numbers.getLast()+1); //version list is sorted
        assertTrue(numbers.add(index)); //no double occurrences
     }
     
     //verify that code was extracted
     assertTrue((new File(version0.getAbsolutePath()+"/version/src/mainPackage")).exists());
     assertTrue((new File(version4.getAbsolutePath()+"/version/src/mainPackage")).exists());
     assertTrue((new File(version6.getAbsolutePath()+"/version/src/mainPackage")).exists());
     assertTrue((new File(version6.getAbsolutePath()+"/version/src/otherPackage")).exists());
     
     //verify that code was compiled
     assertTrue((new File(version0.getAbsolutePath()+"/version/build/mainPackage/HelloWorldMain.class")).exists());
     assertTrue((new File(version4.getAbsolutePath()+"/version/build/mainPackage/HelloWorldMain.class")).exists());
     assertTrue((new File(version6.getAbsolutePath()+"/version/build/mainPackage/HelloWorldMain.class")).exists());
     assertTrue((new File(version6.getAbsolutePath()+"/version/build/otherPackage/IhateTheWorld.class")).exists());
     
     
     
     //test contents of commit files in detail
     JSONObject info0 = new JSONObject(FileUtils.readData(new File(version0.getAbsolutePath()+"/versionInfo")));
     JSONObject info4 = new JSONObject(FileUtils.readData(new File(version4.getAbsolutePath()+"/versionInfo")));
     JSONObject info6 = new JSONObject(FileUtils.readData(new File(version6.getAbsolutePath()+"/versionInfo")));
     
     assertEquals("1017187158c02e5bf4934629c29f221593832e6c",info0.getString("identifier"));
     assertTrue(info0.getString("msg").toLowerCase().contains("helloworld"));
     assertEquals("f31bfd4f9b6b55083cf0102dea13143c7364c204",info4.getString("identifier"));
     assertTrue(info4.getString("msg").toLowerCase().contains("removed paramter"/*I really cannot type, can I ;-) */));
     assertTrue(info4.getString("msg").toLowerCase().contains("added method"));
     assertEquals("efa564a96d3b978fe539fee5226985a6ca18ada9",info6.getString("identifier"));
     assertTrue(info6.getString("msg").toLowerCase().contains("added new class"));
     
     
     
     //test contents of changes files in detail
     JSONArray changesFileLevel0_all = new JSONArray(FileUtils.readData(new File(version0.getAbsolutePath()+"/allFileLevelChanges")));
     JSONArray changesFileLevel0 = new JSONArray(FileUtils.readData(new File(version0.getAbsolutePath()+"/codeFileLevelChanges")));
     JSONArray changesFileLevel4 = new JSONArray(FileUtils.readData(new File(version4.getAbsolutePath()+"/codeFileLevelChanges")));
     JSONArray changesFileLevel6 = new JSONArray(FileUtils.readData(new File(version6.getAbsolutePath()+"/codeFileLevelChanges")));
     
     assertEquals(1,changesFileLevel0.length());
     assertEquals(3, changesFileLevel0_all.length());
     assertEquals(1,changesFileLevel4.length());
     assertEquals(2,changesFileLevel6.length());
   }


    @Test
    public void testFileLevelDifferenceComputation() throws IOException {
        Tool.main(new String[] {
                Commands.extractOnly,
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions,
                "[4e4c79c85a3b4f8937c046f18be3bc943ae95748",
                "697c21a0fff918d4a3398062fdce17eccb2b4545]",
                Commands.loud,
                Commands.pathToLocalRepo, pathToTestRepo,
        });


        File[] extractedFiles = testingOutputFolder.listFiles();
        assertEquals(2 + 1/*=history file*/,extractedFiles.length);

        //check the extracted data structure:
        File version19 = null;
        File version20 = null;
        for (File f : extractedFiles){
            if (f.isFile()){
                if(f.getName().startsWith("history_of_"))
                    /* nothing */;
                else fail("found unexpected file "+f.getName());
            }
            else{
                int index = Integer.parseInt(f.getName());
                switch(index){
                    case 19 : version19 = f; break;
                    case 20 : version20 = f; break;
                    default : fail("found unexpected file "+f.getName());
                }
            }
        }

        //verify that both versions were extracted by checking for something that was never present in older versions!
        assertTrue((new File(version19.getAbsolutePath()+"/version/src/mainPackage/helloWorld/HelloWorldMain.java")).exists());
        assertTrue((new File(version20.getAbsolutePath()+"/version/src/mainPackage/helloWorld/HelloWorldMain.java")).exists());

        JSONArray fileLevelChangesFor19 = new JSONArray(FileUtils.readData(new File(version19.getAbsolutePath()+"/allFileLevelChanges")));
        FileLevelChange[] changes19 = new FileLevelChange[fileLevelChangesFor19.length()];
        for (int i = 0; i<fileLevelChangesFor19.length(); ++i)
            changes19[i] = new FileLevelChange(fileLevelChangesFor19.getJSONObject(i));
        assertEquals(new FileLevelChange(FileDiffType.DELETED, true, null, "src/mainPackage/HelloWorldMain.java"),changes19[0]);
        assertEquals(new FileLevelChange(FileDiffType.ADDED, true, "src/mainPackage/helloWorld/HelloWorldMain.java", null), changes19[1]);
        assertEquals(new FileLevelChange(FileDiffType.MODIFIED, true, "src/otherPackage/IhateTheWorld.java", null),changes19[2]);
        assertEquals(new FileLevelChange(FileDiffType.MODIFIED, true /* since I have not set a proper source folder, it must be assumed that EVERYTHING is source */,
                "tests/tests/TestSuite2.java", null),changes19[3]);

        JSONArray fileLevelChangesFor20 = new JSONArray(FileUtils.readData(new File(version20.getAbsolutePath()+"/allFileLevelChanges")));
        FileLevelChange[] changes20 = new FileLevelChange[fileLevelChangesFor20.length()];
        for (int i = 0; i<fileLevelChangesFor20.length(); ++i)
            changes20[i] = new FileLevelChange(fileLevelChangesFor20.getJSONObject(i));
        assertEquals(new FileLevelChange(FileDiffType.MODIFIED, true, "src/mainPackage/helloWorld/HelloWorldMain.java", null),changes20[0]);
        assertEquals(new FileLevelChange(FileDiffType.MODIFIED, true, "src/otherPackage/IhateTheWorld.java", null), changes20[1]);
    }
   
   @Test
   public void testAutomaticPredecessorComputation() throws IOException {
      Tool.main(new String[] {
            Commands.extractOnly,
            Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
            Commands.versions,
            "8239a8e8e8a601347802a486758498f0db321fd7",
            Commands.pathToLocalRepo, pathToTestRepo,
      });

      File[] extractedFiles = testingOutputFolder.listFiles();
      assertEquals(2 + 1/*=history file*/,extractedFiles.length);
      
      //check the extracted data structure:
      File version1 = null;
      File version2 = null;
      for (File f : extractedFiles){
         if (f.isFile()){
            if(f.getName().startsWith("history_of_"))
                    /* nothing */;
            else fail("found unexpected file "+f.getName());
         }
         else{
            int index = Integer.parseInt(f.getName());
            switch(index){
               case 1 : version1 = f; break;
               case 2 : version2 = f; break;
               default : fail("found unexpected file "+f.getName()); 
            }
         }
      }    
      
      //verify that both versions, including the inferred one, were extracted
      assertTrue((new File(version1.getAbsolutePath()+"/version/src/mainPackage")).exists());
      assertTrue((new File(version2.getAbsolutePath()+"/version/src/mainPackage")).exists());
      
      //verify that the correct predecessor was chosen
      JSONObject info1 = new JSONObject(FileUtils.readData(new File(version1.getAbsolutePath()+"/versionInfo")));
      JSONObject info2 = new JSONObject(FileUtils.readData(new File(version2.getAbsolutePath()+"/versionInfo")));
      assertEquals("4103c4b7d2e80d0d3423b2f28989dda9a2878355",info1.getString("identifier"));
      assertEquals("8239a8e8e8a601347802a486758498f0db321fd7",info2.getString("identifier"));
   }

    @Test
    public void testAutomaticPredecessorComputationWithDeletionOfClass() throws IOException {
        Tool.main(new String[] {
                Commands.extractOnly,
                Commands.relativePathToSources, "src", //required for proper file-level analysis results!
                Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
                Commands.versions, "697c21a0fff918d4a3398062fdce17eccb2b4545",
                Commands.pathToLocalRepo, pathToTestRepo,
        });


        File[] extractedFiles = testingOutputFolder.listFiles();
        assertEquals(2 + 1/*=history file*/,extractedFiles.length);

        //check the extracted data structure:
        File version19 = null;
        File version20 = null;
        for (File f : extractedFiles){
            if (f.isFile()){
                if(!f.getName().startsWith("history_of_"))
                fail("found unexpected file "+f.getName());
            }
            else{
                int index = Integer.parseInt(f.getName());
                switch(index){
                    case 19 : version19 = f; break;
                    case 20 : version20 = f; break;
                    default : fail("found unexpected file "+f.getName());
                }
            }
        }

        //verify that both versions, including the inferred one, were extracted
        assertTrue((new File(version19.getAbsolutePath()+"/version/src/mainPackage")).exists());
        assertTrue((new File(version20.getAbsolutePath()+"/version/src/mainPackage")).exists());

        //verify that the correct predecessor was chosen
        JSONObject info19 = new JSONObject(FileUtils.readData(new File(version19.getAbsolutePath()+"/versionInfo")));
        JSONObject info20 = new JSONObject(FileUtils.readData(new File(version20.getAbsolutePath()+"/versionInfo")));
        assertEquals("4e4c79c85a3b4f8937c046f18be3bc943ae95748",info19.getString("identifier"));
        assertEquals("697c21a0fff918d4a3398062fdce17eccb2b4545",info20.getString("identifier"));


        //now the actually interesting case, can the system properly infer data for this automatically computed predecessor?
        JSONArray fileLevelChangesFor19 = new JSONArray(FileUtils.readData(new File(version19.getAbsolutePath()+"/allFileLevelChanges")));
        FileLevelChange[] changes = new FileLevelChange[fileLevelChangesFor19.length()];
        for (int i = 0; i<fileLevelChangesFor19.length(); ++i)
            changes[i] = new FileLevelChange(fileLevelChangesFor19.getJSONObject(i));
        assertEquals(new FileLevelChange(FileDiffType.DELETED, true, null, "src/mainPackage/HelloWorldMain.java"),changes[0]);
        assertEquals(new FileLevelChange(FileDiffType.ADDED, true, "src/mainPackage/helloWorld/HelloWorldMain.java", null), changes[1]);
        assertEquals(new FileLevelChange(FileDiffType.MODIFIED, true, "src/otherPackage/IhateTheWorld.java", null),changes[2]);
        assertEquals(new FileLevelChange(FileDiffType.MODIFIED, false, "tests/tests/TestSuite2.java", null),changes[3]);
    }
   
   @Test
   public void testExplicitBuildCommands() throws IOException {      
     Tool.main(new String[] {
           Commands.compile,
           Commands.relativePathToSources, "src",
           Commands.relativePathToCompiledSources, "build",
           Commands.loud,
           Commands.pathToLocalRepo, pathToTestRepo,
           Commands.nameOfRepo, "UDS_Master_TestRepo",
           Commands.outputFolder, testingOutputFolder.getAbsolutePath(),
           Commands.versions,
           "[",
           "c88539b1cb812530db213c11788ee623c9aa957d", //'jar' command does not exist
           "b3ff6abd21dcaa19ac5de89f8779b80ff1ddad01", //'jar' command exists
           "]",
           Commands.buildCommand,
           "[",
           /*ant*/ "dist",
           /*ant*/ "jar",
           "]",
           Commands.buildSpot,
           "build.xml"
     });
     
     File[] extractedFiles = testingOutputFolder.listFiles();
     assertEquals(2 + 1/*=history file*/,extractedFiles.length);
     
     //check the extracted data structure:
     File version8 = null;
     File version9 = null;
     LinkedList<Integer> numbers = new LinkedList<Integer>();
     for (File f : extractedFiles){
        if (f.isDirectory()){
           int index = Integer.parseInt(f.getName());
           assertTrue(numbers.add(index)); //no double occurrences
           switch(index){
              case 8 : version8 = f; break;
              case 9 : version9 = f; break;
              default : fail("found unexpected file "+f.getName()); 
           }
        }
     }
     
     //verify that both versions were compiled correctly
     assertTrue((new File(version8.getAbsolutePath()+"/version/build/mainPackage/HelloWorldMain.class")).exists());
     assertTrue((new File(version8.getAbsolutePath()+"/version/build/otherPackage/IhateTheWorld.class")).exists());
     assertTrue((new File(version9.getAbsolutePath()+"/version/build/mainPackage/HelloWorldMain.class")).exists());
     assertTrue((new File(version9.getAbsolutePath()+"/version/build/otherPackage/IhateTheWorld.class")).exists());
     
     //verify that the command did actually also produce a jar file and that the correct command was used
     File dist = new File(version8.getAbsolutePath()+"/version/dist/UDS_MASTER_TEST.jar"); 
     File jar = new File(version9.getAbsolutePath()+"/version/jar/UDS_MASTER_TEST.jar"); 
     assertTrue(dist.exists());
     assertTrue(jar.exists());
   }
}
